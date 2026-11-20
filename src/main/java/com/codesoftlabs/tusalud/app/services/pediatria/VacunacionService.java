package com.codesoftlabs.tusalud.app.services.pediatria;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.DosisRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.EsquemaDosisRequest;
import com.codesoftlabs.tusalud.app.dto.request.PediatriaRequests.EsquemaRequest;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CarneResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.DosisCarne;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.EsquemaDosisResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.EsquemaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.PacienteClinicoResumen;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.RegistroVacunaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.VacunaResponse;
import com.codesoftlabs.tusalud.app.entities.Atencion;
import com.codesoftlabs.tusalud.app.entities.CarneVacunacion;
import com.codesoftlabs.tusalud.app.entities.EsquemaDosis;
import com.codesoftlabs.tusalud.app.entities.EsquemaVacunacion;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecordatorioVacunacion;
import com.codesoftlabs.tusalud.app.entities.RegistroVacunacion;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.Vacuna;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoDosis;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoRegistroVacunacion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AccesoClinicoRepository;
import com.codesoftlabs.tusalud.app.repositories.AtencionRepository;
import com.codesoftlabs.tusalud.app.repositories.CarneVacunacionRepository;
import com.codesoftlabs.tusalud.app.repositories.EsquemaDosisRepository;
import com.codesoftlabs.tusalud.app.repositories.EsquemaVacunacionRepository;
import com.codesoftlabs.tusalud.app.repositories.RecordatorioVacunacionRepository;
import com.codesoftlabs.tusalud.app.repositories.RegistroVacunacionRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.repositories.VacunaRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraEsquemaVacunacion.DosisAplicada;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraEsquemaVacunacion.DosisCalculada;
import com.codesoftlabs.tusalud.app.services.pediatria.CalculadoraEsquemaVacunacion.DosisEsquema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VacunacionService {

    public static final String ESTABLECIMIENTO_POR_DEFECTO = "TuSalud";
    public static final String ACCION_CONSULTADO = "VACUNACION_CONSULTADA";
    public static final String ACCION_REGISTRADA = "VACUNA_REGISTRADA";
    public static final String ACCION_ANULADA = "VACUNA_ANULADA";
    public static final String ACCION_CARNE = "CARNE_GENERADO";
    private static final int ANTICIPO_RECORDATORIO_DIAS = 7;
    private static final int EDAD_MAXIMA_RECORDATORIO_ANIOS = 19;

    private final VacunaRepository vacunas;
    private final EsquemaVacunacionRepository esquemas;
    private final EsquemaDosisRepository dosisRepositorio;
    private final RegistroVacunacionRepository registros;
    private final RecordatorioVacunacionRepository recordatorios;
    private final CarneVacunacionRepository carnes;
    private final AtencionRepository atenciones;
    private final UsuarioRepository usuarios;
    private final AccesoClinicoRepository accesoClinico;
    private final AccesoPaciente acceso;
    private final AuditoriaClinicaService auditoria;
    private final NotificacionService notificaciones;
    private final CarneVacunacionPdf pdf;

    @Value("${app.pediatria.tolerancia-atraso-dias:30}")
    private int tolerancia;

    public record PdfCarne(byte[] contenido, String nombre) {}

    public List<VacunaResponse> catalogo() {
        return vacunas.findByActivoTrueOrderByNombre().stream()
                .map(v -> new VacunaResponse(v.getId(), v.getCodigo(), v.getNombre(), v.getDescripcion()))
                .toList();
    }

    public List<EsquemaResponse> listarEsquemas() {
        return esquemas.findAllByOrderByVigenteDesdeDesc().stream()
                .map(e -> responder(e, dosisRepositorio.delEsquema(e.getId())))
                .toList();
    }

    public EsquemaResponse esquemaVigente() {
        EsquemaVacunacion esquema = vigente();
        return responder(esquema, dosisRepositorio.delEsquema(esquema.getId()));
    }

    @Transactional
    public EsquemaResponse crearEsquema(UsuarioPrincipal actor, EsquemaRequest request) {
        List<EsquemaVacunacion> existentes = esquemas.findAllByOrderByVigenteDesdeDesc();
        if (!existentes.isEmpty()
                && !request.vigenteDesde().isAfter(existentes.get(0).getVigenteDesde())) {
            throw new ConflictException("La vigencia debe ser posterior a la del esquema más reciente");
        }
        Map<String, Vacuna> porCodigo = new HashMap<>();
        for (Vacuna v : vacunas.findAll()) {
            porCodigo.put(v.getCodigo(), v);
        }
        Set<String> vistas = new HashSet<>();
        for (EsquemaDosisRequest d : request.dosis()) {
            if (!porCodigo.containsKey(d.vacunaCodigo())) {
                throw new DatosInvalidosException("La vacuna " + d.vacunaCodigo() + " no existe en el catálogo");
            }
            if (!vistas.add(d.vacunaCodigo() + ":" + d.numeroDosis())) {
                throw new DatosInvalidosException(
                        "La dosis " + d.numeroDosis() + " de " + d.vacunaCodigo() + " está repetida");
            }
            if (d.edadRecomendadaDias() < d.edadMinimaDias()) {
                throw new DatosInvalidosException("La edad recomendada no puede ser menor que la mínima");
            }
            if (d.edadMaximaDias() != null && d.edadMaximaDias() < d.edadRecomendadaDias()) {
                throw new DatosInvalidosException("La edad máxima no puede ser menor que la recomendada");
            }
        }
        for (EsquemaDosisRequest d : request.dosis()) {
            if (d.numeroDosis() > 1 && !vistas.contains(d.vacunaCodigo() + ":" + (d.numeroDosis() - 1))) {
                throw new DatosInvalidosException(
                        "La dosis " + d.numeroDosis() + " de " + d.vacunaCodigo() + " requiere la dosis anterior");
            }
        }
        for (EsquemaVacunacion e : existentes) {
            if (e.getVigenteHasta() == null) {
                e.setVigenteHasta(request.vigenteDesde().minusDays(1));
            }
        }
        EsquemaVacunacion nuevo = esquemas.save(EsquemaVacunacion.builder()
                .nombre(request.nombre().strip())
                .vigenteDesde(request.vigenteDesde())
                .porValidar(request.porValidar())
                .fuente(request.fuente())
                .build());
        List<EsquemaDosis> dosis = new ArrayList<>();
        for (EsquemaDosisRequest d : request.dosis()) {
            dosis.add(EsquemaDosis.builder()
                    .esquema(nuevo)
                    .vacuna(porCodigo.get(d.vacunaCodigo()))
                    .numeroDosis(d.numeroDosis())
                    .etiqueta(d.etiqueta().strip())
                    .edadMinimaDias(d.edadMinimaDias())
                    .edadRecomendadaDias(d.edadRecomendadaDias())
                    .edadMaximaDias(d.edadMaximaDias())
                    .intervaloMinimoDias(d.intervaloMinimoDias())
                    .build());
        }
        dosisRepositorio.saveAll(dosis);
        log.info("Esquema de vacunación {} creado por el usuario {}", nuevo.getId(), actor.getId());
        return responder(nuevo, dosisRepositorio.delEsquema(nuevo.getId()));
    }

    public List<PacienteClinicoResumen> pacientes(UsuarioPrincipal actor, String texto) {
        return acceso.listar(actor, texto);
    }

    @Transactional
    public CarneResponse carne(UsuarioPrincipal actor, UUID pacienteId) {
        Paciente paciente = acceso.exigirLectura(actor, pacienteId);
        if (!acceso.esPaciente(actor)) {
            auditoria.registrar(actor, pacienteId, ACCION_CONSULTADO, "Carné de vacunación");
        }
        return construirCarne(paciente);
    }

    public CarneResponse carneMio(UsuarioPrincipal actor) {
        return construirCarne(acceso.propio(actor));
    }

    @Transactional
    public RegistroVacunaResponse registrarDosis(UsuarioPrincipal actor, DosisRequest request) {
        Paciente paciente = acceso.exigirEscritura(actor, request.pacienteId());
        if (paciente.getFechaNacimiento() == null) {
            throw new DatosInvalidosException("El paciente no tiene fecha de nacimiento registrada");
        }
        LocalDate nacimiento = paciente.getFechaNacimiento();
        if (request.fechaAplicacion().isBefore(nacimiento)) {
            throw new DatosInvalidosException("La fecha de aplicación es anterior al nacimiento del paciente");
        }
        EsquemaVacunacion esquema = vigente();
        EsquemaDosis dosis = dosisRepositorio.delEsquema(esquema.getId()).stream()
                .filter(d ->
                        d.getVacuna().getId().equals(request.vacunaId()) && d.getNumeroDosis() == request.numeroDosis())
                .findFirst()
                .orElseThrow(() -> new DatosInvalidosException("La dosis no existe en el esquema vigente"));
        if (registros.existeVigente(paciente.getId(), request.vacunaId(), request.numeroDosis())) {
            throw new ConflictException("Esta dosis ya está registrada en el carné");
        }
        long edadDias = CalculadoraEsquemaVacunacion.edadEnDias(nacimiento, request.fechaAplicacion());
        if (edadDias < dosis.getEdadMinimaDias()) {
            throw new DatosInvalidosException("A esa fecha el paciente no alcanza la edad mínima de esta dosis");
        }
        if (dosis.getEdadMaximaDias() != null && edadDias > dosis.getEdadMaximaDias()) {
            throw new DatosInvalidosException("A esa fecha el paciente supera la edad máxima de esta dosis");
        }
        List<RegistroVacunacion> delPaciente = registros.delPaciente(paciente.getId());
        if (request.numeroDosis() > 1) {
            RegistroVacunacion previa = delPaciente.stream()
                    .filter(r -> r.getEstado() == EstadoRegistroVacunacion.VIGENTE
                            && r.getVacuna().getId().equals(request.vacunaId())
                            && r.getNumeroDosis() == request.numeroDosis() - 1)
                    .findFirst()
                    .orElseThrow(() -> new DatosInvalidosException("Primero debe registrarse la dosis anterior"));
            if (dosis.getIntervaloMinimoDias() != null
                    && request.fechaAplicacion()
                            .isBefore(previa.getFechaAplicacion().plusDays(dosis.getIntervaloMinimoDias()))) {
                throw new DatosInvalidosException("No se cumple el intervalo mínimo de "
                        + dosis.getIntervaloMinimoDias() + " días desde la dosis anterior");
            }
        }
        Atencion atencion = null;
        if (request.atencionId() != null) {
            atencion = atenciones
                    .findById(request.atencionId())
                    .filter(a -> a.getPaciente().getId().equals(paciente.getId()))
                    .orElseThrow(() -> new DatosInvalidosException("La atención indicada no corresponde al paciente"));
        }
        Usuario responsable = usuarios.findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        String establecimiento =
                request.establecimiento() == null || request.establecimiento().isBlank()
                        ? ESTABLECIMIENTO_POR_DEFECTO
                        : request.establecimiento().strip();
        RegistroVacunacion guardado = registros.save(RegistroVacunacion.builder()
                .paciente(paciente)
                .vacuna(dosis.getVacuna())
                .numeroDosis(request.numeroDosis())
                .fechaAplicacion(request.fechaAplicacion())
                .lote(request.lote().strip().toUpperCase())
                .establecimiento(establecimiento)
                .responsable(responsable)
                .atencion(atencion)
                .observaciones(request.observaciones())
                .build());
        auditoria.registrar(
                actor,
                paciente.getId(),
                ACCION_REGISTRADA,
                dosis.getVacuna().getCodigo() + " dosis " + request.numeroDosis());
        return registro(registros.findConDetalle(guardado.getId()).orElseThrow());
    }

    @Transactional
    public RegistroVacunaResponse anular(UsuarioPrincipal actor, UUID registroId, String motivo) {
        RegistroVacunacion registro = registros
                .findConDetalle(registroId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de vacunación", "id", registroId));
        acceso.exigirEscritura(actor, registro.getPaciente().getId());
        if (registro.getEstado() == EstadoRegistroVacunacion.ANULADO) {
            throw new ConflictException("El registro ya está anulado");
        }
        registro.setEstado(EstadoRegistroVacunacion.ANULADO);
        registro.setAnuladoEn(LocalDateTime.now());
        registro.setAnuladoPor(usuarios.getReferenceById(actor.getId()));
        registro.setMotivoAnulacion(motivo.strip());
        auditoria.registrar(
                actor,
                registro.getPaciente().getId(),
                ACCION_ANULADA,
                registro.getVacuna().getCodigo() + " dosis " + registro.getNumeroDosis());
        return registro(registro);
    }

    @Transactional
    public PdfCarne pdf(UsuarioPrincipal actor, UUID pacienteId) {
        Paciente paciente = acceso.exigirLectura(actor, pacienteId);
        CarneResponse carne = construirCarne(paciente);
        String codigo = CodigoDocumentoClinico.nuevo();
        String hash = CodigoDocumentoClinico.sha256(huella(carne));
        CarneVacunacion emision = carnes.save(CarneVacunacion.builder()
                .paciente(paciente)
                .codigoVerificacion(codigo)
                .contenidoHash(hash)
                .emitidoPor(usuarios.getReferenceById(actor.getId()))
                .build());
        auditoria.registrar(actor, paciente.getId(), ACCION_CARNE, "Carné " + codigo);
        byte[] contenido = pdf.renderizar(carne, codigo, hash, actor.getNombreCompleto(), emision.getEmitidoEn());
        return new PdfCarne(contenido, "carne-vacunacion-" + paciente.getDni() + ".pdf");
    }

    @Scheduled(cron = "${app.pediatria.recordatorios-cron:0 0 8 * * *}")
    @Transactional
    public void recordatoriosDiarios() {
        int enviados = enviarRecordatorios(LocalDate.now());
        log.info("Recordatorios de vacunación enviados: {}", enviados);
    }

    @Transactional
    public int enviarRecordatorios(LocalDate hoy) {
        EsquemaVacunacion esquema = esquemas.vigentes(hoy).stream().findFirst().orElse(null);
        if (esquema == null) {
            return 0;
        }
        List<DosisEsquema> dosisEsquema = dosisEsquema(esquema);
        int enviados = 0;
        for (Paciente paciente : accesoClinico.menoresNacidosDesde(hoy.minusYears(EDAD_MAXIMA_RECORDATORIO_ANIOS))) {
            List<DosisAplicada> aplicadas = aplicadas(registros.delPaciente(paciente.getId()));
            for (DosisCalculada c : CalculadoraEsquemaVacunacion.calcular(
                    paciente.getFechaNacimiento(), hoy, dosisEsquema, aplicadas, tolerancia)) {
                boolean candidata = c.estado() == EstadoDosis.ATRASADA
                        || (c.estado() == EstadoDosis.PROXIMA
                                && !c.fechaRecomendada().isAfter(hoy.plusDays(ANTICIPO_RECORDATORIO_DIAS)));
                if (!candidata
                        || recordatorios.existsByPacienteIdAndVacunaIdAndNumeroDosisAndFechaProgramada(
                                paciente.getId(),
                                c.dosis().vacunaId(),
                                c.dosis().numeroDosis(),
                                c.fechaRecomendada())) {
                    continue;
                }
                recordatorios.save(RecordatorioVacunacion.builder()
                        .paciente(paciente)
                        .vacuna(vacunas.getReferenceById(c.dosis().vacunaId()))
                        .numeroDosis(c.dosis().numeroDosis())
                        .fechaProgramada(c.fechaRecomendada())
                        .build());
                notificaciones.crear(
                        paciente.getUsuario(),
                        TipoNotificacion.VACUNA_PROXIMA,
                        "Tiene una vacuna próxima",
                        "Hay una dosis de vacunación programada. Revise el carné de vacunación en su cuenta.",
                        "/mi-vacunacion");
                enviados++;
            }
        }
        return enviados;
    }

    private EsquemaVacunacion vigente() {
        return esquemas.vigentes(LocalDate.now()).stream()
                .findFirst()
                .orElseThrow(
                        () -> new ResourceNotFoundException("Esquema de vacunación vigente", "fecha", LocalDate.now()));
    }

    private CarneResponse construirCarne(Paciente paciente) {
        LocalDate hoy = LocalDate.now();
        EsquemaVacunacion esquema = vigente();
        List<RegistroVacunacion> historial = registros.delPaciente(paciente.getId());
        List<DosisCalculada> calculadas = paciente.getFechaNacimiento() == null
                ? List.of()
                : CalculadoraEsquemaVacunacion.calcular(
                        paciente.getFechaNacimiento(), hoy, dosisEsquema(esquema), aplicadas(historial), tolerancia);
        Map<UUID, RegistroVacunacion> porId = new HashMap<>();
        historial.forEach(r -> porId.put(r.getId(), r));
        List<DosisCarne> dosis =
                calculadas.stream().map(c -> dosisCarne(c, porId)).toList();
        DosisCalculada proxima = CalculadoraEsquemaVacunacion.proxima(calculadas);
        PacienteClinicoResumen resumen = AccesoPaciente.resumen(paciente, hoy);
        return new CarneResponse(
                paciente.getId(),
                resumen.nombre(),
                resumen.dni(),
                resumen.fechaNacimiento(),
                resumen.edadMeses(),
                resumen.sexo(),
                esquema.getNombre(),
                esquema.isPorValidar(),
                (int) dosis.stream()
                        .filter(d -> d.estado() == EstadoDosis.APLICADA)
                        .count(),
                (int) dosis.stream()
                        .filter(d -> d.estado() == EstadoDosis.ATRASADA)
                        .count(),
                (int) dosis.stream()
                        .filter(d -> d.estado() == EstadoDosis.PROXIMA)
                        .count(),
                proxima == null ? null : dosisCarne(proxima, porId),
                dosis,
                historial.stream().map(this::registro).toList());
    }

    private DosisCarne dosisCarne(DosisCalculada c, Map<UUID, RegistroVacunacion> porId) {
        return new DosisCarne(
                c.dosis().vacunaId(),
                c.dosis().vacunaCodigo(),
                c.dosis().vacunaNombre(),
                c.dosis().numeroDosis(),
                c.dosis().etiqueta(),
                c.estado(),
                c.fechaRecomendada(),
                c.fechaAplicada(),
                c.atrasoDias(),
                c.registroId());
    }

    private List<DosisEsquema> dosisEsquema(EsquemaVacunacion esquema) {
        return dosisRepositorio.delEsquema(esquema.getId()).stream()
                .map(d -> new DosisEsquema(
                        d.getVacuna().getId(),
                        d.getVacuna().getCodigo(),
                        d.getVacuna().getNombre(),
                        d.getNumeroDosis(),
                        d.getEtiqueta(),
                        d.getEdadMinimaDias(),
                        d.getEdadRecomendadaDias(),
                        d.getEdadMaximaDias(),
                        d.getIntervaloMinimoDias()))
                .toList();
    }

    private static List<DosisAplicada> aplicadas(List<RegistroVacunacion> historial) {
        return historial.stream()
                .filter(r -> r.getEstado() == EstadoRegistroVacunacion.VIGENTE)
                .map(r ->
                        new DosisAplicada(r.getVacuna().getId(), r.getNumeroDosis(), r.getFechaAplicacion(), r.getId()))
                .toList();
    }

    private RegistroVacunaResponse registro(RegistroVacunacion r) {
        return new RegistroVacunaResponse(
                r.getId(),
                r.getVacuna().getId(),
                r.getVacuna().getNombre(),
                r.getNumeroDosis(),
                r.getFechaAplicacion(),
                r.getLote(),
                r.getEstablecimiento(),
                r.getResponsable().getNombreCompleto(),
                r.getEstado(),
                r.getObservaciones(),
                r.getMotivoAnulacion());
    }

    private EsquemaResponse responder(EsquemaVacunacion e, List<EsquemaDosis> dosis) {
        return new EsquemaResponse(
                e.getId(),
                e.getNombre(),
                e.getVigenteDesde(),
                e.getVigenteHasta(),
                e.isPorValidar(),
                e.isActivo(),
                e.getFuente(),
                dosis.stream()
                        .map(d -> new EsquemaDosisResponse(
                                d.getVacuna().getId(),
                                d.getVacuna().getCodigo(),
                                d.getVacuna().getNombre(),
                                d.getNumeroDosis(),
                                d.getEtiqueta(),
                                d.getEdadMinimaDias(),
                                d.getEdadRecomendadaDias(),
                                d.getEdadMaximaDias(),
                                d.getIntervaloMinimoDias()))
                        .toList());
    }

    private static String huella(CarneResponse carne) {
        StringBuilder texto = new StringBuilder(carne.pacienteId().toString());
        for (RegistroVacunaResponse r : carne.registros()) {
            if (r.estado() == EstadoRegistroVacunacion.VIGENTE) {
                texto.append('|')
                        .append(r.id())
                        .append(':')
                        .append(r.vacunaId())
                        .append(':')
                        .append(r.numeroDosis())
                        .append(':')
                        .append(r.fechaAplicacion())
                        .append(':')
                        .append(r.lote());
            }
        }
        return texto.toString();
    }
}
