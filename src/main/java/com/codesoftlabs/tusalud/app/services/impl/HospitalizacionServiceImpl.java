package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.AltaHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaAsignacionRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaEstadoRequest;
import com.codesoftlabs.tusalud.app.dto.request.CamaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EmergenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.EvolucionHospRequest;
import com.codesoftlabs.tusalud.app.dto.request.IngresoHospCreateRequest;
import com.codesoftlabs.tusalud.app.dto.request.TriajeRequest;
import com.codesoftlabs.tusalud.app.dto.response.CamaResponse;
import com.codesoftlabs.tusalud.app.dto.response.EvolucionHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.HospResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.IngresoHospResponse;
import com.codesoftlabs.tusalud.app.dto.response.MapaCamasResponse;
import com.codesoftlabs.tusalud.app.entities.Cama;
import com.codesoftlabs.tusalud.app.entities.EvolucionHospitalaria;
import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.AreaCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCama;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEvolucion;
import com.codesoftlabs.tusalud.app.entities.enums.TipoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.CamaRepository;
import com.codesoftlabs.tusalud.app.repositories.EvolucionHospitalariaRepository;
import com.codesoftlabs.tusalud.app.repositories.IngresoHospitalarioRepository;
import com.codesoftlabs.tusalud.app.repositories.MedicoRepository;
import com.codesoftlabs.tusalud.app.repositories.PacienteRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.HospitalizacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RelacionAsistencialService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class HospitalizacionServiceImpl implements HospitalizacionService {

    private static final int LIMITE_INGRESOS = 200;
    private static final int LIMITE_NOTIFICACION_EMERGENCIA = 20;
    private static final int NIVEL_CRITICO = 2;
    private static final Set<AreaCama> AREAS_EMERGENCIA =
            EnumSet.of(AreaCama.EMERGENCIA, AreaCama.UCI, AreaCama.OBSERVACION);

    private final CamaRepository camaRepository;
    private final IngresoHospitalarioRepository ingresoRepository;
    private final EvolucionHospitalariaRepository evolucionRepository;
    private final PacienteRepository pacienteRepository;
    private final MedicoRepository medicoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final RelacionAsistencialService relacionAsistencial;

    @Override
    @Transactional(readOnly = true)
    public List<CamaResponse> listarCamas(AreaCama area, EstadoCama estado) {
        Map<UUID, IngresoHospitalario> ocupantes = ocupantesPorCama();
        return camaRepository.findAllByOrderByAreaAscCodigoAsc().stream()
                .filter(cama -> area == null || cama.getArea() == area)
                .filter(cama -> estado == null || cama.getEstado() == estado)
                .map(cama -> respuesta(cama, ocupantes))
                .toList();
    }

    @Override
    public CamaResponse crearCama(CamaRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        if (camaRepository.existsByCodigoIgnoreCase(codigo)) {
            throw new ConflictException("Ya existe una cama con el código " + codigo);
        }
        Cama cama = Cama.builder().estado(EstadoCama.LIBRE).build();
        aplicar(cama, request, codigo);
        return respuesta(camaRepository.save(cama), Map.of());
    }

    @Override
    public CamaResponse actualizarCama(UUID id, CamaRequest request) {
        Cama cama = buscarCama(id);
        String codigo = normalizarCodigo(request.codigo());
        camaRepository
                .findByCodigoIgnoreCase(codigo)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ConflictException("Ya existe una cama con el código " + codigo);
                });
        boolean desactiva = request.activa() != null && !request.activa();
        if (desactiva && cama.getEstado() == EstadoCama.OCUPADA) {
            throw new ConflictException("No se puede desactivar una cama ocupada");
        }
        aplicar(cama, request, codigo);
        return respuesta(camaRepository.save(cama), ocupantesPorCama());
    }

    @Override
    public CamaResponse cambiarEstadoCama(UUID id, CamaEstadoRequest request) {
        Cama cama = buscarCama(id);
        EstadoCama nuevo = request.estado();
        if (cama.getEstado() == EstadoCama.OCUPADA && nuevo != EstadoCama.OCUPADA) {
            throw new ConflictException("La cama está ocupada: se libera dando el alta o trasladando al paciente");
        }
        if (nuevo == EstadoCama.OCUPADA && cama.getEstado() != EstadoCama.OCUPADA) {
            throw new ConflictException("Una cama se ocupa al registrar un ingreso o asignarla a una emergencia");
        }
        cama.setEstado(nuevo);
        return respuesta(camaRepository.save(cama), ocupantesPorCama());
    }

    @Override
    public IngresoHospResponse crearIngreso(UsuarioPrincipal actor, IngresoHospCreateRequest request) {
        relacionAsistencial.exigirRelacion(
                actor,
                request.pacienteId(),
                "Solo puede ingresar a pacientes que atiende o que otro médico compartió con usted");
        Paciente paciente = buscarPaciente(request.pacienteId());
        Cama cama = buscarCamaLibre(request.camaId());
        validarSinIngresoActivo(paciente);
        Medico medico = resolverMedico(actor, request.medicoId());
        LocalDateTime ahora = LocalDateTime.now();

        IngresoHospitalario nuevo = IngresoHospitalario.builder()
                .codigo("PEND-" + System.nanoTime())
                .tipo(TipoIngresoHosp.PROGRAMADO)
                .estado(EstadoIngresoHosp.ACTIVO)
                .paciente(paciente)
                .cama(cama)
                .medico(medico)
                .motivo(request.motivo().trim())
                .diagnosticoIngreso(vacioANulo(request.diagnosticoIngreso()))
                .fechaIngreso(ahora)
                .desdeEnCama(ahora)
                .build();
        IngresoHospitalario ingreso = guardarConCodigo(nuevo);
        cama.setEstado(EstadoCama.OCUPADA);
        camaRepository.save(cama);

        notificacionService.crear(
                paciente.getUsuario(),
                TipoNotificacion.INGRESO_HOSPITALARIO,
                "Ingreso hospitalario registrado",
                "Fuiste ingresado a la cama " + cama.getCodigo() + " con el código " + ingreso.getCodigo() + ".",
                "/mis-ingresos");
        return IngresoHospResponse.from(ingreso, ahora);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngresoHospResponse> listarIngresos(UsuarioPrincipal actor, EstadoIngresoHosp estado, String busqueda) {
        LocalDateTime ahora = LocalDateTime.now();
        String termino = busqueda == null ? "" : busqueda.trim().toLowerCase(Locale.ROOT);
        return ingresoRepository.findRecientes(PageRequest.of(0, 1000)).stream()
                .filter(ingreso -> estado == null || ingreso.getEstado() == estado)
                .filter(ingreso -> termino.isEmpty() || coincide(ingreso, termino))
                .filter(ingreso -> puedeVer(actor, ingreso))
                .limit(LIMITE_INGRESOS)
                .map(ingreso -> paraActor(IngresoHospResponse.from(ingreso, ahora), actor))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public IngresoHospResponse obtenerIngreso(UsuarioPrincipal actor, UUID id) {
        IngresoHospitalario ingreso = buscarIngreso(id);
        if (!puedeVer(actor, ingreso)) {
            throw new ForbiddenOperationException("No tiene acceso a este ingreso");
        }
        return paraActor(IngresoHospResponse.from(ingreso, LocalDateTime.now()), actor);
    }

    @Override
    public IngresoHospResponse trasladar(UUID id, CamaAsignacionRequest request) {
        IngresoHospitalario ingreso = buscarIngresoActivo(id);
        Cama actual = ingreso.getCama();
        if (actual == null) {
            throw new ConflictException("El paciente aún no tiene cama: use la asignación de cama");
        }
        Cama destino = buscarCamaLibre(request.camaId());
        LocalDateTime ahora = LocalDateTime.now();
        acumularCargo(ingreso, actual, ahora);
        actual.setEstado(EstadoCama.LIMPIEZA);
        destino.setEstado(EstadoCama.OCUPADA);
        ingreso.setCama(destino);
        ingreso.setDesdeEnCama(ahora);
        camaRepository.save(actual);
        camaRepository.save(destino);
        return IngresoHospResponse.from(ingresoRepository.save(ingreso), ahora);
    }

    @Override
    public IngresoHospResponse asignarCama(UUID id, CamaAsignacionRequest request) {
        IngresoHospitalario ingreso = buscarIngresoActivo(id);
        if (ingreso.getTipo() != TipoIngresoHosp.EMERGENCIA || ingreso.getCama() != null) {
            throw new ConflictException("Solo una emergencia sin cama puede recibir una asignación");
        }
        Cama destino = buscarCamaLibre(request.camaId());
        if (!AREAS_EMERGENCIA.contains(destino.getArea())) {
            throw new ConflictException("Una emergencia solo se asigna a camas de emergencia, UCI u observación");
        }
        LocalDateTime ahora = LocalDateTime.now();
        destino.setEstado(EstadoCama.OCUPADA);
        ingreso.setCama(destino);
        ingreso.setDesdeEnCama(ahora);
        camaRepository.save(destino);
        return IngresoHospResponse.from(ingresoRepository.save(ingreso), ahora);
    }

    @Override
    public IngresoHospResponse darAlta(UsuarioPrincipal actor, UUID id, AltaHospRequest request) {
        IngresoHospitalario ingreso = buscarIngresoActivo(id);
        exigirMedicoDelIngreso(actor, ingreso, "Solo el médico del ingreso puede dar el alta");
        LocalDateTime ahora = LocalDateTime.now();
        Cama cama = ingreso.getCama();
        if (cama != null) {
            acumularCargo(ingreso, cama, ahora);
            cama.setEstado(EstadoCama.LIMPIEZA);
            camaRepository.save(cama);
        }
        ingreso.setEstado(EstadoIngresoHosp.ALTA);
        ingreso.setFechaAlta(ahora);
        ingreso.setDiagnosticoAlta(request.diagnosticoAlta().trim());
        ingreso.setIndicacionesAlta(request.indicacionesAlta().trim());
        ingreso.setDesdeEnCama(null);
        IngresoHospitalario guardado = ingresoRepository.save(ingreso);

        notificacionService.crear(
                guardado.getPaciente().getUsuario(),
                TipoNotificacion.ALTA_HOSPITALARIA,
                "Alta hospitalaria",
                "Recibiste el alta del ingreso " + guardado.getCodigo() + ". Revisa tus indicaciones.",
                "/mis-ingresos");
        return IngresoHospResponse.from(guardado, ahora);
    }

    @Override
    public IngresoHospResponse cancelarIngreso(UUID id) {
        IngresoHospitalario ingreso = buscarIngresoActivo(id);
        if (evolucionRepository.countByIngresoId(id) > 0) {
            throw new ConflictException("El ingreso ya tiene evoluciones registradas y no se puede cancelar");
        }
        Cama cama = ingreso.getCama();
        if (cama != null) {
            cama.setEstado(EstadoCama.LIBRE);
            camaRepository.save(cama);
        }
        ingreso.setEstado(EstadoIngresoHosp.CANCELADO);
        ingreso.setDesdeEnCama(null);
        return IngresoHospResponse.from(ingresoRepository.save(ingreso), LocalDateTime.now());
    }

    @Override
    public EvolucionHospResponse registrarEvolucion(
            UsuarioPrincipal actor, UUID ingresoId, EvolucionHospRequest request) {
        IngresoHospitalario ingreso = buscarIngresoActivo(ingresoId);
        RolNombre requerido =
                request.tipo() == TipoEvolucion.MEDICA ? RolNombre.ROLE_MEDICO : RolNombre.ROLE_ENFERMERIA;
        if (!tieneRol(actor, requerido)) {
            throw new ForbiddenOperationException(
                    request.tipo() == TipoEvolucion.MEDICA
                            ? "Solo un médico registra evoluciones médicas"
                            : "Solo el personal de enfermería registra evoluciones de enfermería");
        }
        if (request.tipo() == TipoEvolucion.MEDICA) {
            exigirMedicoDelIngreso(actor, ingreso, "Solo el médico del ingreso registra evoluciones médicas");
        }
        Usuario autor = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        EvolucionHospitalaria evolucion = evolucionRepository.save(EvolucionHospitalaria.builder()
                .ingreso(ingreso)
                .tipo(request.tipo())
                .nota(request.nota().trim())
                .presionArterial(vacioANulo(request.presionArterial()))
                .frecuenciaCardiaca(request.frecuenciaCardiaca())
                .temperatura(request.temperatura())
                .saturacionO2(request.saturacionO2())
                .autor(autor)
                .fecha(LocalDateTime.now())
                .build());
        return EvolucionHospResponse.from(evolucion, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EvolucionHospResponse> listarEvoluciones(UsuarioPrincipal actor, UUID ingresoId) {
        IngresoHospitalario ingreso = buscarIngreso(ingresoId);
        boolean personal = tieneRol(actor, RolNombre.ROLE_MEDICO) || tieneRol(actor, RolNombre.ROLE_ENFERMERIA);
        if (!puedeVerClinico(actor, ingreso)) {
            throw new ForbiddenOperationException("No tiene acceso a las evoluciones de este ingreso");
        }
        return evolucionRepository.findDelIngreso(ingresoId).stream()
                .filter(evolucion -> personal || evolucion.getTipo() == TipoEvolucion.MEDICA)
                .map(evolucion -> EvolucionHospResponse.from(evolucion, !personal))
                .toList();
    }

    @Override
    public IngresoHospResponse registrarEmergencia(UsuarioPrincipal actor, EmergenciaRequest request) {
        Paciente paciente = buscarPaciente(request.pacienteId());
        validarSinIngresoActivo(paciente);
        TriajeRequest triaje = request.triaje();
        Usuario registrador = usuarioRepository
                .findById(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", actor.getId()));
        LocalDateTime ahora = LocalDateTime.now();

        IngresoHospitalario nuevo = IngresoHospitalario.builder()
                .codigo("PEND-" + System.nanoTime())
                .tipo(TipoIngresoHosp.EMERGENCIA)
                .estado(EstadoIngresoHosp.ACTIVO)
                .paciente(paciente)
                .motivo(triaje.motivoConsulta().trim())
                .fechaIngreso(ahora)
                .triajeNivel(triaje.nivel())
                .triajePresionArterial(vacioANulo(triaje.presionArterial()))
                .triajeFrecuenciaCardiaca(triaje.frecuenciaCardiaca())
                .triajeFrecuenciaRespiratoria(triaje.frecuenciaRespiratoria())
                .triajeTemperatura(triaje.temperatura())
                .triajeSaturacionO2(triaje.saturacionO2())
                .triajeGlasgow(triaje.glasgow())
                .triajeMotivoConsulta(triaje.motivoConsulta().trim())
                .triajeRegistradoPor(registrador)
                .triajeFecha(ahora)
                .build();
        IngresoHospitalario ingreso = guardarConCodigo(nuevo);

        if (triaje.nivel() <= NIVEL_CRITICO) {
            usuarioRepository.findActivosPorRol(RolNombre.ROLE_MEDICO).stream()
                    .limit(LIMITE_NOTIFICACION_EMERGENCIA)
                    .forEach(destinatario -> notificacionService.crear(
                            destinatario,
                            TipoNotificacion.EMERGENCIA_TRIAJE,
                            "Emergencia de triaje " + triaje.nivel(),
                            paciente.getUsuario().getNombreCompleto() + " llegó a emergencia: " + ingreso.getMotivo()
                                    + ".",
                            "/hospitalizacion/emergencias"));
        }
        return IngresoHospResponse.from(ingreso, ahora);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngresoHospResponse> listarEmergencias(UsuarioPrincipal actor) {
        LocalDateTime ahora = LocalDateTime.now();
        return ingresoRepository.findEmergenciasActivas().stream()
                .sorted(Comparator.comparing(
                                IngresoHospitalario::getTriajeNivel, Comparator.nullsLast(Integer::compare))
                        .thenComparing(IngresoHospitalario::getFechaIngreso))
                .map(ingreso -> paraActor(IngresoHospResponse.from(ingreso, ahora), actor))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MapaCamasResponse mapa() {
        Map<UUID, IngresoHospitalario> ocupantes = ocupantesPorCama();
        Map<AreaCama, List<CamaResponse>> porArea = new EnumMap<>(AreaCama.class);
        for (AreaCama area : AreaCama.values()) {
            porArea.put(area, new java.util.ArrayList<>());
        }
        camaRepository.findAllByOrderByAreaAscCodigoAsc().stream()
                .filter(Cama::isActiva)
                .forEach(cama -> porArea.get(cama.getArea()).add(respuesta(cama, ocupantes)));
        List<MapaCamasResponse.AreaMapa> areas = porArea.entrySet().stream()
                .map(entrada -> new MapaCamasResponse.AreaMapa(
                        entrada.getKey(),
                        entrada.getValue().size(),
                        (int) entrada.getValue().stream()
                                .filter(c -> c.estado() == EstadoCama.LIBRE)
                                .count(),
                        (int) entrada.getValue().stream()
                                .filter(c -> c.estado() == EstadoCama.OCUPADA)
                                .count(),
                        entrada.getValue()))
                .toList();
        return new MapaCamasResponse(areas);
    }

    @Override
    @Transactional(readOnly = true)
    public HospResumenResponse resumen() {
        long activas = camaRepository.countByActivaTrue();
        long ocupadas = camaRepository.countByActivaTrueAndEstado(EstadoCama.OCUPADA);
        double porcentaje = activas == 0
                ? 0
                : BigDecimal.valueOf(ocupadas * 100.0 / activas)
                        .setScale(1, RoundingMode.HALF_UP)
                        .doubleValue();
        return new HospResumenResponse(
                camaRepository.countByActivaTrueAndEstado(EstadoCama.LIBRE),
                ocupadas,
                camaRepository.countByActivaTrueAndEstado(EstadoCama.LIMPIEZA),
                ingresoRepository.countByEstado(EstadoIngresoHosp.ACTIVO),
                ingresoRepository.countByEstadoAndTipoAndCamaIsNull(
                        EstadoIngresoHosp.ACTIVO, TipoIngresoHosp.EMERGENCIA),
                porcentaje);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngresoHospResponse> misIngresos(UsuarioPrincipal actor) {
        LocalDateTime ahora = LocalDateTime.now();
        return ingresoRepository.findDelUsuarioPaciente(actor.getId()).stream()
                .map(ingreso -> IngresoHospResponse.from(ingreso, ahora))
                .toList();
    }

    private IngresoHospitalario guardarConCodigo(IngresoHospitalario nuevo) {
        IngresoHospitalario guardado = ingresoRepository.saveAndFlush(nuevo);
        guardado.setCodigo(String.format("HOS-%06d", ingresoRepository.count()));
        return guardado;
    }

    private void acumularCargo(IngresoHospitalario ingreso, Cama cama, LocalDateTime ahora) {
        if (ingreso.getDesdeEnCama() == null) {
            return;
        }
        long dias = IngresoHospResponse.dias(ingreso.getDesdeEnCama(), ahora);
        ingreso.setCargoAcumulado(
                ingreso.getCargoAcumulado().add(cama.getPrecioDia().multiply(BigDecimal.valueOf(dias))));
    }

    private void aplicar(Cama cama, CamaRequest request, String codigo) {
        cama.setCodigo(codigo);
        cama.setArea(request.area());
        cama.setPiso(request.piso());
        cama.setDescripcion(vacioANulo(request.descripcion()));
        cama.setPrecioDia(request.precioDia());
        cama.setActiva(request.activa() == null || request.activa());
    }

    private Map<UUID, IngresoHospitalario> ocupantesPorCama() {
        return ingresoRepository.findActivosConCama().stream()
                .collect(Collectors.toMap(
                        ingreso -> ingreso.getCama().getId(), Function.identity(), (a, b) -> a, HashMap::new));
    }

    private static CamaResponse respuesta(Cama cama, Map<UUID, IngresoHospitalario> ocupantes) {
        IngresoHospitalario ocupante = ocupantes.get(cama.getId());
        return CamaResponse.from(
                cama,
                ocupante == null ? null : ocupante.getId(),
                ocupante == null ? null : ocupante.getPaciente().getUsuario().getNombreCompleto());
    }

    private Cama buscarCama(UUID id) {
        return camaRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cama", "id", id));
    }

    private Cama buscarCamaLibre(UUID id) {
        Cama cama = buscarCama(id);
        if (!cama.isActiva()) {
            throw new ConflictException("La cama " + cama.getCodigo() + " está desactivada");
        }
        if (cama.getEstado() != EstadoCama.LIBRE) {
            throw new ConflictException("La cama " + cama.getCodigo() + " no está libre");
        }
        return cama;
    }

    private Paciente buscarPaciente(UUID id) {
        return pacienteRepository
                .findByIdWithUsuario(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", "id", id));
    }

    private IngresoHospitalario buscarIngreso(UUID id) {
        return ingresoRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ingreso hospitalario", "id", id));
    }

    private IngresoHospitalario buscarIngresoActivo(UUID id) {
        IngresoHospitalario ingreso = buscarIngreso(id);
        if (ingreso.getEstado() != EstadoIngresoHosp.ACTIVO) {
            throw new ConflictException("El ingreso " + ingreso.getCodigo() + " ya no está activo");
        }
        return ingreso;
    }

    private void validarSinIngresoActivo(Paciente paciente) {
        if (ingresoRepository.existsByPacienteIdAndEstado(paciente.getId(), EstadoIngresoHosp.ACTIVO)) {
            throw new ConflictException("El paciente ya tiene un ingreso activo");
        }
    }

    private Medico resolverMedico(UsuarioPrincipal actor, UUID medicoId) {
        if (tieneRol(actor, RolNombre.ROLE_MEDICO)) {
            return medicoRepository
                    .findByUsuarioEmail(actor.getEmail())
                    .orElseThrow(() -> new ForbiddenOperationException("No se encontró el perfil médico"));
        }
        if (medicoId == null) {
            return null;
        }
        return medicoRepository
                .findByIdWithDetails(medicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", "id", medicoId));
    }

    private static boolean coincide(IngresoHospitalario ingreso, String termino) {
        return ingreso.getCodigo().toLowerCase(Locale.ROOT).contains(termino)
                || ingreso.getPaciente().getDni().contains(termino)
                || ingreso.getPaciente()
                        .getUsuario()
                        .getNombreCompleto()
                        .toLowerCase(Locale.ROOT)
                        .contains(termino);
    }

    private static boolean esDuenio(UsuarioPrincipal actor, IngresoHospitalario ingreso) {
        return ingreso.getPaciente().getUsuario().getId().equals(actor.getId());
    }

    private boolean puedeVerClinico(UsuarioPrincipal actor, IngresoHospitalario ingreso) {
        if (esDuenio(actor, ingreso) || tieneRol(actor, RolNombre.ROLE_ENFERMERIA)) {
            return true;
        }
        return tieneRol(actor, RolNombre.ROLE_MEDICO) && medicoPuedeVer(actor, ingreso);
    }

    private boolean puedeVer(UsuarioPrincipal actor, IngresoHospitalario ingreso) {
        if (puedeVerClinico(actor, ingreso)) {
            return true;
        }
        return tieneRol(actor, RolNombre.ROLE_ADMIN) || tieneRol(actor, RolNombre.ROLE_RECEPCION);
    }

    private boolean medicoPuedeVer(UsuarioPrincipal actor, IngresoHospitalario ingreso) {
        Medico asignado = ingreso.getMedico();
        if (asignado == null) {
            return ingreso.getTipo() == TipoIngresoHosp.EMERGENCIA;
        }
        return asignado.getUsuario().getId().equals(actor.getId())
                || relacionAsistencial.tieneRelacion(
                        actor, ingreso.getPaciente().getId());
    }

    private IngresoHospResponse paraActor(IngresoHospResponse ingreso, UsuarioPrincipal actor) {
        boolean clinico = tieneRol(actor, RolNombre.ROLE_MEDICO)
                || tieneRol(actor, RolNombre.ROLE_ENFERMERIA)
                || actor.getRoles().contains(RolNombre.ROLE_PACIENTE.name());
        return clinico ? ingreso : ingreso.sinDatosClinicos();
    }

    private void exigirMedicoDelIngreso(UsuarioPrincipal actor, IngresoHospitalario ingreso, String mensaje) {
        if (!tieneRol(actor, RolNombre.ROLE_MEDICO)) {
            throw new ForbiddenOperationException(mensaje);
        }
        Medico asignado = ingreso.getMedico();
        if (asignado == null) {
            if (ingreso.getTipo() != TipoIngresoHosp.EMERGENCIA) {
                throw new ForbiddenOperationException(mensaje);
            }
            ingreso.setMedico(medicoRepository
                    .findByUsuarioEmail(actor.getEmail())
                    .orElseThrow(() -> new ForbiddenOperationException(mensaje)));
            return;
        }
        if (!asignado.getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException(mensaje);
        }
    }

    private static boolean tieneRol(UsuarioPrincipal actor, RolNombre rol) {
        return actor.getRoles().contains(rol.name());
    }

    private static String normalizarCodigo(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static String vacioANulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
