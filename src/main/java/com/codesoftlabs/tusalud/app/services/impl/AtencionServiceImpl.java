package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.AtencionRequest;
import com.codesoftlabs.tusalud.app.dto.request.DiagnosticoRequest;
import com.codesoftlabs.tusalud.app.dto.request.RecetaItemRequest;
import com.codesoftlabs.tusalud.app.dto.response.AtencionResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.AtencionDiagnostico;
import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecetaItem;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCita;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoDispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.DispensacionRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.cie10.Cie10Normalizador;
import com.codesoftlabs.tusalud.app.services.interfaces.AtencionService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.Cie10Service;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AtencionServiceImpl implements AtencionService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final long VENTANA_EDICION_HORAS = 48;
    private static final String DIAGNOSTICOS_REGISTRADOS = "DIAGNOSTICOS_REGISTRADOS";
    private static final String DIAGNOSTICOS_ACTUALIZADOS = "DIAGNOSTICOS_ACTUALIZADOS";

    private final CitaRepository citaRepository;
    private final AtencionRepository atencionRepository;
    private final DispensacionRepository dispensacionRepository;
    private final NotificacionService notificacionService;
    private final Cie10Service cie10Service;
    private final AuditoriaClinicaService auditoriaClinicaService;
    private final com.codesoftlabs.tusalud.app.repositories.RecetaElectronicaRepository recetasElectronicas;
    private final com.codesoftlabs.tusalud.app.repositories.AtencionCierreRepository cierres;

    @Override
    @Transactional
    public AtencionResponse registrar(UUID citaId, AtencionRequest request, UsuarioPrincipal medico) {
        Cita cita = buscarCita(citaId);
        if (!esMedicoDe(cita, medico)) {
            throw new ForbiddenOperationException("Solo el médico de la cita puede registrar su atención");
        }
        if (atencionRepository.existsByCitaId(citaId)) {
            throw new ConflictException("La cita ya tiene una atención registrada");
        }
        if (cita.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessLogicException("No se puede registrar la atención de una cita CANCELADA");
        }

        Atencion atencion = Atencion.builder()
                .cita(cita)
                .paciente(cita.getPaciente())
                .medico(cita.getMedico())
                .fecha(LocalDateTime.now())
                .build();
        List<String> avisos = aplicar(atencion, request);
        atencion = atencionRepository.save(atencion);

        if (cita.getEstado() != EstadoCita.ATENDIDA) {
            cita.setEstado(EstadoCita.ATENDIDA);
        }
        notificacionService.crear(
                cita.getPaciente().getUsuario(),
                TipoNotificacion.CITA_ATENDIDA,
                "Atención registrada",
                "Se registró la atención de su cita del " + cita.getFechaHora().format(FORMATO_FECHA) + " con "
                        + cita.getMedico().getUsuario().getNombreCompleto() + ".",
                "/citas/" + cita.getId());
        auditoriaClinicaService.registrar(
                medico, cita.getPaciente().getId(), DIAGNOSTICOS_REGISTRADOS, resumenDiagnosticos(atencion));
        log.info("Atención {} registrada para la cita {}", atencion.getId(), citaId);
        return AtencionResponse.from(atencion).conAdvertencias(avisos);
    }

    @Override
    @Transactional
    public AtencionResponse actualizar(UUID citaId, AtencionRequest request, UsuarioPrincipal medico) {
        Cita cita = buscarCita(citaId);
        if (!esMedicoDe(cita, medico)) {
            throw new ForbiddenOperationException("Solo el médico de la cita puede editar su atención");
        }
        Atencion atencion = buscarAtencion(citaId);
        if (cierres.existsByAtencionId(atencion.getId())) {
            throw new ConflictException(
                    "La atención está cerrada y firmada: use una adenda o una corrección con motivo");
        }
        if (atencion.getFecha().isBefore(LocalDateTime.now().minusHours(VENTANA_EDICION_HORAS))) {
            throw new ConflictException(
                    "La atención ya no se puede modificar: pasaron más de " + VENTANA_EDICION_HORAS + " horas");
        }
        if (recetasElectronicas.existsByAtencionIdAndEstadoNot(
                atencion.getId(),
                com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica.ANULADA)) {
            throw new ConflictException(
                    "La atención tiene una receta electrónica emitida. Anúlela para modificar la receta");
        }
        if (dispensacionRepository.existsByAtencionIdAndEstado(atencion.getId(), EstadoDispensacion.EMITIDA)) {
            throw new ConflictException("La receta de esta atención ya fue dispensada y no se puede modificar");
        }
        List<String> avisos = aplicar(atencion, request);
        auditoriaClinicaService.registrar(
                medico, atencion.getPaciente().getId(), DIAGNOSTICOS_ACTUALIZADOS, resumenDiagnosticos(atencion));
        return AtencionResponse.from(atencion).conAdvertencias(avisos);
    }

    @Override
    public AtencionResponse obtener(UUID citaId, UsuarioPrincipal actor) {
        Cita cita = buscarCita(citaId);
        boolean paciente = cita.getPaciente().getUsuario().getId().equals(actor.getId());
        if (!paciente && !esMedicoDe(cita, actor)) {
            throw new ForbiddenOperationException("No tiene permiso para ver la atención de esta cita");
        }
        return AtencionResponse.from(buscarAtencion(citaId));
    }

    private Cita buscarCita(UUID citaId) {
        return citaRepository
                .findByIdWithDetails(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Cita", "id", citaId));
    }

    private Atencion buscarAtencion(UUID citaId) {
        return atencionRepository
                .findByCitaIdConDetalle(citaId)
                .orElseThrow(() -> new ResourceNotFoundException("Atención", "cita", citaId));
    }

    private static boolean esMedicoDe(Cita cita, UsuarioPrincipal actor) {
        return cita.getMedico().getUsuario().getId().equals(actor.getId());
    }

    private List<String> aplicar(Atencion atencion, AtencionRequest request) {
        List<AtencionDiagnostico> diagnosticos = new ArrayList<>();
        List<String> avisos = reconciliarDiagnosticos(atencion, request.diagnosticos(), diagnosticos);
        AtencionDiagnostico principal = diagnosticos.get(0);
        String descripcion = vacioANulo(request.diagnostico());
        atencion.setDiagnostico(recortar(descripcion == null ? principal.getDescripcion() : descripcion, 1000));
        atencion.setTratamiento(vacioANulo(request.tratamiento()));
        atencion.setIndicaciones(vacioANulo(request.indicaciones()));
        atencion.setObservaciones(vacioANulo(request.observaciones()));
        atencion.setPesoKg(request.pesoKg());
        atencion.setTallaCm(request.tallaCm());
        atencion.setPresionArterial(vacioANulo(request.presionArterial()));
        atencion.setTemperaturaC(request.temperaturaC());
        atencion.setFrecuenciaCardiaca(request.frecuenciaCardiaca());
        List<RecetaItem> receta = new ArrayList<>();
        if (request.receta() != null) {
            for (RecetaItemRequest item : request.receta()) {
                receta.add(RecetaItem.builder()
                        .medicamento(item.medicamento().trim())
                        .dosis(vacioANulo(item.dosis()))
                        .frecuencia(vacioANulo(item.frecuencia()))
                        .duracion(vacioANulo(item.duracion()))
                        .build());
            }
        }
        atencion.setReceta(receta);
        return avisos;
    }

    private List<String> reconciliarDiagnosticos(
            Atencion atencion, List<DiagnosticoRequest> pedidos, List<AtencionDiagnostico> resultado) {
        if (pedidos == null || pedidos.isEmpty()) {
            throw new DatosInvalidosException("Registre al menos un diagnóstico CIE-10 principal");
        }
        long principales =
                pedidos.stream().filter(DiagnosticoRequest::principal).count();
        if (principales != 1) {
            throw new DatosInvalidosException("Debe haber exactamente un diagnóstico principal");
        }
        Map<String, DiagnosticoRequest> porCodigo = new LinkedHashMap<>();
        for (DiagnosticoRequest pedido : pedidos) {
            String codigo = Cie10Normalizador.codigo(pedido.codigo());
            if (porCodigo.put(codigo, pedido) != null) {
                throw new DatosInvalidosException("El diagnóstico " + codigo + " está repetido");
            }
        }
        List<String> ordenados = new ArrayList<>();
        porCodigo.forEach((codigo, pedido) -> {
            if (pedido.principal()) {
                ordenados.add(0, codigo);
            } else {
                ordenados.add(codigo);
            }
        });

        Map<String, AtencionDiagnostico> actuales = new HashMap<>();
        for (AtencionDiagnostico existente : atencion.getDiagnosticos()) {
            actuales.put(existente.getCodigo(), existente);
        }
        Paciente paciente = atencion.getPaciente();
        List<String> avisos = new ArrayList<>();
        int posicion = 0;
        for (String codigo : ordenados) {
            DiagnosticoRequest pedido = porCodigo.get(codigo);
            Cie10Catalogo catalogo = cie10Service.exigirActivo(codigo);
            AtencionDiagnostico fila = actuales.remove(codigo);
            if (fila == null) {
                fila = AtencionDiagnostico.builder().atencion(atencion).build();
                atencion.getDiagnosticos().add(fila);
            }
            fila.setCie10(catalogo);
            fila.setCodigo(catalogo.getCodigo());
            fila.setDescripcion(catalogo.getDescripcion());
            fila.setTipo(pedido.tipo());
            fila.setPrincipal(pedido.principal());
            fila.setObservacion(vacioANulo(pedido.observacion()));
            fila.setPosicion(posicion++);
            resultado.add(fila);
            agregarAvisos(avisos, catalogo, paciente, atencion);
        }
        atencion.getDiagnosticos().removeAll(actuales.values());
        atencion.getDiagnosticos().sort(java.util.Comparator.comparingInt(AtencionDiagnostico::getPosicion));
        return avisos;
    }

    private static void agregarAvisos(
            List<String> avisos, Cie10Catalogo catalogo, Paciente paciente, Atencion atencion) {
        if (catalogo.getSexo() != null && paciente.getSexo() != null && catalogo.getSexo() != paciente.getSexo()) {
            avisos.add("El diagnóstico " + catalogo.getCodigo() + " está definido para el sexo "
                    + (catalogo.getSexo().name().equals("F") ? "femenino" : "masculino")
                    + " y no coincide con el del paciente");
        }
        if (paciente.getFechaNacimiento() != null && atencion.getFecha() != null) {
            int edad = Period.between(
                            paciente.getFechaNacimiento(), atencion.getFecha().toLocalDate())
                    .getYears();
            if (catalogo.getEdadMinima() != null && edad < catalogo.getEdadMinima()) {
                avisos.add("El diagnóstico " + catalogo.getCodigo() + " se aplica desde los " + catalogo.getEdadMinima()
                        + " años y el paciente tiene " + edad);
            }
            if (catalogo.getEdadMaxima() != null && edad > catalogo.getEdadMaxima()) {
                avisos.add("El diagnóstico " + catalogo.getCodigo() + " se aplica hasta los " + catalogo.getEdadMaxima()
                        + " años y el paciente tiene " + edad);
            }
        }
    }

    private static String resumenDiagnosticos(Atencion atencion) {
        StringBuilder texto = new StringBuilder();
        for (AtencionDiagnostico d : atencion.getDiagnosticos()) {
            if (texto.length() > 0) {
                texto.append(", ");
            }
            texto.append(d.isPrincipal() ? "principal " : "").append(d.getCodigo());
        }
        return recortar(texto.toString(), 400);
    }

    private static String recortar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
