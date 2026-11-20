package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.AjusteAsistenciaRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.ConsolidarRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.MarcacionManualRequest;
import com.codesoftlabs.tusalud.app.dto.request.PlanillaRequests.MarcacionTerminalRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.AsistenciaDiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ConsolidacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MarcacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MarcacionTerminalResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ResumenAsistenciaResponse;
import com.codesoftlabs.tusalud.app.entities.AsistenciaDia;
import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.Marcacion;
import com.codesoftlabs.tusalud.app.entities.TerminalAsistencia;
import com.codesoftlabs.tusalud.app.entities.TurnoPersonal;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.DestinoExtra;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoAsistencia;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.OrigenMarcacion;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoMarcacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AsistenciaDiaRepository;
import com.codesoftlabs.tusalud.app.repositories.AusenciaRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.MarcacionRepository;
import com.codesoftlabs.tusalud.app.repositories.PlanillaRepository;
import com.codesoftlabs.tusalud.app.repositories.TurnoPersonalRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AsistenciaService {

    static final int MINUTOS_EXTRA_MINIMOS = 15;
    private static final int MAXIMO_DIAS = 62;
    private static final int SEGUNDOS_ENTRE_MARCACIONES = 60;
    private static final String ENTIDAD = "ASISTENCIA";
    private static final Set<EstadoPlanilla> BLOQUEANTES =
            EnumSet.of(EstadoPlanilla.APROBADA, EstadoPlanilla.PAGADA, EstadoPlanilla.CERRADA);

    private final EmpleadoRepository empleados;
    private final MarcacionRepository marcaciones;
    private final AsistenciaDiaRepository asistencias;
    private final TurnoPersonalRepository turnos;
    private final AusenciaRepository ausencias;
    private final PlanillaRepository planillas;
    private final UsuarioRepository usuarioRepository;
    private final TerminalesAsistenciaService terminales;
    private final ParametrosPlanillaService parametros;
    private final PlanillaAuditor auditor;

    public List<MarcacionResponse> listarMarcaciones(UUID empleadoId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return marcaciones
                .entre(empleadoId, desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream()
                .map(PlanillaMapper::marcacion)
                .toList();
    }

    @Transactional
    public MarcacionResponse registrarManual(UsuarioPrincipal actor, MarcacionManualRequest request) {
        Empleado empleado = empleados
                .findByIdConUsuario(request.empleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", request.empleadoId()));
        if (request.fechaHora().isAfter(LocalDateTime.now().plusMinutes(5))) {
            throw new DatosInvalidosException("No se puede registrar una marcación en el futuro");
        }
        verificarMesAbierto(request.fechaHora().toLocalDate());
        if (marcaciones.existsByEmpleadoIdAndFechaHoraAndTipo(empleado.getId(), request.fechaHora(), request.tipo())) {
            throw new ConflictException("Ya existe esa marcación");
        }
        Usuario usuario = usuarioRepository.getReferenceById(actor.getId());
        Marcacion marcacion = marcaciones.save(Marcacion.builder()
                .empleado(empleado)
                .tipo(request.tipo())
                .fechaHora(request.fechaHora())
                .origen(OrigenMarcacion.MANUAL)
                .registradoPor(usuario)
                .observacion(request.observacion().trim())
                .build());
        auditor.registrar(
                usuario,
                ENTIDAD,
                marcacion.getId(),
                "MARCACION_MANUAL",
                request.tipo() + " " + request.fechaHora() + " "
                        + empleado.getUsuario().getNombreCompleto());
        return PlanillaMapper.marcacion(marcacion);
    }

    @Transactional
    public MarcacionTerminalResponse marcarEnTerminal(String token, MarcacionTerminalRequest request) {
        TerminalAsistencia terminal = terminales
                .autenticar(token)
                .orElseThrow(() -> new ResourceNotFoundException("Marcación", "código", "no válido"));
        Empleado empleado = empleados
                .findByCodigoMarcacionIgnoreCaseAndEstado(request.codigo(), EstadoEmpleado.ACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Marcación", "código", "no válido"));
        LocalDateTime ahora = LocalDateTime.now().withNano(0);
        Optional<Marcacion> ultima = marcaciones.findFirstByEmpleadoIdOrderByFechaHoraDesc(empleado.getId());
        if (ultima.isPresent()
                && Duration.between(ultima.get().getFechaHora(), ahora).getSeconds() < SEGUNDOS_ENTRE_MARCACIONES) {
            throw new ConflictException("Espere un minuto antes de volver a marcar");
        }
        TipoMarcacion tipo = request.tipo() != null
                ? request.tipo()
                : (ultima.isPresent()
                                && ultima.get().getTipo() == TipoMarcacion.ENTRADA
                                && Duration.between(ultima.get().getFechaHora(), ahora)
                                                .toHours()
                                        < 18
                        ? TipoMarcacion.SALIDA
                        : TipoMarcacion.ENTRADA);
        verificarMesAbierto(ahora.toLocalDate());
        marcaciones.save(Marcacion.builder()
                .empleado(empleado)
                .tipo(tipo)
                .fechaHora(ahora)
                .origen(OrigenMarcacion.TERMINAL)
                .terminal(terminal)
                .build());
        return new MarcacionTerminalResponse(tipo, ahora, empleado.getUsuario().getNombre());
    }

    public List<AsistenciaDiaResponse> dias(UUID empleadoId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return asistencias.entre(desde, hasta, empleadoId).stream()
                .map(PlanillaMapper::asistencia)
                .toList();
    }

    @Transactional
    public ConsolidacionResponse consolidar(UsuarioPrincipal actor, ConsolidarRequest request) {
        ConsolidacionResponse respuesta = consolidarRango(request.desde(), request.hasta(), request.empleadoId());
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                request.empleadoId(),
                "CONSOLIDAR",
                request.desde() + " a " + request.hasta() + ", " + respuesta.dias() + " días");
        return respuesta;
    }

    @Transactional
    public ConsolidacionResponse consolidarRango(LocalDate desde, LocalDate hasta, UUID empleadoId) {
        validarRango(desde, hasta);
        LocalDate hoy = LocalDate.now();
        LocalDate fin = hasta.isAfter(hoy) ? hoy : hasta;
        if (fin.isBefore(desde)) {
            return new ConsolidacionResponse(0, 0, 0);
        }
        Map<UUID, Map<LocalDate, TurnoPersonal>> turnosPorEmpleado = new HashMap<>();
        for (TurnoPersonal t : turnos.findEntre(desde, fin, null, empleadoId)) {
            turnosPorEmpleado
                    .computeIfAbsent(t.getEmpleado().getId(), k -> new HashMap<>())
                    .putIfAbsent(t.getFecha(), t);
        }
        Map<UUID, List<Ausencia>> ausenciasPorEmpleado = new HashMap<>();
        for (Ausencia a : ausencias.filtrar(EstadoAusencia.APROBADA, null, desde, fin)) {
            if (empleadoId == null || empleadoId.equals(a.getEmpleado().getId())) {
                ausenciasPorEmpleado
                        .computeIfAbsent(a.getEmpleado().getId(), k -> new ArrayList<>())
                        .add(a);
            }
        }
        Map<UUID, List<Marcacion>> marcacionesPorEmpleado = new HashMap<>();
        for (Marcacion m : marcaciones.entre(
                empleadoId, desde.atStartOfDay(), fin.plusDays(2).atStartOfDay())) {
            marcacionesPorEmpleado
                    .computeIfAbsent(m.getEmpleado().getId(), k -> new ArrayList<>())
                    .add(m);
        }
        Set<UUID> ids = new HashSet<>(turnosPorEmpleado.keySet());
        ids.addAll(marcacionesPorEmpleado.keySet());
        Tolerancia ajustes = new Tolerancia(parametros.vigentesEn(fin));
        int dias = 0;
        int omitidas = 0;
        Map<UUID, Empleado> empleadosPorId = new HashMap<>();
        for (UUID id : ids) {
            Empleado empleado = empleados.findByIdConUsuario(id).orElse(null);
            if (empleado == null) {
                continue;
            }
            empleadosPorId.put(id, empleado);
            for (LocalDate fecha = desde; !fecha.isAfter(fin); fecha = fecha.plusDays(1)) {
                int resultado = consolidarDia(
                        empleado,
                        fecha,
                        turnosPorEmpleado.getOrDefault(id, Map.of()).get(fecha),
                        ausenciasPorEmpleado.getOrDefault(id, List.of()),
                        marcacionesPorEmpleado.getOrDefault(id, List.of()),
                        ajustes,
                        hoy);
                if (resultado == 1) {
                    dias++;
                } else if (resultado == -1) {
                    omitidas++;
                }
            }
        }
        return new ConsolidacionResponse(empleadosPorId.size(), dias, omitidas);
    }

    private record Tolerancia(CalculadoraPlanilla.Parametros parametros) {

        int tolerancia() {
            return parametros.get("TARDANZA_TOLERANCIA_MINUTOS").intValue();
        }
    }

    private int consolidarDia(
            Empleado empleado,
            LocalDate fecha,
            TurnoPersonal turno,
            List<Ausencia> ausenciasEmpleado,
            List<Marcacion> marcacionesEmpleado,
            Tolerancia ajustes,
            LocalDate hoy) {
        boolean ausente = ausenciasEmpleado.stream()
                .anyMatch(a -> !fecha.isBefore(a.getFechaInicio()) && !fecha.isAfter(a.getFechaFin()));
        LocalDateTime inicioDia = fecha.atStartOfDay();
        LocalDateTime finDia = fecha.plusDays(1).atStartOfDay();
        List<Marcacion> deldia = marcacionesEmpleado.stream()
                .filter(m -> !m.getFechaHora().isBefore(inicioDia)
                        && m.getFechaHora().isBefore(finDia))
                .sorted(Comparator.comparing(Marcacion::getFechaHora))
                .toList();
        LocalDateTime entrada = deldia.stream()
                .filter(m -> m.getTipo() == TipoMarcacion.ENTRADA)
                .map(Marcacion::getFechaHora)
                .findFirst()
                .orElse(null);
        LocalDateTime salida = null;
        if (entrada != null) {
            LocalDateTime tope =
                    turno != null && turno.getPlantilla().cruzaMedianoche() ? entrada.plusHours(18) : finDia;
            final LocalDateTime inicio = entrada;
            salida = marcacionesEmpleado.stream()
                    .filter(m -> m.getTipo() == TipoMarcacion.SALIDA
                            && m.getFechaHora().isAfter(inicio)
                            && m.getFechaHora().isBefore(tope))
                    .map(Marcacion::getFechaHora)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
        }
        if (turno == null && entrada == null) {
            return 0;
        }
        if (turno != null && !ausente && entrada == null && !fecha.isBefore(hoy)) {
            return 0;
        }

        AsistenciaDia dia = asistencias
                .findByEmpleadoIdAndFecha(empleado.getId(), fecha)
                .orElseGet(() -> AsistenciaDia.builder()
                        .empleado(empleado)
                        .fecha(fecha)
                        .extraDestino(DestinoExtra.PAGO)
                        .build());
        if (dia.getAjustadoPor() != null) {
            return -1;
        }
        dia.setTurnoPersonal(turno);
        dia.setProgramadaInicio(turno == null ? null : turno.getPlantilla().getHoraInicio());
        dia.setProgramadaFin(turno == null ? null : turno.getPlantilla().getHoraFin());
        dia.setEntrada(entrada);
        dia.setSalida(salida);
        int tardanza = 0;
        int trabajados = 0;
        int extra = 0;
        EstadoAsistencia estado;
        if (ausente) {
            estado = EstadoAsistencia.AUSENCIA_APROBADA;
        } else if (turno == null) {
            estado = EstadoAsistencia.SIN_TURNO;
            trabajados =
                    salida == null ? 0 : (int) Duration.between(entrada, salida).toMinutes();
        } else if (entrada == null) {
            estado = EstadoAsistencia.FALTA;
        } else {
            LocalDateTime programadoInicio = turno.inicio();
            long tarde = Duration.between(programadoInicio, entrada).toMinutes();
            tardanza = tarde > ajustes.tolerancia() ? (int) tarde : 0;
            if (salida == null) {
                long duracion = turno.getPlantilla().duracion().toMinutes();
                trabajados = (int) Math.max(0, duracion - tardanza);
                estado = fecha.isBefore(hoy) ? EstadoAsistencia.SIN_SALIDA : EstadoAsistencia.PRESENTE;
            } else {
                trabajados = (int) Duration.between(entrada, salida).toMinutes();
                long sobra = Duration.between(turno.fin(), salida).toMinutes();
                extra = sobra >= MINUTOS_EXTRA_MINIMOS ? (int) sobra : 0;
                estado = tardanza > 0 ? EstadoAsistencia.TARDANZA : EstadoAsistencia.PRESENTE;
            }
            if (salida == null && tardanza > 0 && estado == EstadoAsistencia.PRESENTE) {
                estado = EstadoAsistencia.TARDANZA;
            }
        }
        dia.setMinutosTardanza(tardanza);
        dia.setMinutosTrabajados(trabajados);
        dia.setMinutosExtra(extra);
        dia.setEstado(estado);
        dia.setActualizadoEn(LocalDateTime.now());
        asistencias.save(dia);
        return 1;
    }

    @Transactional
    public AsistenciaDiaResponse ajustar(UsuarioPrincipal actor, UUID id, AjusteAsistenciaRequest request) {
        AsistenciaDia dia = asistencias
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asistencia", "id", id));
        verificarMesAbierto(dia.getFecha());
        if (request.faltaJustificada() != null) {
            if (request.faltaJustificada() && dia.getEstado() != EstadoAsistencia.FALTA) {
                throw new ConflictException("Solo se justifican los días con falta");
            }
            dia.setFaltaJustificada(request.faltaJustificada());
        }
        if (request.minutosTardanza() != null) {
            dia.setMinutosTardanza(request.minutosTardanza());
        }
        if (request.minutosExtra() != null) {
            dia.setMinutosExtra(request.minutosExtra());
        }
        if (request.extraDestino() != null) {
            dia.setExtraDestino(request.extraDestino());
        }
        dia.setJustificacion(request.justificacion().trim());
        dia.setAjustadoPor(usuarioRepository.getReferenceById(actor.getId()));
        dia.setActualizadoEn(LocalDateTime.now());
        auditor.registrar(
                usuarioRepository.getReferenceById(actor.getId()),
                ENTIDAD,
                id,
                "AJUSTAR",
                dia.getEmpleado().getUsuario().getNombreCompleto() + " " + dia.getFecha());
        return PlanillaMapper.asistencia(dia);
    }

    @Transactional
    public ResumenAsistenciaResponse miAsistencia(UsuarioPrincipal actor, LocalDate desde, LocalDate hasta) {
        Empleado empleado = empleados.findByUsuarioId(actor.getId()).orElse(null);
        if (empleado == null) {
            return resumir(desde, hasta, List.of(), null);
        }
        consolidarRango(desde, hasta, empleado.getId());
        return resumir(desde, hasta, asistencias.entre(desde, hasta, empleado.getId()), empleado.getCodigoMarcacion());
    }

    public ResumenAsistenciaResponse resumen(UUID empleadoId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return resumir(desde, hasta, asistencias.entre(desde, hasta, empleadoId), null);
    }

    public List<MarcacionResponse> misMarcaciones(UsuarioPrincipal actor, LocalDate desde, LocalDate hasta) {
        Empleado empleado = empleados.findByUsuarioId(actor.getId()).orElse(null);
        if (empleado == null) {
            return List.of();
        }
        return listarMarcaciones(empleado.getId(), desde, hasta);
    }

    public long marcacionesDeHoy() {
        LocalDate hoy = LocalDate.now();
        return marcaciones
                .entre(null, hoy.atStartOfDay(), hoy.plusDays(1).atStartOfDay())
                .size();
    }

    private static ResumenAsistenciaResponse resumir(
            LocalDate desde, LocalDate hasta, List<AsistenciaDia> filas, String codigoMarcacion) {
        int presente = 0;
        int tardanza = 0;
        int falta = 0;
        int faltaJustificada = 0;
        int ausencia = 0;
        int minTardanza = 0;
        int minTrabajados = 0;
        int minExtra = 0;
        for (AsistenciaDia d : filas) {
            switch (d.getEstado()) {
                case PRESENTE, SIN_SALIDA, SIN_TURNO -> presente++;
                case TARDANZA -> {
                    presente++;
                    tardanza++;
                }
                case FALTA -> {
                    if (d.isFaltaJustificada()) {
                        faltaJustificada++;
                    } else {
                        falta++;
                    }
                }
                case AUSENCIA_APROBADA -> ausencia++;
            }
            minTardanza += d.getMinutosTardanza();
            minTrabajados += d.getMinutosTrabajados();
            minExtra += d.getMinutosExtra();
        }
        return new ResumenAsistenciaResponse(
                desde,
                hasta,
                presente,
                tardanza,
                falta,
                faltaJustificada,
                ausencia,
                minTardanza,
                minTrabajados,
                minExtra,
                codigoMarcacion,
                filas.stream().map(PlanillaMapper::asistencia).toList());
    }

    private void verificarMesAbierto(LocalDate fecha) {
        planillas
                .findByAnioAndMes(fecha.getYear(), fecha.getMonthValue())
                .filter(p -> BLOQUEANTES.contains(p.getEstado()))
                .ifPresent(p -> {
                    throw new ConflictException(
                            "La planilla " + p.getCodigo() + " ya fue aprobada, no admite cambios de asistencia");
                });
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new DatosInvalidosException("El rango de fechas no es válido");
        }
        if (Duration.between(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                        .toDays()
                > MAXIMO_DIAS) {
            throw new DatosInvalidosException("El rango no puede superar " + MAXIMO_DIAS + " días");
        }
    }
}
