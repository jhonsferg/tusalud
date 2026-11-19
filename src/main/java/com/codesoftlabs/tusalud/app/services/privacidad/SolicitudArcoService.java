package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.DecisionArco;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.ResolucionArcoRequest;
import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.SolicitudArcoRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.BloqueoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EventoArcoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PreviaCancelacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.SolicitudArcoDetalle;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.SolicitudArcoResponse;
import com.codesoftlabs.tusalud.app.entities.FinalidadTratamiento;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.SolicitudArco;
import com.codesoftlabs.tusalud.app.entities.SolicitudArcoEvento;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.CampoRectificable;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.OrigenPrivacidad;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoArco;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.facturacion.AlmacenFiscal;
import com.codesoftlabs.tusalud.app.repositories.ContactoCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.FinalidadTratamientoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.SolicitudArcoEventoRepository;
import com.codesoftlabs.tusalud.app.repositories.SolicitudArcoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.ArchivoService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SolicitudArcoService {

    private static final Set<EstadoArco> ABIERTAS = EnumSet.of(EstadoArco.RECIBIDA, EstadoArco.EN_REVISION);

    private final SolicitudArcoRepository solicitudes;
    private final SolicitudArcoEventoRepository eventos;
    private final UsuarioRepository usuarios;
    private final PacienteRepository pacientes;
    private final ContactoCanalRepository contactos;
    private final FinalidadTratamientoRepository finalidades;
    private final ConsentimientosPrivacidadService consentimientos;
    private final ExportacionDatosService exportacion;
    private final AlmacenFiscal almacen;
    private final ArchivoService archivos;
    private final NotificacionService notificaciones;
    private final ObjectMapper mapper;

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public SolicitudArcoResponse crear(UsuarioPrincipal actor, SolicitudArcoRequest r) {
        if (solicitudes.abiertasDelUsuario(actor.getId(), r.tipo(), ABIERTAS) > 0) {
            throw new ConflictException(
                    "Ya tiene una solicitud de " + r.tipo().etiqueta().toLowerCase() + " en trámite");
        }
        FinalidadTratamiento finalidad = null;
        String campo = null;
        String valor = null;
        if (r.tipo() == TipoArco.OPOSICION) {
            if (r.finalidadId() == null) {
                throw new DatosInvalidosException("Indique la finalidad a la que se opone");
            }
            finalidad = finalidades
                    .findById(r.finalidadId())
                    .orElseThrow(() -> new ResourceNotFoundException("Finalidad", "id", r.finalidadId()));
            if (finalidad.isObligatoria()) {
                throw new ConflictException(
                        "Esta finalidad es necesaria para prestar el servicio. Base: " + finalidad.getBaseLegal());
            }
        }
        if (r.tipo() == TipoArco.RECTIFICACION && r.campo() != null) {
            if (r.valorPropuesto() == null || r.valorPropuesto().isBlank()) {
                throw new DatosInvalidosException("Indique el valor correcto del dato");
            }
            campo = r.campo().name();
            valor = r.valorPropuesto().trim();
            validarValor(r.campo(), valor);
        }
        Usuario usuario = usuarios.getReferenceById(actor.getId());
        LocalDateTime ahora = LocalDateTime.now();
        SolicitudArco s = solicitudes.save(SolicitudArco.builder()
                .codigo(nuevoCodigo(ahora))
                .usuario(usuario)
                .tipo(r.tipo())
                .estado(EstadoArco.RECIBIDA)
                .descripcion(r.descripcion().trim())
                .campo(campo)
                .valorPropuesto(valor)
                .finalidad(finalidad)
                .recibidaEn(ahora)
                .venceEn(sumarDiasHabiles(ahora.toLocalDate(), r.tipo().diasHabiles()))
                .build());
        evento(s, usuario, "RECIBIDA", "Solicitud de " + r.tipo().etiqueta().toLowerCase() + " registrada");
        avisarResponsables(s);
        log.info("Solicitud ARCO {} registrada", s.getCodigo());
        return respuesta(s, actor.getNombreCompleto());
    }

    public List<SolicitudArcoResponse> mias(UsuarioPrincipal actor) {
        return solicitudes.deUsuario(actor.getId()).stream()
                .map(s -> respuesta(s, s.getUsuario().getNombreCompleto()))
                .toList();
    }

    public List<SolicitudArcoResponse> bandeja(EstadoArco estado, TipoArco tipo) {
        return solicitudes.bandeja(estado, tipo, PageRequest.of(0, 200)).stream()
                .map(s -> respuesta(s, s.getUsuario().getNombreCompleto()))
                .toList();
    }

    public SolicitudArcoDetalle detalle(UsuarioPrincipal actor, UUID id) {
        SolicitudArco s = cargar(id);
        autorizar(actor, s);
        List<BloqueoResponse> resultado = List.of();
        if (s.getResultadoJson() != null) {
            resultado = mapper.readValue(s.getResultadoJson(), new TypeReference<List<BloqueoResponse>>() {});
        }
        List<EventoArcoResponse> historial = eventos.deSolicitud(id).stream()
                .map(e -> new EventoArcoResponse(
                        e.getId(),
                        e.getAccion(),
                        e.getDetalle(),
                        e.getActor() == null ? "Sistema" : e.getActor().getNombreCompleto(),
                        e.getRegistradoEn()))
                .toList();
        return new SolicitudArcoDetalle(
                respuesta(s, s.getUsuario().getNombreCompleto()),
                s.getFinalidad() == null ? null : s.getFinalidad().getId(),
                s.getFinalidad() == null ? null : s.getFinalidad().getNombre(),
                resultado,
                historial);
    }

    public PreviaCancelacionResponse previaCancelacion(UUID usuarioId) {
        List<BloqueoResponse> categorias = evaluarCancelacion(usuarioId);
        return new PreviaCancelacionResponse(
                categorias, categorias.stream().anyMatch(b -> "BLOQUEADO".equals(b.estado())));
    }

    @Transactional
    public SolicitudArcoResponse tomar(UsuarioPrincipal actor, UUID id) {
        SolicitudArco s = cargar(id);
        if (!s.getEstado().abierta()) {
            throw new ConflictException("La solicitud ya fue resuelta");
        }
        s.setResponsable(usuarios.getReferenceById(actor.getId()));
        s.setEstado(EstadoArco.EN_REVISION);
        evento(s, s.getResponsable(), "ASIGNADA", "Asignada a " + actor.getNombreCompleto());
        return respuesta(s, s.getUsuario().getNombreCompleto());
    }

    @Transactional
    public SolicitudArcoResponse resolver(UsuarioPrincipal actor, UUID id, ResolucionArcoRequest r) {
        SolicitudArco s = cargar(id);
        if (!s.getEstado().abierta()) {
            throw new ConflictException("La solicitud ya fue resuelta");
        }
        Usuario responsable = usuarios.getReferenceById(actor.getId());
        if (s.getResponsable() == null) {
            s.setResponsable(responsable);
        }
        EstadoArco estado = switch (r.decision()) {
            case ATENDER -> EstadoArco.ATENDIDA;
            case ATENDER_PARCIALMENTE -> EstadoArco.PARCIAL;
            case RECHAZAR -> EstadoArco.RECHAZADA;
        };
        String detalle = r.resolucion().trim();
        if (r.decision() != DecisionArco.RECHAZAR) {
            switch (s.getTipo()) {
                case ACCESO -> generarExportacion(s);
                case RECTIFICACION -> {
                    if (s.getCampo() != null) {
                        aplicarRectificacion(s);
                    } else if (r.decision() == DecisionArco.ATENDER) {
                        throw new DatosInvalidosException(
                                "La rectificación de datos clínicos no se aplica desde aquí. Derive al médico tratante y use la resolución parcial");
                    }
                }
                case CANCELACION -> {
                    List<BloqueoResponse> aplicado = aplicarCancelacion(s);
                    s.setResultadoJson(mapper.writeValueAsString(aplicado));
                    if (aplicado.stream().anyMatch(b -> "BLOQUEADO".equals(b.estado()))) {
                        estado = EstadoArco.PARCIAL;
                    }
                }
                case OPOSICION -> {
                    if (s.getFinalidad() == null || s.getFinalidad().isObligatoria()) {
                        throw new ConflictException("La finalidad no admite oposición");
                    }
                    consentimientos.revocarPorSolicitud(
                            s.getUsuario().getId(),
                            s.getFinalidad(),
                            "Oposición " + s.getCodigo(),
                            OrigenPrivacidad.SOLICITUD_ARCO);
                }
            }
        }
        s.setEstado(estado);
        s.setResolucion(detalle);
        s.setResueltaEn(LocalDateTime.now());
        evento(s, responsable, estado.name(), detalle);
        notificaciones.crear(
                s.getUsuario(),
                TipoNotificacion.ARCO_RESUELTA,
                "Respuesta a su solicitud " + s.getCodigo(),
                "Su solicitud de " + s.getTipo().etiqueta().toLowerCase() + " ya tiene respuesta.",
                "/privacidad");
        return respuesta(s, s.getUsuario().getNombreCompleto());
    }

    @Transactional
    public byte[] exportar(UsuarioPrincipal actor, UUID id) {
        SolicitudArco s = cargar(id);
        autorizar(actor, s);
        if (s.getTipo() != TipoArco.ACCESO) {
            throw new ConflictException("Solo las solicitudes de acceso generan una exportación");
        }
        if (s.getExportacionClave() == null) {
            if (s.getEstado().abierta()) {
                throw new ConflictException("La exportación estará disponible cuando se atienda la solicitud");
            }
            throw new ResourceNotFoundException("Exportación de datos", "solicitud", id);
        }
        boolean titular = s.getUsuario().getId().equals(actor.getId());
        evento(
                s,
                usuarios.getReferenceById(actor.getId()),
                "EXPORTACION_DESCARGADA",
                titular ? "Descargada por el titular" : "Descargada por el personal");
        return almacen.leer(s.getExportacionClave());
    }

    public long abiertas() {
        return em.createQuery("SELECT COUNT(s) FROM SolicitudArco s WHERE s.estado IN :e", Long.class)
                .setParameter("e", ABIERTAS)
                .getSingleResult();
    }

    public long vencidas() {
        return em.createQuery(
                        "SELECT COUNT(s) FROM SolicitudArco s WHERE s.estado IN :e AND s.venceEn < :hoy", Long.class)
                .setParameter("e", ABIERTAS)
                .setParameter("hoy", LocalDate.now())
                .getSingleResult();
    }

    private void generarExportacion(SolicitudArco s) {
        byte[] json = exportacion.generar(s.getUsuario().getId());
        s.setExportacionClave(almacen.guardarJson("privacidad/exportaciones", s.getId(), json));
        s.setExportacionSha256(com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico.sha256(json));
    }

    private void aplicarRectificacion(SolicitudArco s) {
        CampoRectificable campo = CampoRectificable.valueOf(s.getCampo());
        String valor = s.getValorPropuesto();
        validarValor(campo, valor);
        Usuario u = usuarios.findById(s.getUsuario().getId()).orElseThrow();
        switch (campo) {
            case NOMBRE -> u.setNombre(valor);
            case APELLIDO -> u.setApellido(valor);
            case TELEFONO -> u.setTelefono(valor);
            case EMAIL -> {
                if (!valor.equalsIgnoreCase(u.getEmail()) && usuarios.existsByEmailIgnoreCase(valor)) {
                    throw new ConflictException("Ese correo ya está registrado por otra cuenta");
                }
                u.setEmail(valor.toLowerCase());
            }
            case FECHA_NACIMIENTO -> paciente(u).setFechaNacimiento(LocalDate.parse(valor));
            case DNI -> {
                Paciente p = paciente(u);
                if (!valor.equals(p.getDni()) && pacientes.existsByDni(valor)) {
                    throw new ConflictException("Ese documento ya está registrado");
                }
                p.setDni(valor);
            }
        }
    }

    private Paciente paciente(Usuario u) {
        return pacientes
                .findByUsuarioId(u.getId())
                .orElseThrow(() -> new ConflictException("La cuenta no tiene ficha de paciente"));
    }

    private static void validarValor(CampoRectificable campo, String valor) {
        boolean valido = switch (campo) {
            case NOMBRE, APELLIDO -> valor.length() >= 2 && valor.length() <= 60;
            case TELEFONO -> valor.matches("[0-9+ ]{7,20}");
            case EMAIL -> valor.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+") && valor.length() <= 120;
            case DNI -> valor.matches("\\d{8}");
            case FECHA_NACIMIENTO -> {
                try {
                    yield LocalDate.parse(valor).isBefore(LocalDate.now());
                } catch (RuntimeException ex) {
                    yield false;
                }
            }
        };
        if (!valido) {
            throw new DatosInvalidosException(
                    "El valor propuesto no es válido para " + campo.etiqueta().toLowerCase());
        }
    }

    private List<BloqueoResponse> aplicarCancelacion(SolicitudArco s) {
        UUID usuarioId = s.getUsuario().getId();
        List<BloqueoResponse> evaluadas = evaluarCancelacion(usuarioId);
        for (BloqueoResponse b : evaluadas) {
            if (!"CANCELABLE".equals(b.estado())) {
                continue;
            }
            switch (b.categoria()) {
                case "Canales de notificación" -> contactos.deleteAll(contactos.findByUsuarioId(usuarioId));
                case "Consentimientos opcionales" ->
                    consentimientos.revocarOpcionales(
                            usuarioId, "Cancelación " + s.getCodigo(), OrigenPrivacidad.SOLICITUD_ARCO);
                case "Foto de perfil" -> {
                    Usuario u = usuarios.findById(usuarioId).orElseThrow();
                    var avatar = u.getAvatarArchivo();
                    u.setAvatarArchivo(null);
                    if (avatar != null) {
                        archivos.eliminarSinAutorizar(avatar);
                    }
                }
                default -> {}
            }
        }
        return evaluadas;
    }

    private List<BloqueoResponse> evaluarCancelacion(UUID usuarioId) {
        List<BloqueoResponse> lista = new ArrayList<>();
        boolean clinico = contar("SELECT COUNT(a) FROM Atencion a WHERE a.paciente.usuario.id = :u", usuarioId) > 0
                || contar("SELECT COUNT(c) FROM Cita c WHERE c.paciente.usuario.id = :u", usuarioId) > 0;
        if (clinico) {
            lista.add(new BloqueoResponse(
                    "Historia clínica y citas",
                    "BLOQUEADO",
                    "La historia clínica debe conservarse durante el plazo que fija la norma sectorial. No puede cancelarse mientras corra ese plazo. Si un dato es inexacto, solicite su rectificación.",
                    "Norma técnica de la historia clínica, por confirmar con asesor legal"));
            lista.add(new BloqueoResponse(
                    "Consentimientos informados",
                    "BLOQUEADO",
                    "Forman parte de la historia clínica y respaldan los procedimientos realizados.",
                    "Norma técnica de la historia clínica, por confirmar con asesor legal"));
            lista.add(new BloqueoResponse(
                    "Cuenta de acceso",
                    "BLOQUEADO",
                    "La cuenta se mantiene mientras exista historia clínica, para que pueda consultarla.",
                    "Derecho de acceso del titular"));
        }
        if (contar("SELECT COUNT(c) FROM Comprobante c WHERE c.paciente.usuario.id = :u", usuarioId) > 0) {
            lista.add(new BloqueoResponse(
                    "Comprobantes de pago",
                    "BLOQUEADO",
                    "Los comprobantes deben conservarse por el plazo de prescripción tributaria.",
                    "Obligaciones tributarias de conservación, por confirmar con asesor legal"));
        }
        lista.add(new BloqueoResponse(
                "Registros de auditoría",
                "BLOQUEADO",
                "Los accesos y eventos de seguridad se conservan para proteger el banco de datos.",
                "Medidas de seguridad de la Ley 29733, por confirmar con asesor legal"));
        if (!contactos.findByUsuarioId(usuarioId).isEmpty()) {
            lista.add(new BloqueoResponse(
                    "Canales de notificación",
                    "CANCELABLE",
                    "Se eliminan el correo, el teléfono y el WhatsApp registrados para avisos.",
                    "Consentimiento del titular"));
        }
        if (!consentimientos.mios(usuarioId).stream()
                .filter(c -> "OTORGADO".equals(c.estado()))
                .toList()
                .isEmpty()) {
            lista.add(new BloqueoResponse(
                    "Consentimientos opcionales",
                    "CANCELABLE",
                    "Se revocan los consentimientos otorgados para finalidades opcionales.",
                    "Consentimiento del titular"));
        }
        if (usuarios.findById(usuarioId).map(u -> u.getAvatarArchivo() != null).orElse(false)) {
            lista.add(new BloqueoResponse(
                    "Foto de perfil", "CANCELABLE", "Se elimina la foto de perfil.", "Consentimiento del titular"));
        }
        return lista;
    }

    private long contar(String jpql, UUID usuarioId) {
        return em.createQuery(jpql, Long.class).setParameter("u", usuarioId).getSingleResult();
    }

    private void avisarResponsables(SolicitudArco s) {
        Set<UUID> vistos = new LinkedHashSet<>();
        List<Usuario> destinatarios = new ArrayList<>();
        for (RolNombre rol : List.of(RolNombre.ROLE_CALIDAD, RolNombre.ROLE_ADMIN)) {
            for (Usuario u : usuarios.findActivosPorRol(rol)) {
                if (vistos.add(u.getId())) {
                    destinatarios.add(u);
                }
            }
        }
        for (Usuario u : destinatarios) {
            notificaciones.crear(
                    u,
                    TipoNotificacion.ARCO_RECIBIDA,
                    "Nueva solicitud de privacidad",
                    "Solicitud " + s.getCodigo() + " de "
                            + s.getTipo().etiqueta().toLowerCase() + ", vence el " + s.getVenceEn() + ".",
                    "/privacidad/solicitudes");
        }
    }

    private void evento(SolicitudArco s, Usuario actor, String accion, String detalle) {
        eventos.save(SolicitudArcoEvento.builder()
                .solicitud(s)
                .actor(actor)
                .accion(accion)
                .detalle(detalle)
                .registradoEn(LocalDateTime.now())
                .build());
    }

    private SolicitudArcoResponse respuesta(SolicitudArco s, String solicitante) {
        boolean vencida = s.getEstado().abierta() && s.getVenceEn().isBefore(LocalDate.now());
        return new SolicitudArcoResponse(
                s.getId(),
                s.getCodigo(),
                s.getTipo(),
                s.getEstado(),
                s.getDescripcion(),
                s.getCampo(),
                s.getValorPropuesto(),
                solicitante,
                s.getUsuario().getId(),
                s.getResponsable() == null ? null : s.getResponsable().getNombreCompleto(),
                s.getRecibidaEn(),
                s.getVenceEn(),
                vencida,
                s.getResueltaEn(),
                s.getResolucion(),
                s.getExportacionClave() != null);
    }

    private SolicitudArco cargar(UUID id) {
        return solicitudes
                .findConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitud de privacidad", "id", id));
    }

    private static void autorizar(UsuarioPrincipal actor, SolicitudArco s) {
        boolean titular = s.getUsuario().getId().equals(actor.getId());
        boolean personal = actor.getRoles().contains(RolNombre.ROLE_ADMIN.name())
                || actor.getRoles().contains(RolNombre.ROLE_CALIDAD.name());
        if (!titular && !personal) {
            throw new ForbiddenOperationException("No tiene acceso a esta solicitud");
        }
    }

    private String nuevoCodigo(LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        long total = solicitudes.contarEntre(
                hoy.withDayOfYear(1).atStartOfDay(), hoy.plusDays(1).atStartOfDay());
        return String.format("ARCO-%d-%05d", hoy.getYear(), total + 1);
    }

    public static LocalDate sumarDiasHabiles(LocalDate desde, int dias) {
        LocalDate fecha = desde;
        int restantes = dias;
        while (restantes > 0) {
            fecha = fecha.plusDays(1);
            if (fecha.getDayOfWeek() != DayOfWeek.SATURDAY && fecha.getDayOfWeek() != DayOfWeek.SUNDAY) {
                restantes--;
            }
        }
        return fecha;
    }
}
