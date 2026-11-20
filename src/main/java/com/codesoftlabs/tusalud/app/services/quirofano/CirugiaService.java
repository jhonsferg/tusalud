package com.codesoftlabs.tusalud.app.services.quirofano;

import com.codesoftlabs.tusalud.app.dto.request.DocumentosMedicosRequests.ConsentimientoSolicitudRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.ChecklistRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.EquipoQuirofanoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.InsumoCirugiaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.MiembroEquipoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.NotaOperatoriaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.ProgramacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.QuirofanoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.RegistroAnestesicoRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.SolicitudCirugiaRequest;
import com.codesoftlabs.tusalud.app.dto.request.QuirofanoRequests.UsoSetRequest;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.ChecklistResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CirugiaDetalle;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.CirugiaResumen;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.ConsentimientoCirugia;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.EquipoQuirofanoResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.EventoCirugiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.InsumoCirugiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.InsumoDisponibleResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.MiCirugiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.MiembroEquipoResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.NotaOperatoriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.QuirofanoResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.RegistroAnestesicoResponse;
import com.codesoftlabs.tusalud.app.dto.response.QuirofanoResponses.SetUsadoResponse;
import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import com.codesoftlabs.tusalud.app.entities.Cirugia;
import com.codesoftlabs.tusalud.app.entities.CirugiaChecklist;
import com.codesoftlabs.tusalud.app.entities.CirugiaEquipo;
import com.codesoftlabs.tusalud.app.entities.CirugiaEvento;
import com.codesoftlabs.tusalud.app.entities.CirugiaInsumo;
import com.codesoftlabs.tusalud.app.entities.CirugiaSet;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.LoteEsteril;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.Quirofano;
import com.codesoftlabs.tusalud.app.entities.QuirofanoEquipo;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoEfectivoLote;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoLoteEsteril;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoQuirofano;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.MomentoChecklist;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.RolQuirurgico;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoAnestesia;
import com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoCirugia;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.Cie10CatalogoRepository;
import com.codesoftlabs.tusalud.app.repositories.CirugiaChecklistRepository;
import com.codesoftlabs.tusalud.app.repositories.CirugiaEquipoRepository;
import com.codesoftlabs.tusalud.app.repositories.CirugiaEventoRepository;
import com.codesoftlabs.tusalud.app.repositories.CirugiaInsumoRepository;
import com.codesoftlabs.tusalud.app.repositories.CirugiaRepository;
import com.codesoftlabs.tusalud.app.repositories.CirugiaSetRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoPlantillaRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.IngresoHospitalarioRepository;
import com.codesoftlabs.tusalud.app.repositories.LoteEsterilRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.ProductoRepository;
import com.codesoftlabs.tusalud.app.repositories.QuirofanoEquipoRepository;
import com.codesoftlabs.tusalud.app.repositories.QuirofanoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.documentosmedicos.ConsentimientoService;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaClinicaService;
import com.codesoftlabs.tusalud.app.services.interfaces.InventarioService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CirugiaService {

    public static final String PLANTILLA_QUIRURGICA = "CIRUGIA_MAYOR";
    public static final String PLANTILLA_ANESTESICA = "ANESTESIA_GENERAL";
    private static final UUID SIN_EXCLUSION = new UUID(0L, 0L);
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CirugiaRepository cirugias;
    private final CirugiaEquipoRepository equipos;
    private final CirugiaChecklistRepository checklists;
    private final CirugiaInsumoRepository insumos;
    private final CirugiaEventoRepository eventos;
    private final CirugiaSetRepository usosDeSets;
    private final QuirofanoRepository quirofanos;
    private final QuirofanoEquipoRepository equiposQuirofano;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarios;
    private final Cie10CatalogoRepository cie10;
    private final IngresoHospitalarioRepository ingresos;
    private final ConsentimientoPlantillaRepository plantillas;
    private final ConsentimientoRepository consentimientos;
    private final ConsentimientoService consentimientoService;
    private final ProductoRepository productos;
    private final InventarioService inventario;
    private final LoteEsterilRepository lotes;
    private final RelacionAsistencialService relacion;
    private final AuditoriaClinicaService auditoria;
    private final NotificacionService notificaciones;
    private final SecuenciasQuirofano secuencias;

    public List<QuirofanoResponse> quirofanos(boolean soloActivos) {
        List<Quirofano> lista = quirofanos.findAllByOrderByCodigoAsc().stream()
                .filter(q -> !soloActivos || q.isActivo())
                .toList();
        Map<UUID, List<QuirofanoEquipo>> porQuirofano = lista.isEmpty()
                ? Map.of()
                : equiposQuirofano
                        .deQuirofanos(lista.stream().map(Quirofano::getId).toList())
                        .stream()
                        .collect(Collectors.groupingBy(e -> e.getQuirofano().getId()));
        return lista.stream()
                .map(q -> quirofano(q, porQuirofano.getOrDefault(q.getId(), List.of())))
                .toList();
    }

    @Transactional
    public QuirofanoResponse crearQuirofano(QuirofanoRequest request) {
        String codigo = request.codigo().strip().toUpperCase();
        if (quirofanos.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe un quirófano con el código " + codigo);
        }
        Quirofano q = quirofanos.save(Quirofano.builder()
                .codigo(codigo)
                .nombre(request.nombre().strip())
                .ubicacion(request.ubicacion())
                .estado(request.estado())
                .activo(request.activo() == null || request.activo())
                .observaciones(request.observaciones())
                .build());
        return quirofano(q, reemplazarEquipos(q, request.equipos()));
    }

    @Transactional
    public QuirofanoResponse actualizarQuirofano(UUID id, QuirofanoRequest request) {
        Quirofano q = quirofanos.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quirófano", "id", id));
        if (request.estado() != EstadoQuirofano.DISPONIBLE || Boolean.FALSE.equals(request.activo())) {
            boolean ocupado = cirugias
                    .solapadasEnQuirofano(
                            id, LocalDateTime.now(), LocalDateTime.now().plusYears(5), SIN_EXCLUSION)
                    .stream()
                    .anyMatch(c -> c.getEstado() == EstadoCirugia.EN_CURSO);
            if (ocupado) {
                throw new ConflictException("El quirófano tiene una cirugía en curso");
            }
        }
        q.setNombre(request.nombre().strip());
        q.setUbicacion(request.ubicacion());
        q.setEstado(request.estado());
        if (request.activo() != null) {
            q.setActivo(request.activo());
        }
        q.setObservaciones(request.observaciones());
        return quirofano(q, reemplazarEquipos(q, request.equipos()));
    }

    public List<CirugiaResumen> agenda(
            UsuarioPrincipal actor, LocalDateTime desde, LocalDateTime hasta, UUID quirofanoId, EstadoCirugia estado) {
        exigirPersonal(actor);
        List<Cirugia> lista = cirugias.enRango(desde, hasta).stream()
                .filter(c -> quirofanoId == null
                        || (c.getQuirofano() != null && c.getQuirofano().getId().equals(quirofanoId)))
                .filter(c -> estado != null ? c.getEstado() == estado : c.getEstado() != EstadoCirugia.CANCELADA)
                .toList();
        Set<UUID> miEquipo = lista.isEmpty()
                ? Set.of()
                : equipos.deCirugias(lista.stream().map(Cirugia::getId).toList()).stream()
                        .filter(e -> e.getUsuario().getId().equals(actor.getId()))
                        .map(e -> e.getCirugia().getId())
                        .collect(Collectors.toSet());
        return lista.stream()
                .map(c -> resumen(actor, c, miEquipo.contains(c.getId())))
                .toList();
    }

    @Transactional
    public CirugiaDetalle detalle(UsuarioPrincipal actor, UUID id) {
        Cirugia c = cargar(id);
        exigirPersonal(actor);
        boolean enEquipo = equipos.existsByCirugiaIdAndUsuarioId(id, actor.getId());
        if (esMedico(actor) && !puedeVerDetalle(actor, c, enEquipo)) {
            throw new ForbiddenOperationException("No tiene relación con esta cirugía ni con el paciente");
        }
        boolean clinico = clinicoVisible(actor, c, enEquipo);
        if (clinico) {
            auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_CONSULTADA", c.getCodigo());
        }
        return construirDetalle(actor, c, enEquipo, clinico);
    }

    public List<MiCirugiaResponse> mias(UsuarioPrincipal actor) {
        Paciente paciente = pacienteRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo un paciente consulta sus cirugías"));
        return cirugias.deLaPersona(paciente.getId()).stream()
                .map(c -> new MiCirugiaResponse(
                        c.getId(),
                        c.getCodigo(),
                        c.getEstado(),
                        c.getProcedimiento(),
                        c.getQuirofano() == null ? null : c.getQuirofano().getNombre(),
                        c.getInicioProgramado(),
                        c.getSolicitante().getUsuario().getNombreCompleto(),
                        consentimiento(c.getConsentimientoQuirurgico()),
                        consentimiento(c.getConsentimientoAnestesico())))
                .toList();
    }

    public List<MiembroEquipoResponse> personalDisponible(UsuarioPrincipal actor, RolQuirurgico rol) {
        exigirPersonal(actor);
        RolNombre requerido = rol.esMedico() ? RolNombre.ROLE_MEDICO : RolNombre.ROLE_ENFERMERIA;
        return usuarios.activosPorRol(requerido).stream()
                .map(u -> new MiembroEquipoResponse(u.getId(), u.getNombreCompleto(), rol))
                .sorted(java.util.Comparator.comparing(MiembroEquipoResponse::nombre))
                .toList();
    }

    public List<InsumoDisponibleResponse> insumosDisponibles(UsuarioPrincipal actor, String texto) {
        exigirPersonal(actor);
        String patron =
                texto == null || texto.isBlank() ? null : "%" + texto.strip().toLowerCase() + "%";
        List<Producto> lista =
                productos.buscar(patron, null, true, org.springframework.data.domain.PageRequest.of(0, 100)).stream()
                        .filter(p -> p.getTipo() == com.codesoftlabs.tusalud.app.entities.enums.TipoProducto.INSUMO
                                || p.getTipo() == com.codesoftlabs.tusalud.app.entities.enums.TipoProducto.MATERIAL)
                        .toList();
        Map<UUID, InventarioService.StockProducto> stock =
                inventario.stockDe(lista.stream().map(Producto::getId).toList());
        return lista.stream()
                .map(p -> new InsumoDisponibleResponse(
                        p.getId(),
                        p.getCodigo(),
                        p.getNombre(),
                        p.getUnidad(),
                        stock.containsKey(p.getId()) ? stock.get(p.getId()).stock() : 0))
                .toList();
    }

    @Transactional
    public CirugiaDetalle solicitar(UsuarioPrincipal actor, SolicitudCirugiaRequest request) {
        Medico solicitante = medicoDe(actor);
        Paciente paciente = pacienteRepository
                .findByIdWithUsuario(request.pacienteId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", request.pacienteId()));
        relacion.exigirRelacion(actor, paciente.getId(), "No tiene relación asistencial con este paciente");
        Cie10Catalogo diagnostico = cie10.findByCodigo(
                        request.diagnosticoCodigo().strip().toUpperCase())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Diagnóstico CIE-10", "codigo", request.diagnosticoCodigo()));
        if (!diagnostico.isActivo()) {
            throw new ConflictException("El diagnóstico CIE-10 no está activo");
        }
        IngresoHospitalario ingreso = null;
        if (request.ingresoId() != null) {
            ingreso = ingresos.findById(request.ingresoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Ingreso", "id", request.ingresoId()));
            if (!ingreso.getPaciente().getId().equals(paciente.getId())) {
                throw new DatosInvalidosException("El ingreso no corresponde a este paciente");
            }
            if (ingreso.getEstado() != EstadoIngresoHosp.ACTIVO) {
                throw new ConflictException("El ingreso ya no está activo");
            }
        }
        Cirugia c = cirugias.save(Cirugia.builder()
                .codigo(secuencias.codigo("CIR", SecuenciasQuirofano.CIRUGIA))
                .paciente(paciente)
                .solicitante(solicitante)
                .cie10Id(diagnostico.getId())
                .diagnosticoCodigo(diagnostico.getCodigo())
                .diagnosticoDescripcion(diagnostico.getDescripcion())
                .procedimiento(request.procedimiento().strip())
                .tipo(request.tipo())
                .tipoAnestesia(request.tipoAnestesia())
                .ingreso(ingreso)
                .indicacionesPreoperatorias(request.indicacionesPreoperatorias())
                .build());
        registrar(actor, c, "SOLICITADA", request.tipo().name());
        auditoria.registrar(actor, paciente.getId(), "CIRUGIA_SOLICITADA", c.getCodigo());
        usuarios.activosPorRol(RolNombre.ROLE_ENFERMERIA)
                .forEach(u -> notificaciones.crear(
                        u,
                        TipoNotificacion.SISTEMA,
                        "Solicitud quirúrgica pendiente",
                        "Hay una cirugía " + c.getCodigo() + " pendiente de programar.",
                        "/quirofano"));
        return construirDetalle(actor, c, false, true);
    }

    @Transactional
    public CirugiaDetalle programar(UsuarioPrincipal actor, UUID id, ProgramacionRequest request) {
        Cirugia c = cargar(id);
        exigirOperador(actor, c);
        if (c.getEstado() != EstadoCirugia.SOLICITADA
                && c.getEstado() != EstadoCirugia.SUSPENDIDA
                && c.getEstado() != EstadoCirugia.PROGRAMADA) {
            throw new ConflictException("La cirugía no se puede programar en estado " + c.getEstado());
        }
        LocalDateTime inicio = request.inicio();
        if (c.getTipo() == TipoCirugia.ELECTIVA
                && inicio.isBefore(LocalDateTime.now().minusMinutes(5))) {
            throw new DatosInvalidosException("Una cirugía electiva no se programa en el pasado");
        }
        LocalDateTime fin = inicio.plusMinutes(request.duracionMin());
        Quirofano q = quirofanos
                .bloquear(request.quirofanoId())
                .orElseThrow(() -> new ResourceNotFoundException("Quirófano", "id", request.quirofanoId()));
        if (!q.isActivo() || q.getEstado() != EstadoQuirofano.DISPONIBLE) {
            throw new ConflictException("El quirófano " + q.getNombre() + " no está disponible");
        }
        if (!cirugias.solapadasEnQuirofano(q.getId(), inicio, fin, c.getId()).isEmpty()) {
            throw new ConflictException("El quirófano ya tiene una cirugía en ese horario");
        }
        List<Usuario> miembros = validarEquipo(c, request.equipo(), inicio, fin);
        equipos.deleteByCirugiaId(c.getId());
        equipos.flush();
        for (int i = 0; i < request.equipo().size(); i++) {
            MiembroEquipoRequest m = request.equipo().get(i);
            equipos.save(CirugiaEquipo.builder()
                    .cirugia(c)
                    .usuario(miembros.get(i))
                    .rol(m.rol())
                    .build());
        }
        c.setQuirofano(q);
        c.setInicioProgramado(inicio);
        c.setFinProgramado(fin);
        c.setEstado(EstadoCirugia.PROGRAMADA);
        c.setMotivo(null);
        c.setActualizadoEn(LocalDateTime.now());
        registrar(actor, c, "PROGRAMADA", q.getCodigo() + " " + FORMATO.format(inicio));
        auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_PROGRAMADA", c.getCodigo());
        notificaciones.crear(
                c.getPaciente().getUsuario(),
                TipoNotificacion.CIRUGIA_PROGRAMADA,
                "Tiene un procedimiento programado",
                "Se programó un procedimiento a su nombre para el " + FORMATO.format(inicio) + ".",
                "/mis-cirugias");
        miembros.stream()
                .distinct()
                .forEach(u -> notificaciones.crear(
                        u,
                        TipoNotificacion.CIRUGIA_ASIGNADA,
                        "Cirugía asignada",
                        "Fue asignado a la cirugía " + c.getCodigo() + " del " + FORMATO.format(inicio) + " en "
                                + q.getNombre() + ".",
                        "/quirofano/" + c.getId()));
        return construirDetalle(actor, c, equipos.existsByCirugiaIdAndUsuarioId(c.getId(), actor.getId()), true);
    }

    @Transactional
    public CirugiaDetalle solicitarConsentimientos(UsuarioPrincipal actor, UUID id) {
        Cirugia c = cargar(id);
        exigirMedicoResponsable(actor, c);
        if (!EnumSet.of(
                        EstadoCirugia.SOLICITADA,
                        EstadoCirugia.PROGRAMADA,
                        EstadoCirugia.EN_PREPARACION,
                        EstadoCirugia.SUSPENDIDA)
                .contains(c.getEstado())) {
            throw new ConflictException("No se solicitan consentimientos en estado " + c.getEstado());
        }
        boolean cambio = false;
        if (requiereNuevo(c.getConsentimientoQuirurgico())) {
            c.setConsentimientoQuirurgico(solicitarConsentimiento(actor, c, PLANTILLA_QUIRURGICA, "Cirugía"));
            cambio = true;
        }
        if (c.getTipoAnestesia().requiereConsentimientoAnestesico() && requiereNuevo(c.getConsentimientoAnestesico())) {
            c.setConsentimientoAnestesico(solicitarConsentimiento(actor, c, PLANTILLA_ANESTESICA, "Anestesia"));
            cambio = true;
        }
        if (!cambio) {
            throw new ConflictException("Los consentimientos ya fueron solicitados o firmados");
        }
        c.setActualizadoEn(LocalDateTime.now());
        registrar(actor, c, "CONSENTIMIENTOS_SOLICITADOS", null);
        return construirDetalle(actor, c, equipos.existsByCirugiaIdAndUsuarioId(id, actor.getId()), true);
    }

    @Transactional
    public CirugiaDetalle preparar(UsuarioPrincipal actor, UUID id) {
        Cirugia c = cargar(id);
        exigirOperador(actor, c);
        if (c.getEstado() != EstadoCirugia.PROGRAMADA) {
            throw new ConflictException("Solo una cirugía programada pasa a preparación");
        }
        c.setEstado(EstadoCirugia.EN_PREPARACION);
        c.setActualizadoEn(LocalDateTime.now());
        registrar(actor, c, "EN_PREPARACION", null);
        return construirDetalle(actor, c, equipos.existsByCirugiaIdAndUsuarioId(id, actor.getId()), true);
    }

    @Transactional
    public CirugiaDetalle registrarChecklist(UsuarioPrincipal actor, UUID id, ChecklistRequest request) {
        Cirugia c = cargar(id);
        exigirEquipo(actor, c);
        MomentoChecklist momento = request.momento();
        boolean induccion = checklists.existsByCirugiaIdAndMomento(id, MomentoChecklist.ANTES_INDUCCION);
        boolean incision = checklists.existsByCirugiaIdAndMomento(id, MomentoChecklist.ANTES_INCISION);
        switch (momento) {
            case ANTES_INDUCCION -> {
                if (c.getEstado() != EstadoCirugia.EN_PREPARACION && c.getEstado() != EstadoCirugia.EN_CURSO) {
                    throw new ConflictException("La lista antes de la inducción se completa durante la preparación");
                }
            }
            case ANTES_INCISION -> {
                if (c.getEstado() != EstadoCirugia.EN_CURSO) {
                    throw new ConflictException("La lista antes de la incisión se completa con la cirugía en curso");
                }
                if (!induccion) {
                    throw new ConflictException("Falta completar la lista antes de la inducción");
                }
            }
            case ANTES_SALIDA -> {
                if (c.getEstado() != EstadoCirugia.EN_CURSO) {
                    throw new ConflictException("La lista antes de salir se completa con la cirugía en curso");
                }
                if (!incision) {
                    throw new ConflictException("Falta completar la lista antes de la incisión");
                }
            }
        }
        if (checklists.existsByCirugiaIdAndMomento(id, momento)) {
            throw new ConflictException("Esta lista de verificación ya fue registrada");
        }
        Set<String> confirmados = new HashSet<>(request.items());
        List<String> faltantes = momento.items().stream()
                .filter(item -> !confirmados.contains(item))
                .toList();
        if (!faltantes.isEmpty()) {
            throw new DatosInvalidosException("Faltan puntos por confirmar: " + String.join(", ", faltantes));
        }
        Usuario responsable = usuario(actor.getId());
        checklists.save(CirugiaChecklist.builder()
                .cirugia(c)
                .momento(momento)
                .items(String.join(",", momento.items()))
                .observaciones(request.observaciones())
                .completadoPor(responsable)
                .build());
        registrar(actor, c, "CHECKLIST_" + momento.name(), null);
        return construirDetalle(actor, c, true, true);
    }

    @Transactional
    public CirugiaDetalle iniciar(UsuarioPrincipal actor, UUID id) {
        Cirugia c = cargar(id);
        exigirRolEnEquipo(actor, c, RolQuirurgico.CIRUJANO, "Solo el cirujano del equipo inicia la cirugía");
        List<String> pendientes = pendientesParaIniciar(c);
        if (!pendientes.isEmpty()) {
            throw new ConflictException("No se puede iniciar: " + String.join("; ", pendientes));
        }
        c.setEstado(EstadoCirugia.EN_CURSO);
        c.setInicioReal(LocalDateTime.now());
        c.setActualizadoEn(LocalDateTime.now());
        registrar(actor, c, "INICIADA", null);
        auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_INICIADA", c.getCodigo());
        return construirDetalle(actor, c, true, true);
    }

    @Transactional
    public CirugiaDetalle finalizar(UsuarioPrincipal actor, UUID id) {
        Cirugia c = cargar(id);
        exigirRolEnEquipo(actor, c, RolQuirurgico.CIRUJANO, "Solo el cirujano del equipo finaliza la cirugía");
        List<String> pendientes = pendientesParaFinalizar(c);
        if (!pendientes.isEmpty()) {
            throw new ConflictException("No se puede finalizar: " + String.join("; ", pendientes));
        }
        c.setEstado(EstadoCirugia.FINALIZADA);
        c.setFinReal(LocalDateTime.now());
        c.setActualizadoEn(LocalDateTime.now());
        liberarSets(c);
        registrar(actor, c, "FINALIZADA", null);
        auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_FINALIZADA", c.getCodigo());
        return construirDetalle(actor, c, true, true);
    }

    @Transactional
    public CirugiaDetalle suspender(UsuarioPrincipal actor, UUID id, String motivo) {
        Cirugia c = cargar(id);
        exigirOperador(actor, c);
        if (!EnumSet.of(EstadoCirugia.PROGRAMADA, EstadoCirugia.EN_PREPARACION, EstadoCirugia.EN_CURSO)
                .contains(c.getEstado())) {
            throw new ConflictException("No se puede suspender una cirugía en estado " + c.getEstado());
        }
        c.setEstado(EstadoCirugia.SUSPENDIDA);
        c.setMotivo(motivo.strip());
        c.setActualizadoEn(LocalDateTime.now());
        liberarSets(c);
        registrar(actor, c, "SUSPENDIDA", motivo.strip());
        auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_SUSPENDIDA", c.getCodigo());
        return construirDetalle(actor, c, equipos.existsByCirugiaIdAndUsuarioId(id, actor.getId()), true);
    }

    @Transactional
    public CirugiaDetalle cancelar(UsuarioPrincipal actor, UUID id, String motivo) {
        Cirugia c = cargar(id);
        exigirOperador(actor, c);
        if (!EnumSet.of(
                        EstadoCirugia.SOLICITADA,
                        EstadoCirugia.PROGRAMADA,
                        EstadoCirugia.EN_PREPARACION,
                        EstadoCirugia.SUSPENDIDA)
                .contains(c.getEstado())) {
            throw new ConflictException("No se puede cancelar una cirugía en estado " + c.getEstado());
        }
        c.setEstado(EstadoCirugia.CANCELADA);
        c.setMotivo(motivo.strip());
        c.setActualizadoEn(LocalDateTime.now());
        liberarSets(c);
        registrar(actor, c, "CANCELADA", motivo.strip());
        auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_CANCELADA", c.getCodigo());
        return construirDetalle(actor, c, equipos.existsByCirugiaIdAndUsuarioId(id, actor.getId()), true);
    }

    @Transactional
    public CirugiaDetalle notaOperatoria(UsuarioPrincipal actor, UUID id, NotaOperatoriaRequest request) {
        Cirugia c = cargar(id);
        exigirRolEnEquipo(actor, c, RolQuirurgico.CIRUJANO, "Solo el cirujano del equipo registra la nota operatoria");
        if (c.getEstado() != EstadoCirugia.EN_CURSO) {
            throw new ConflictException("La nota operatoria se registra con la cirugía en curso");
        }
        c.setHallazgos(request.hallazgos().strip());
        c.setTecnica(request.tecnica().strip());
        c.setComplicaciones(request.complicaciones());
        c.setSangradoMl(request.sangradoMl());
        c.setDiagnosticoPostoperatorio(request.diagnosticoPostoperatorio().strip());
        c.setIndicacionesPostoperatorias(request.indicacionesPostoperatorias());
        c.setNotaRegistradaEn(LocalDateTime.now());
        c.setNotaPor(usuario(actor.getId()));
        c.setActualizadoEn(LocalDateTime.now());
        registrar(actor, c, "NOTA_OPERATORIA", null);
        auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_NOTA_OPERATORIA", c.getCodigo());
        return construirDetalle(actor, c, true, true);
    }

    @Transactional
    public CirugiaDetalle registroAnestesico(UsuarioPrincipal actor, UUID id, RegistroAnestesicoRequest request) {
        Cirugia c = cargar(id);
        exigirRolEnEquipo(
                actor, c, RolQuirurgico.ANESTESIOLOGO, "Solo el anestesiólogo del equipo registra la anestesia");
        if (c.getEstado() != EstadoCirugia.EN_CURSO) {
            throw new ConflictException("El registro anestésico se completa con la cirugía en curso");
        }
        if (!request.fin().isAfter(request.inicio())) {
            throw new DatosInvalidosException("El fin de la anestesia debe ser posterior al inicio");
        }
        c.setAsa(request.asa().shortValue());
        c.setAnestesiaInicio(request.inicio());
        c.setAnestesiaFin(request.fin());
        c.setAnestesiaObservaciones(request.observaciones());
        c.setAnestesiaRegistradaEn(LocalDateTime.now());
        c.setAnestesiaPor(usuario(actor.getId()));
        c.setActualizadoEn(LocalDateTime.now());
        registrar(actor, c, "REGISTRO_ANESTESICO", "ASA " + request.asa());
        auditoria.registrar(actor, c.getPaciente().getId(), "CIRUGIA_REGISTRO_ANESTESICO", c.getCodigo());
        return construirDetalle(actor, c, true, true);
    }

    @Transactional
    public CirugiaDetalle registrarInsumo(UsuarioPrincipal actor, UUID id, InsumoCirugiaRequest request) {
        Cirugia c = cargar(id);
        exigirEquipo(actor, c);
        exigirEstadoOperativo(c);
        Producto producto = productos
                .findById(request.productoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto", "id", request.productoId()));
        if (!producto.isActivo()) {
            throw new ConflictException("El producto no está activo");
        }
        Usuario responsable = usuario(actor.getId());
        inventario.descontar(
                producto,
                request.cantidad(),
                MotivoMovimiento.CONSUMO_QUIRURGICO,
                c.getCodigo(),
                responsable,
                "Insumo de la cirugía " + c.getCodigo());
        insumos.save(CirugiaInsumo.builder()
                .cirugia(c)
                .producto(producto)
                .cantidad(request.cantidad())
                .referencia(c.getCodigo())
                .registradoPor(responsable)
                .build());
        registrar(actor, c, "INSUMO", producto.getNombre() + " x" + request.cantidad());
        return construirDetalle(actor, c, true, true);
    }

    @Transactional
    public CirugiaDetalle usarSet(UsuarioPrincipal actor, UUID id, UsoSetRequest request) {
        Cirugia c = cargar(id);
        exigirEquipo(actor, c);
        exigirEstadoOperativo(c);
        LoteEsteril lote = lotes.findParaUsar(request.loteId())
                .orElseThrow(() -> new ResourceNotFoundException("Lote esterilizado", "id", request.loteId()));
        EstadoEfectivoLote estado = EsterilizacionService.efectivo(lote, LocalDateTime.now());
        if (estado != EstadoEfectivoLote.ESTERIL) {
            throw new ConflictException(
                    "El lote " + lote.getCodigo() + " no se puede usar: " + descripcionEstado(estado));
        }
        lote.setEstado(EstadoLoteEsteril.EN_USO);
        usosDeSets.save(CirugiaSet.builder()
                .cirugia(c)
                .lote(lote)
                .registradoPor(usuario(actor.getId()))
                .build());
        registrar(actor, c, "SET_USADO", lote.getCodigo());
        return construirDetalle(actor, c, true, true);
    }

    public List<EventoCirugiaResponse> historial(UsuarioPrincipal actor, UUID id) {
        Cirugia c = cargar(id);
        exigirPersonal(actor);
        boolean enEquipo = equipos.existsByCirugiaIdAndUsuarioId(id, actor.getId());
        if (esMedico(actor) && !puedeVerDetalle(actor, c, enEquipo)) {
            throw new ForbiddenOperationException("No tiene relación con esta cirugía ni con el paciente");
        }
        return eventos.deCirugia(id).stream()
                .map(e -> new EventoCirugiaResponse(
                        e.getFecha(), e.getUsuario().getNombreCompleto(), e.getAccion(), e.getDetalle()))
                .toList();
    }

    public List<String> pendientesParaIniciar(Cirugia c) {
        List<String> pendientes = new ArrayList<>();
        if (c.getEstado() != EstadoCirugia.EN_PREPARACION) {
            pendientes.add("la cirugía debe estar en preparación");
        }
        if (!firmado(c.getConsentimientoQuirurgico())) {
            pendientes.add("falta el consentimiento quirúrgico firmado");
        }
        if (c.getTipoAnestesia().requiereConsentimientoAnestesico() && !firmado(c.getConsentimientoAnestesico())) {
            pendientes.add("falta el consentimiento anestésico firmado");
        }
        if (!checklists.existsByCirugiaIdAndMomento(c.getId(), MomentoChecklist.ANTES_INDUCCION)) {
            pendientes.add("falta la lista de verificación antes de la inducción");
        }
        return pendientes;
    }

    public List<String> pendientesParaFinalizar(Cirugia c) {
        List<String> pendientes = new ArrayList<>();
        if (c.getEstado() != EstadoCirugia.EN_CURSO) {
            pendientes.add("la cirugía debe estar en curso");
        }
        if (!checklists.existsByCirugiaIdAndMomento(c.getId(), MomentoChecklist.ANTES_INCISION)) {
            pendientes.add("falta la lista de verificación antes de la incisión");
        }
        if (!checklists.existsByCirugiaIdAndMomento(c.getId(), MomentoChecklist.ANTES_SALIDA)) {
            pendientes.add("falta la lista de verificación antes de salir del quirófano");
        }
        if (c.getNotaRegistradaEn() == null) {
            pendientes.add("falta la nota operatoria");
        }
        if (EnumSet.of(TipoAnestesia.GENERAL, TipoAnestesia.REGIONAL, TipoAnestesia.SEDACION)
                        .contains(c.getTipoAnestesia())
                && c.getAnestesiaRegistradaEn() == null) {
            pendientes.add("falta el registro anestésico");
        }
        return pendientes;
    }

    private List<Usuario> validarEquipo(
            Cirugia c, List<MiembroEquipoRequest> equipo, LocalDateTime inicio, LocalDateTime fin) {
        long cirujanos =
                equipo.stream().filter(m -> m.rol() == RolQuirurgico.CIRUJANO).count();
        if (cirujanos != 1) {
            throw new DatosInvalidosException("El equipo debe tener exactamente un cirujano");
        }
        boolean anestesiologo = equipo.stream().anyMatch(m -> m.rol() == RolQuirurgico.ANESTESIOLOGO);
        if (c.getTipoAnestesia().requiereConsentimientoAnestesico()
                && c.getTipoAnestesia() != TipoAnestesia.LOCAL
                && !anestesiologo) {
            throw new DatosInvalidosException("El equipo debe incluir un anestesiólogo");
        }
        Set<String> vistos = new HashSet<>();
        List<Usuario> miembros = new ArrayList<>();
        for (MiembroEquipoRequest m : equipo) {
            if (!vistos.add(m.usuarioId() + ":" + m.rol())) {
                throw new DatosInvalidosException("Hay un miembro repetido con el mismo rol");
            }
            Usuario u = usuarios.findByIdConRoles(m.usuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", m.usuarioId()));
            RolNombre requerido = m.rol().esMedico() ? RolNombre.ROLE_MEDICO : RolNombre.ROLE_ENFERMERIA;
            boolean tieneRol = u.getRoles().stream().anyMatch(r -> r.getNombre() == requerido);
            if (!tieneRol) {
                throw new DatosInvalidosException(u.getNombreCompleto() + " no puede actuar como "
                        + m.rol().name().toLowerCase());
            }
            if (!cirugias.solapadasDeUsuario(u.getId(), inicio, fin, c.getId()).isEmpty()) {
                throw new ConflictException(u.getNombreCompleto() + " ya está asignado a otra cirugía en ese horario");
            }
            miembros.add(u);
        }
        return miembros;
    }

    private Consentimiento solicitarConsentimiento(
            UsuarioPrincipal actor, Cirugia c, String codigoPlantilla, String prefijo) {
        UUID plantillaId = plantillas
                .findByCodigo(codigoPlantilla)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Plantilla de consentimiento", "codigo", codigoPlantilla))
                .getId();
        String procedimiento = prefijo + ": " + c.getProcedimiento();
        if (procedimiento.length() > 200) {
            procedimiento = procedimiento.substring(0, 200);
        }
        UUID consentimientoId = consentimientoService
                .solicitar(
                        actor,
                        new ConsentimientoSolicitudRequest(
                                plantillaId,
                                c.getPaciente().getId(),
                                null,
                                c.getIngreso() == null ? null : c.getIngreso().getId(),
                                null,
                                procedimiento))
                .id();
        return consentimientos
                .findById(consentimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Consentimiento", "id", consentimientoId));
    }

    private void liberarSets(Cirugia c) {
        for (CirugiaSet uso : usosDeSets.deCirugia(c.getId())) {
            if (uso.getLote().getEstado() == EstadoLoteEsteril.EN_USO) {
                uso.getLote().setEstado(EstadoLoteEsteril.USADO);
            }
        }
    }

    private List<QuirofanoEquipo> reemplazarEquipos(Quirofano q, List<EquipoQuirofanoRequest> pedidos) {
        if (pedidos == null) {
            return equiposQuirofano.deQuirofanos(List.of(q.getId()));
        }
        equiposQuirofano.deleteByQuirofanoId(q.getId());
        equiposQuirofano.flush();
        return pedidos.stream()
                .map(e -> equiposQuirofano.save(QuirofanoEquipo.builder()
                        .quirofano(q)
                        .nombre(e.nombre().strip())
                        .serie(e.serie())
                        .build()))
                .toList();
    }

    private CirugiaDetalle construirDetalle(UsuarioPrincipal actor, Cirugia c, boolean enEquipo, boolean clinico) {
        UUID id = c.getId();
        List<CirugiaEquipo> miembros = equipos.deCirugia(id);
        boolean miembro = enEquipo
                || miembros.stream().anyMatch(m -> m.getUsuario().getId().equals(actor.getId()));
        List<ChecklistResponse> listas = checklists.deCirugia(id).stream()
                .map(k -> new ChecklistResponse(
                        k.getMomento(),
                        Arrays.asList(k.getItems().split(",")),
                        k.getObservaciones(),
                        k.getCompletadoPor().getNombreCompleto(),
                        k.getCompletadoEn()))
                .toList();
        List<InsumoCirugiaResponse> insumosResp = clinico
                ? insumos.deCirugia(id).stream()
                        .map(i -> new InsumoCirugiaResponse(
                                i.getId(),
                                i.getProducto().getId(),
                                i.getProducto().getNombre(),
                                i.getProducto().getUnidad(),
                                i.getCantidad(),
                                i.getRegistradoPor().getNombreCompleto(),
                                i.getRegistradoEn()))
                        .toList()
                : List.of();
        List<SetUsadoResponse> setsResp = usosDeSets.deCirugia(id).stream()
                .map(s -> new SetUsadoResponse(
                        s.getId(),
                        s.getLote().getId(),
                        s.getLote().getCodigo(),
                        s.getLote().getSetInstrumental().getNombre(),
                        s.getLote().getCiclo().getNumero(),
                        s.getLote().getEsterilizadoEn(),
                        s.getLote().getVenceEn(),
                        s.getRegistradoPor().getNombreCompleto(),
                        s.getRegistradoEn()))
                .toList();
        NotaOperatoriaResponse nota = clinico && c.getNotaRegistradaEn() != null
                ? new NotaOperatoriaResponse(
                        c.getHallazgos(),
                        c.getTecnica(),
                        c.getComplicaciones(),
                        c.getSangradoMl(),
                        c.getDiagnosticoPostoperatorio(),
                        c.getIndicacionesPostoperatorias(),
                        c.getNotaRegistradaEn(),
                        c.getNotaPor() == null ? null : c.getNotaPor().getNombreCompleto())
                : null;
        RegistroAnestesicoResponse anestesia = clinico && c.getAnestesiaRegistradaEn() != null
                ? new RegistroAnestesicoResponse(
                        c.getAsa() == null ? null : c.getAsa().intValue(),
                        c.getAnestesiaInicio(),
                        c.getAnestesiaFin(),
                        c.getAnestesiaObservaciones(),
                        c.getAnestesiaRegistradaEn(),
                        c.getAnestesiaPor() == null ? null : c.getAnestesiaPor().getNombreCompleto())
                : null;
        List<EventoCirugiaResponse> historial = eventos.deCirugia(id).stream()
                .map(e -> new EventoCirugiaResponse(
                        e.getFecha(), e.getUsuario().getNombreCompleto(), e.getAccion(), e.getDetalle()))
                .toList();
        return new CirugiaDetalle(
                resumen(actor, c, miembro),
                clinico ? c.getDiagnosticoCodigo() : null,
                clinico ? c.getDiagnosticoDescripcion() : null,
                c.getTipoAnestesia(),
                c.getIngreso() == null ? null : c.getIngreso().getId(),
                c.getIngreso() == null ? null : c.getIngreso().getCodigo(),
                clinico ? c.getIndicacionesPreoperatorias() : null,
                c.getMotivo(),
                c.getInicioReal(),
                c.getFinReal(),
                consentimiento(c.getConsentimientoQuirurgico()),
                consentimiento(c.getConsentimientoAnestesico()),
                miembros.stream()
                        .map(m -> new MiembroEquipoResponse(
                                m.getUsuario().getId(), m.getUsuario().getNombreCompleto(), m.getRol()))
                        .toList(),
                listas,
                insumosResp,
                setsResp,
                nota,
                anestesia,
                historial,
                c.getEstado().abierta() ? pendientesParaIniciar(c) : List.of(),
                c.getEstado().abierta() ? pendientesParaFinalizar(c) : List.of(),
                clinico);
    }

    private CirugiaResumen resumen(UsuarioPrincipal actor, Cirugia c, boolean enEquipo) {
        boolean visible = !esMedico(actor) || puedeVerDetalle(actor, c, enEquipo);
        return new CirugiaResumen(
                c.getId(),
                c.getCodigo(),
                c.getEstado(),
                c.getTipo(),
                c.getQuirofano() == null ? null : c.getQuirofano().getId(),
                c.getQuirofano() == null ? null : c.getQuirofano().getNombre(),
                c.getInicioProgramado(),
                c.getFinProgramado(),
                visible,
                visible ? c.getPaciente().getId() : null,
                visible ? c.getPaciente().getUsuario().getNombreCompleto() : null,
                visible ? c.getProcedimiento() : null,
                visible ? c.getSolicitante().getUsuario().getNombreCompleto() : null,
                enEquipo);
    }

    private static ConsentimientoCirugia consentimiento(Consentimiento c) {
        return c == null ? null : new ConsentimientoCirugia(c.getId(), c.estadoEfectivo(), c.getFirmadoEn());
    }

    private static boolean firmado(Consentimiento c) {
        return c != null && c.estadoEfectivo() == EstadoConsentimiento.FIRMADO;
    }

    private static boolean requiereNuevo(Consentimiento c) {
        return c == null
                || c.estadoEfectivo() == EstadoConsentimiento.REVOCADO
                || c.estadoEfectivo() == EstadoConsentimiento.VENCIDO;
    }

    private static String descripcionEstado(EstadoEfectivoLote estado) {
        return switch (estado) {
            case BLOQUEADO -> "proviene de un ciclo no conforme";
            case VENCIDO -> "su esterilidad venció";
            case EN_ESPERA_DE_INDICADOR -> "el ciclo aún no tiene indicadores conformes";
            case EN_USO -> "ya está en uso";
            case USADO -> "ya fue usado";
            default -> "no está disponible";
        };
    }

    private void exigirEstadoOperativo(Cirugia c) {
        if (c.getEstado() != EstadoCirugia.EN_PREPARACION && c.getEstado() != EstadoCirugia.EN_CURSO) {
            throw new ConflictException("Solo se registra durante la preparación o con la cirugía en curso");
        }
    }

    private void exigirPersonal(UsuarioPrincipal actor) {
        if (!esMedico(actor) && !esEnfermeria(actor)) {
            throw new ForbiddenOperationException("No tiene permiso para consultar la actividad quirúrgica");
        }
    }

    private void exigirOperador(UsuarioPrincipal actor, Cirugia c) {
        if (esEnfermeria(actor)) {
            return;
        }
        if (esMedico(actor)
                && (esSolicitante(actor, c) || equipos.existeRol(c.getId(), actor.getId(), RolQuirurgico.CIRUJANO))) {
            return;
        }
        throw new ForbiddenOperationException("No tiene permiso para gestionar esta cirugía");
    }

    private void exigirMedicoResponsable(UsuarioPrincipal actor, Cirugia c) {
        if (esMedico(actor)
                && (esSolicitante(actor, c) || equipos.existeRol(c.getId(), actor.getId(), RolQuirurgico.CIRUJANO))) {
            return;
        }
        throw new ForbiddenOperationException("Solo el médico solicitante o el cirujano gestionan los consentimientos");
    }

    private void exigirEquipo(UsuarioPrincipal actor, Cirugia c) {
        if (!equipos.existsByCirugiaIdAndUsuarioId(c.getId(), actor.getId())) {
            throw new ForbiddenOperationException("Solo el equipo quirúrgico registra en esta cirugía");
        }
    }

    private void exigirRolEnEquipo(UsuarioPrincipal actor, Cirugia c, RolQuirurgico rol, String mensaje) {
        if (!equipos.existeRol(c.getId(), actor.getId(), rol)) {
            throw new ForbiddenOperationException(mensaje);
        }
    }

    private boolean esSolicitante(UsuarioPrincipal actor, Cirugia c) {
        return c.getSolicitante().getUsuario().getId().equals(actor.getId());
    }

    private boolean puedeVerDetalle(UsuarioPrincipal actor, Cirugia c, boolean enEquipo) {
        return enEquipo
                || esSolicitante(actor, c)
                || relacion.tieneRelacion(actor, c.getPaciente().getId());
    }

    private boolean clinicoVisible(UsuarioPrincipal actor, Cirugia c, boolean enEquipo) {
        if (esMedico(actor)) {
            return puedeVerDetalle(actor, c, enEquipo);
        }
        return enEquipo;
    }

    private boolean esMedico(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_MEDICO.name());
    }

    private boolean esEnfermeria(UsuarioPrincipal actor) {
        return actor.getRoles().contains(RolNombre.ROLE_ENFERMERIA.name());
    }

    private Cirugia cargar(UUID id) {
        return cirugias.findConDetalle(id).orElseThrow(() -> new ResourceNotFoundException("Cirugía", "id", id));
    }

    private Medico medicoDe(UsuarioPrincipal actor) {
        return medicoRepository
                .findByUsuarioEmail(actor.getEmail())
                .orElseThrow(() -> new ForbiddenOperationException("Solo el personal médico solicita cirugías"));
    }

    private Usuario usuario(UUID id) {
        return usuarios.findById(id).orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", id));
    }

    private void registrar(UsuarioPrincipal actor, Cirugia c, String accion, String detalle) {
        eventos.save(CirugiaEvento.builder()
                .cirugia(c)
                .usuario(usuario(actor.getId()))
                .accion(accion.length() > 30 ? accion.substring(0, 30) : accion)
                .detalle(detalle == null ? null : (detalle.length() > 300 ? detalle.substring(0, 300) : detalle))
                .build());
    }

    private static QuirofanoResponse quirofano(Quirofano q, Collection<QuirofanoEquipo> equiposDelQuirofano) {
        return new QuirofanoResponse(
                q.getId(),
                q.getCodigo(),
                q.getNombre(),
                q.getUbicacion(),
                q.getEstado(),
                q.isActivo(),
                q.getObservaciones(),
                equiposDelQuirofano.stream()
                        .map(e -> new EquipoQuirofanoResponse(e.getId(), e.getNombre(), e.getSerie()))
                        .toList());
    }
}
