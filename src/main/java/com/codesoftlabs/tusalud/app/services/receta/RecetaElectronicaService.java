package com.codesoftlabs.tusalud.app.services.receta;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.request.DispensacionItemRequest;
import com.codesoftlabs.tusalud.app.dto.request.DispensacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.AlertasRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.DispensarRecetaRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.EmitirRecetaRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.ItemDispensarReceta;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.ItemEmision;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.ItemPrescripcion;
import com.codesoftlabs.tusalud.app.dto.request.RecetaRequests.JustificacionAlerta;
import com.codesoftlabs.tusalud.app.dto.response.DispensacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.AlertaRegistradaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.AlertasResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.ItemRecetaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.RecetaFarmaciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.RecetaResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.RecetaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.RecetaResponses.VerificacionFirmaResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.AtencionDiagnostico;
import com.codesoftlabs.tusalud.app.entities.Medicamento;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecetaAlerta;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronicaItem;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicamentoRepository;
import com.codesoftlabs.tusalud.app.repositories.RecetaElectronicaRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.SecuenciasDocumentosMedicos;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.FarmaciaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.pediatria.AccesoPaciente;
import com.codesoftlabs.tusalud.app.services.receta.AlertasPrescripcionService.Alerta;
import com.codesoftlabs.tusalud.app.services.receta.RecetaFirma.Firmada;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecetaElectronicaService {

    private static final int VIGENCIA_ESTANDAR_DIAS = 30;
    private static final int VIGENCIA_CONTROL_ESPECIAL_DIAS = 7;
    private static final int LIMITE_FARMACIA = 50;

    private final RecetaElectronicaRepository recetas;
    private final AtencionRepository atenciones;
    private final MedicamentoRepository medicamentos;
    private final UsuarioRepository usuarios;
    private final AccesoPaciente acceso;
    private final AlertasPrescripcionService alertas;
    private final RecetaFirma firma;
    private final SecuenciasDocumentosMedicos secuencias;
    private final NotificacionService notificaciones;
    private final AuditoriaClinicaService auditoria;
    private final FarmaciaService farmacia;
    private final RecetaElectronicaPdf pdf;

    @Value("${app.documentos-medicos.url-verificacion:http://localhost:4600/verificar/}")
    private String urlVerificacion;

    public AlertasResponse evaluarAlertas(UsuarioPrincipal actor, AlertasRequest r) {
        Paciente paciente = acceso.exigirEscritura(actor, r.pacienteId());
        List<Alerta> lista = alertas.evaluar(
                paciente, r.items(), Boolean.TRUE.equals(r.embarazada()), Boolean.TRUE.equals(r.lactando()), null);
        return respuestaAlertas(lista, Set.of());
    }

    @Transactional
    public RecetaResponse emitir(UsuarioPrincipal actor, EmitirRecetaRequest r) {
        Atencion atencion = atenciones
                .findByIdConDetalle(r.atencionId())
                .orElseThrow(() -> new ResourceNotFoundException("Atención", "id", r.atencionId()));
        if (!atencion.getMedico().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el médico de la atención puede emitir su receta electrónica");
        }
        Paciente paciente = atencion.getPaciente();
        if (recetas.existsByAtencionIdAndEstadoNot(atencion.getId(), EstadoRecetaElectronica.ANULADA)) {
            throw new ConflictException(
                    "La atención ya tiene una receta electrónica vigente. Anúlela para emitir una nueva");
        }
        RecetaElectronica reemplaza = null;
        if (r.reemplazaId() != null) {
            reemplaza = recetas.findDetalle(r.reemplazaId())
                    .orElseThrow(() -> new ResourceNotFoundException("Receta", "id", r.reemplazaId()));
            if (reemplaza.getEstado() != EstadoRecetaElectronica.ANULADA
                    || !reemplaza.getMedico().getUsuario().getId().equals(actor.getId())
                    || !reemplaza.getPaciente().getId().equals(paciente.getId())) {
                throw new ConflictException("Solo se puede reemplazar una receta anulada del mismo médico y paciente");
            }
        }
        List<RecetaItem> lineas = atencion.getReceta();
        Set<Integer> posiciones = new HashSet<>();
        Map<UUID, Medicamento> catalogo = new HashMap<>();
        List<ItemPrescripcion> prescripcion = new ArrayList<>();
        for (ItemEmision item : r.items()) {
            if (item.atencionPosicion() >= lineas.size()) {
                throw new DatosInvalidosException(
                        "La posición " + item.atencionPosicion() + " no existe en la receta de la atención");
            }
            if (!posiciones.add(item.atencionPosicion())) {
                throw new DatosInvalidosException("La posición " + item.atencionPosicion() + " está repetida");
            }
            Medicamento m = medicamentos
                    .findConPrincipio(item.medicamentoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicamento", "id", item.medicamentoId()));
            catalogo.put(m.getId(), m);
            prescripcion.add(new ItemPrescripcion(item.medicamentoId(), item.dosisUnitariaMg(), item.vecesPorDia()));
        }
        boolean embarazada = Boolean.TRUE.equals(r.embarazada());
        boolean lactando = Boolean.TRUE.equals(r.lactando());
        List<Alerta> evaluadas = alertas.evaluar(paciente, prescripcion, embarazada, lactando, null);
        Map<String, String> justificaciones = new HashMap<>();
        if (r.justificaciones() != null) {
            for (JustificacionAlerta j : r.justificaciones()) {
                justificaciones.put(j.clave(), j.texto().strip());
            }
        }
        List<String> faltantes = new ArrayList<>();
        for (Alerta a : evaluadas) {
            if (a.requiereJustificacion()) {
                String texto = justificaciones.get(a.clave());
                if (texto == null || texto.length() < a.minimoJustificacion()) {
                    faltantes.add(a.mensaje());
                }
            }
        }
        if (!faltantes.isEmpty()) {
            throw new ConflictException("Faltan justificaciones para continuar: " + String.join(" | ", faltantes));
        }
        boolean controlEspecial =
                catalogo.values().stream().anyMatch(m -> m.getCategoriaControl().especial());
        int maxima = controlEspecial ? VIGENCIA_CONTROL_ESPECIAL_DIAS : 90;
        int vigenciaDias = r.vigenciaDias() == null
                ? (controlEspecial ? VIGENCIA_CONTROL_ESPECIAL_DIAS : VIGENCIA_ESTANDAR_DIAS)
                : r.vigenciaDias();
        if (vigenciaDias > maxima) {
            throw new DatosInvalidosException("La vigencia máxima de esta receta es de " + maxima
                    + " días por incluir medicamentos de control especial");
        }
        LocalDateTime emitidaEn = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        String numero =
                String.format("RX-%d-%06d", emitidaEn.getYear(), secuencias.siguiente("RECETA", emitidaEn.getYear()));
        AtencionDiagnostico principal = atencion.getDiagnosticos().stream()
                .filter(AtencionDiagnostico::isPrincipal)
                .findFirst()
                .orElse(null);

        RecetaElectronica receta = RecetaElectronica.builder()
                .numero(numero)
                .atencion(atencion)
                .paciente(paciente)
                .medico(atencion.getMedico())
                .estado(EstadoRecetaElectronica.EMITIDA)
                .emitidaEn(emitidaEn)
                .vigenteHasta(emitidaEn.plusDays(vigenciaDias))
                .indicaciones(vacioANulo(r.indicaciones()))
                .diagnosticoCodigo(principal == null ? null : principal.getCodigo())
                .diagnosticoDescripcion(principal == null ? null : recortar(principal.getDescripcion(), 300))
                .controlEspecial(controlEspecial)
                .reemplaza(reemplaza)
                .build();
        int posicion = 0;
        for (ItemEmision item : r.items()) {
            Medicamento m = catalogo.get(item.medicamentoId());
            RecetaItem linea = lineas.get(item.atencionPosicion());
            receta.agregarItem(RecetaElectronicaItem.builder()
                    .posicion(posicion++)
                    .atencionPosicion(item.atencionPosicion())
                    .medicamento(m)
                    .medicamentoTexto(m.nombreCompleto())
                    .dosis(linea.getDosis())
                    .frecuencia(linea.getFrecuencia())
                    .duracion(linea.getDuracion())
                    .via(
                            vacioANulo(item.via()) == null
                                    ? m.getViaAdministracion()
                                    : item.via().strip())
                    .dosisUnitariaMg(item.dosisUnitariaMg())
                    .vecesPorDia(item.vecesPorDia())
                    .cantidad(item.cantidad())
                    .controlEspecial(m.getCategoriaControl().especial())
                    .build());
        }
        String canonico = canonico(receta);
        Firmada firmada = firma.firmar(canonico);
        receta.setContenidoCanonico(canonico);
        receta.setHash(CodigoDocumentoClinico.sha256(canonico));
        receta.setFirma(firmada.firma());
        receta.setFirmaAlgoritmo(firmada.algoritmo());
        receta.setCertificadoHuella(firmada.huella());
        receta.setCertificadoSujeto(recortar(firmada.sujeto(), 200));
        receta.setCertificadoDer(firmada.certificadoDer());
        receta.setCertificadoDePrueba(firmada.dePrueba());
        receta.setCodigoVerificacion(codigoUnico());
        var usuario = usuarios.getReferenceById(actor.getId());
        for (Alerta a : evaluadas) {
            String texto = justificaciones.get(a.clave());
            receta.agregarAlerta(RecetaAlerta.builder()
                    .clave(recortar(a.clave(), 120))
                    .tipo(a.tipo())
                    .severidad(a.severidad())
                    .mensaje(recortar(a.mensaje(), 500))
                    .justificacion(texto)
                    .justificadaPor(texto == null ? null : usuario)
                    .justificadaEn(texto == null ? null : emitidaEn)
                    .build());
        }
        receta = recetas.save(receta);
        long justificadas = evaluadas.stream()
                .filter(a -> justificaciones.containsKey(a.clave()))
                .count();
        auditoria.registrar(
                actor,
                paciente.getId(),
                "RECETA_ELECTRONICA_EMITIDA",
                "Receta " + numero + " con " + receta.getItems().size() + " medicamentos y " + justificadas
                        + " alertas justificadas");
        notificaciones.crear(
                paciente.getUsuario(),
                TipoNotificacion.RECETA_EMITIDA,
                "Receta electrónica emitida",
                "Se emitió la receta " + numero + ". Revísela en la aplicación.",
                "/mis-recetas");
        log.info("Receta electrónica {} emitida para la atención {}", numero, atencion.getId());
        return aRespuesta(receta);
    }

    public RecetaResponse obtener(UsuarioPrincipal actor, UUID id) {
        RecetaElectronica receta = buscar(id);
        exigirLectura(actor, receta);
        RecetaResponse respuesta = aRespuesta(receta);
        if (actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name())) {
            return sinAlertas(respuesta);
        }
        return respuesta;
    }

    private RecetaResponse sinAlertas(RecetaResponse r) {
        return new RecetaResponse(
                r.id(),
                r.numero(),
                r.estado(),
                r.vigente(),
                r.emitidaEn(),
                r.vigenteHasta(),
                r.atencionId(),
                r.citaId(),
                r.pacienteId(),
                r.pacienteNombre(),
                r.pacienteDni(),
                r.medicoNombre(),
                r.medicoColegiatura(),
                r.diagnosticoCodigo(),
                r.diagnosticoDescripcion(),
                r.indicaciones(),
                r.controlEspecial(),
                r.codigoVerificacion(),
                r.hash(),
                r.firmaAlgoritmo(),
                r.certificadoHuella(),
                r.certificadoSujeto(),
                r.certificadoDePrueba(),
                r.firmaValida(),
                r.anuladaEn(),
                r.motivoAnulacion(),
                r.reemplazaNumero(),
                r.items(),
                List.of());
    }

    public List<RecetaResumenResponse> misRecetas(UsuarioPrincipal actor) {
        Paciente propio = acceso.propio(actor);
        return recetas.delPaciente(propio.getId()).stream().map(this::resumen).toList();
    }

    public List<RecetaResumenResponse> delPaciente(UsuarioPrincipal actor, UUID pacienteId) {
        acceso.exigirEscritura(actor, pacienteId);
        return recetas.delPaciente(pacienteId).stream().map(this::resumen).toList();
    }

    public List<RecetaResumenResponse> emitidasPor(UsuarioPrincipal actor) {
        return recetas.delMedico(actor.getId()).stream().map(this::resumen).toList();
    }

    public List<RecetaResumenResponse> deLaAtencion(UsuarioPrincipal actor, UUID atencionId) {
        Atencion atencion = atenciones
                .findByIdConDetalle(atencionId)
                .orElseThrow(() -> new ResourceNotFoundException("Atención", "id", atencionId));
        acceso.exigirEscritura(actor, atencion.getPaciente().getId());
        return recetas.findByAtencionIdOrderByEmitidaEnDesc(atencionId).stream()
                .map(this::resumen)
                .toList();
    }

    @Transactional
    public RecetaResponse anular(UsuarioPrincipal actor, UUID id, String motivo) {
        RecetaElectronica receta = buscar(id);
        if (!receta.getMedico().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("Solo el médico que emitió la receta puede anularla");
        }
        if (receta.getEstado() == EstadoRecetaElectronica.ANULADA) {
            throw new ConflictException("La receta " + receta.getNumero() + " ya está anulada");
        }
        if (receta.getEstado() != EstadoRecetaElectronica.EMITIDA) {
            throw new ConflictException("La receta " + receta.getNumero() + " ya tiene medicamentos dispensados");
        }
        String limpio = vacioANulo(motivo);
        if (limpio == null || limpio.length() < 5 || limpio.length() > 300) {
            throw new DatosInvalidosException("El motivo debe tener entre 5 y 300 caracteres");
        }
        receta.setEstado(EstadoRecetaElectronica.ANULADA);
        receta.setAnuladaEn(LocalDateTime.now());
        receta.setMotivoAnulacion(limpio);
        auditoria.registrar(
                actor,
                receta.getPaciente().getId(),
                "RECETA_ELECTRONICA_ANULADA",
                "Receta " + receta.getNumero() + " anulada con motivo registrado");
        return aRespuesta(receta);
    }

    public byte[] generarPdf(UsuarioPrincipal actor, UUID id) {
        RecetaElectronica receta = buscar(id);
        if (!actor.getRoles().contains(RolNombre.ROLE_FARMACIA.name())) {
            exigirLectura(actor, receta);
        }
        return pdf.renderizar(receta, firmaValida(receta), actor.getUsername());
    }

    public VerificacionFirmaResponse verificarFirma(UsuarioPrincipal actor, UUID id) {
        RecetaElectronica receta = buscar(id);
        exigirLectura(actor, receta);
        boolean hash =
                CodigoDocumentoClinico.sha256(receta.getContenidoCanonico()).equals(receta.getHash());
        boolean valida =
                hash && firma.verificar(receta.getContenidoCanonico(), receta.getFirma(), receta.getCertificadoDer());
        return new VerificacionFirmaResponse(
                receta.getNumero(), hash, valida, receta.getCertificadoHuella(), receta.isCertificadoDePrueba());
    }

    public List<RecetaFarmaciaResponse> paraDispensar(String q) {
        String termino = q == null || q.isBlank() ? null : "%" + q.strip().toLowerCase(java.util.Locale.ROOT) + "%";
        return recetas
                .paraDispensar(
                        EnumSet.of(EstadoRecetaElectronica.EMITIDA, EstadoRecetaElectronica.DISPENSADA_PARCIAL),
                        LocalDateTime.now(),
                        termino,
                        PageRequest.of(0, LIMITE_FARMACIA))
                .stream()
                .map(this::aFarmacia)
                .toList();
    }

    public RecetaFarmaciaResponse farmaciaDetalle(UUID id) {
        return aFarmacia(buscar(id));
    }

    @Transactional
    public DispensacionResponse dispensar(UsuarioPrincipal actor, UUID id, DispensarRecetaRequest r) {
        RecetaElectronica receta = buscar(id);
        LocalDateTime ahora = LocalDateTime.now();
        if (receta.getEstado() == EstadoRecetaElectronica.ANULADA) {
            throw new ConflictException("La receta " + receta.getNumero() + " está anulada");
        }
        if (receta.getEstado() == EstadoRecetaElectronica.DISPENSADA) {
            throw new ConflictException("La receta " + receta.getNumero() + " ya fue dispensada por completo");
        }
        if (ahora.isAfter(receta.getVigenteHasta())) {
            throw new ConflictException("La receta " + receta.getNumero() + " venció");
        }
        if (!firmaValida(receta)) {
            throw new ConflictException("La firma de la receta " + receta.getNumero() + " no se pudo verificar");
        }
        Map<UUID, RecetaElectronicaItem> items = new HashMap<>();
        receta.getItems().forEach(i -> items.put(i.getId(), i));
        List<DispensacionItemRequest> pedidos = new ArrayList<>();
        Set<UUID> vistos = new HashSet<>();
        for (ItemDispensarReceta pedido : r.items()) {
            RecetaElectronicaItem item = items.get(pedido.itemId());
            if (item == null) {
                throw new DatosInvalidosException("El ítem " + pedido.itemId() + " no pertenece a la receta");
            }
            if (!vistos.add(item.getId())) {
                throw new DatosInvalidosException("El ítem " + item.getMedicamentoTexto() + " está repetido");
            }
            if (item.getMedicamento() == null || item.getMedicamento().getProductoId() == null) {
                throw new DatosInvalidosException(
                        item.getMedicamentoTexto() + " no está vinculado a un producto de la farmacia");
            }
            pedidos.add(new DispensacionItemRequest(
                    item.getAtencionPosicion(), item.getMedicamento().getProductoId(), pedido.cantidad()));
        }
        return farmacia.dispensar(
                actor,
                new DispensacionRequest(
                        receta.getAtencion().getId(),
                        receta.getPaciente().getId(),
                        pedidos,
                        r.metodoPago(),
                        r.observaciones()));
    }

    public boolean firmaValida(RecetaElectronica receta) {
        return CodigoDocumentoClinico.sha256(receta.getContenidoCanonico()).equals(receta.getHash())
                && firma.verificar(receta.getContenidoCanonico(), receta.getFirma(), receta.getCertificadoDer());
    }

    private RecetaElectronica buscar(UUID id) {
        return recetas.findDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Receta", "id", id));
    }

    private void exigirLectura(UsuarioPrincipal actor, RecetaElectronica receta) {
        if (actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name())) {
            Paciente propio = acceso.propio(actor);
            if (!propio.getId().equals(receta.getPaciente().getId())) {
                throw new ForbiddenOperationException("Solo puede consultar sus propias recetas");
            }
            return;
        }
        if (receta.getMedico().getUsuario().getId().equals(actor.getId())) {
            return;
        }
        acceso.exigirEscritura(actor, receta.getPaciente().getId());
    }

    private String canonico(RecetaElectronica r) {
        StringBuilder sb = new StringBuilder();
        sb.append("RECETA|")
                .append(r.getNumero())
                .append('|')
                .append(r.getEmitidaEn())
                .append('|')
                .append(r.getVigenteHasta())
                .append('\n');
        sb.append("PACIENTE|")
                .append(r.getPaciente().getId())
                .append('|')
                .append(r.getPaciente().getDni())
                .append('\n');
        sb.append("MEDICO|")
                .append(r.getMedico().getId())
                .append('|')
                .append(r.getMedico().getColegiatura())
                .append('\n');
        sb.append("ATENCION|").append(r.getAtencion().getId()).append('\n');
        sb.append("DX|")
                .append(nulo(r.getDiagnosticoCodigo()))
                .append('|')
                .append(nulo(r.getDiagnosticoDescripcion()))
                .append('\n');
        for (RecetaElectronicaItem i : r.getItems()) {
            sb.append("ITEM|")
                    .append(i.getPosicion())
                    .append('|')
                    .append(i.getMedicamento().getId())
                    .append('|')
                    .append(i.getMedicamentoTexto())
                    .append('|')
                    .append(nulo(i.getDosis()))
                    .append('|')
                    .append(nulo(i.getFrecuencia()))
                    .append('|')
                    .append(nulo(i.getDuracion()))
                    .append('|')
                    .append(nulo(i.getVia()))
                    .append('|')
                    .append(i.getCantidad())
                    .append('\n');
        }
        sb.append("INDICACIONES|").append(nulo(r.getIndicaciones())).append('\n');
        return sb.toString();
    }

    private String codigoUnico() {
        String codigo = CodigoDocumentoClinico.nuevo();
        return recetas.findByCodigo(codigo).isPresent() ? CodigoDocumentoClinico.nuevo() : codigo;
    }

    private AlertasResponse respuestaAlertas(List<Alerta> lista, Set<String> justificadas) {
        int pendientes = (int) lista.stream()
                .filter(a -> a.requiereJustificacion() && !justificadas.contains(a.clave()))
                .count();
        boolean contraindicadas = lista.stream()
                .anyMatch(a -> a.severidad()
                        == com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadAlerta.CONTRAINDICADA);
        return new AlertasResponse(lista.stream().map(alertas::aRespuesta).toList(), pendientes, contraindicadas, true);
    }

    private RecetaResumenResponse resumen(RecetaElectronica r) {
        return new RecetaResumenResponse(
                r.getId(),
                r.getNumero(),
                r.getEstado(),
                r.vigente(LocalDateTime.now()),
                r.getEmitidaEn(),
                r.getVigenteHasta(),
                r.getPaciente().getId(),
                r.getPaciente().getUsuario().getNombreCompleto(),
                r.getPaciente().getDni(),
                r.getMedico().getUsuario().getNombreCompleto(),
                r.getItems().size(),
                r.isControlEspecial());
    }

    private ItemRecetaResponse aItem(RecetaElectronicaItem i) {
        return new ItemRecetaResponse(
                i.getId(),
                i.getPosicion(),
                i.getAtencionPosicion(),
                i.getMedicamento() == null ? null : i.getMedicamento().getId(),
                i.getMedicamentoTexto(),
                i.getDosis(),
                i.getFrecuencia(),
                i.getDuracion(),
                i.getVia(),
                i.getDosisUnitariaMg(),
                i.getVecesPorDia(),
                i.getCantidad(),
                i.getCantidadDispensada(),
                i.pendiente(),
                i.isControlEspecial(),
                i.getMedicamento() == null ? null : i.getMedicamento().getProductoId());
    }

    private RecetaFarmaciaResponse aFarmacia(RecetaElectronica r) {
        return new RecetaFarmaciaResponse(
                r.getId(),
                r.getNumero(),
                r.getEstado(),
                r.getEmitidaEn(),
                r.getVigenteHasta(),
                r.getAtencion().getId(),
                r.getPaciente().getUsuario().getNombreCompleto(),
                r.getPaciente().getDni(),
                r.getMedico().getUsuario().getNombreCompleto(),
                r.getMedico().getColegiatura(),
                r.isControlEspecial(),
                r.getItems().stream().map(this::aItem).toList());
    }

    private RecetaResponse aRespuesta(RecetaElectronica r) {
        List<AlertaRegistradaResponse> registradas = r.getAlertas().stream()
                .map(a -> new AlertaRegistradaResponse(
                        a.getClave(),
                        a.getTipo(),
                        a.getSeveridad(),
                        a.getMensaje(),
                        a.getJustificacion(),
                        a.getJustificadaPor() == null
                                ? null
                                : a.getJustificadaPor().getNombreCompleto(),
                        a.getJustificadaEn()))
                .toList();
        return new RecetaResponse(
                r.getId(),
                r.getNumero(),
                r.getEstado(),
                r.vigente(LocalDateTime.now()),
                r.getEmitidaEn(),
                r.getVigenteHasta(),
                r.getAtencion().getId(),
                r.getAtencion().getCita().getId(),
                r.getPaciente().getId(),
                r.getPaciente().getUsuario().getNombreCompleto(),
                r.getPaciente().getDni(),
                r.getMedico().getUsuario().getNombreCompleto(),
                r.getMedico().getColegiatura(),
                r.getDiagnosticoCodigo(),
                r.getDiagnosticoDescripcion(),
                r.getIndicaciones(),
                r.isControlEspecial(),
                r.getCodigoVerificacion(),
                r.getHash(),
                r.getFirmaAlgoritmo(),
                r.getCertificadoHuella(),
                r.getCertificadoSujeto(),
                r.isCertificadoDePrueba(),
                firmaValida(r),
                r.getAnuladaEn(),
                r.getMotivoAnulacion(),
                r.getReemplaza() == null ? null : r.getReemplaza().getNumero(),
                r.getItems().stream().map(this::aItem).toList(),
                registradas);
    }

    private static String nulo(String texto) {
        return texto == null ? "" : texto;
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static String recortar(String texto, int max) {
        return texto == null ? null : texto.length() <= max ? texto : texto.substring(0, max);
    }
}
