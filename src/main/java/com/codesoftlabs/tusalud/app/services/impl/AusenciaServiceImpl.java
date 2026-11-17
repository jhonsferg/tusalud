package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.AusenciaRequest;
import com.codesoftlabs.tusalud.app.dto.response.AusenciaResponse;
import com.codesoftlabs.tusalud.app.dto.response.MisAusenciasResponse;
import com.codesoftlabs.tusalud.app.dto.response.SaldoVacacionesResponse;
import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AusenciaRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaRrhhService;
import com.codesoftlabs.tusalud.app.services.interfaces.AusenciaService;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AusenciaServiceImpl implements AusenciaService {

    private static final int MAX_DIAS_AUSENCIA = 60;
    private static final int DIAS_RETROACTIVOS_DESCANSO = 7;
    private static final long MAX_DIAS_CONSULTA = 366;

    private final AusenciaRepository ausenciaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;
    private final AuditoriaRrhhService auditoria;

    @Override
    @Transactional
    public AusenciaResponse solicitar(UsuarioPrincipal actor, AusenciaRequest request) {
        Empleado empleado = empleadoDe(actor);
        if (empleado.getEstado() == EstadoEmpleado.CESADO) {
            throw new ConflictException("Un empleado cesado no puede solicitar ausencias");
        }
        LocalDate inicio = request.fechaInicio();
        LocalDate fin = request.fechaFin();
        if (fin.isBefore(inicio)) {
            throw new DatosInvalidosException("La fecha de fin no puede ser anterior a la de inicio");
        }
        LocalDate minimo = request.tipo() == TipoAusencia.DESCANSO_MEDICO
                ? LocalDate.now().minusDays(DIAS_RETROACTIVOS_DESCANSO)
                : LocalDate.now();
        if (inicio.isBefore(minimo)) {
            throw new DatosInvalidosException("La fecha de inicio no puede ser anterior al " + minimo);
        }
        int dias = (int) ChronoUnit.DAYS.between(inicio, fin) + 1;
        if (dias > MAX_DIAS_AUSENCIA) {
            throw new DatosInvalidosException("Una ausencia no puede superar " + MAX_DIAS_AUSENCIA + " días");
        }
        if (ausenciaRepository.contarSolapadas(
                        empleado.getId(), inicio, fin, EnumSet.of(EstadoAusencia.SOLICITADA, EstadoAusencia.APROBADA))
                > 0) {
            throw new ConflictException("Ya tienes una ausencia solicitada o aprobada en esas fechas");
        }
        if (request.tipo() == TipoAusencia.VACACIONES) {
            SaldoVacacionesResponse saldo = calcularSaldo(empleado, inicio.getYear());
            if (dias > saldo.saldo()) {
                throw new ConflictException("Los días solicitados superan tu saldo de vacaciones de " + saldo.saldo());
            }
        }
        Ausencia ausencia = ausenciaRepository.saveAndFlush(Ausencia.builder()
                .empleado(empleado)
                .tipo(request.tipo())
                .fechaInicio(inicio)
                .fechaFin(fin)
                .dias(dias)
                .motivo(
                        request.motivo() == null || request.motivo().isBlank()
                                ? null
                                : request.motivo().trim())
                .estado(EstadoAusencia.SOLICITADA)
                .build());
        usuarioRepository
                .findActivosPorRol(RolNombre.ROLE_RRHH)
                .forEach(u -> notificacionService.crear(
                        u,
                        TipoNotificacion.SOLICITUD_AUSENCIA,
                        "Nueva solicitud de ausencia",
                        empleado.getUsuario().getNombreCompleto() + " solicitó "
                                + request.tipo().name().toLowerCase().replace('_', ' ') + " del " + inicio + " al "
                                + fin,
                        "/ausencias"));
        log.info("Ausencia {} solicitada por el empleado {}", ausencia.getId(), empleado.getId());
        return AusenciaResponse.from(ausencia);
    }

    @Override
    public MisAusenciasResponse misAusencias(UsuarioPrincipal actor) {
        Empleado empleado = empleadoDe(actor);
        return new MisAusenciasResponse(
                calcularSaldo(empleado, LocalDate.now().getYear()),
                ausenciaRepository.findDeEmpleado(empleado.getId()).stream()
                        .map(AusenciaResponse::from)
                        .toList());
    }

    @Override
    @Transactional
    public AusenciaResponse cancelarPropia(UsuarioPrincipal actor, UUID id) {
        Empleado empleado = empleadoDe(actor);
        Ausencia ausencia = buscar(id);
        if (!ausencia.getEmpleado().getId().equals(empleado.getId())) {
            throw new ResourceNotFoundException("Ausencia", "id", id);
        }
        if (ausencia.getEstado() != EstadoAusencia.SOLICITADA) {
            throw new ConflictException("Solo puedes cancelar una solicitud que aún no fue resuelta");
        }
        ausencia.setEstado(EstadoAusencia.CANCELADA);
        ausenciaRepository.saveAndFlush(ausencia);
        return AusenciaResponse.from(ausencia);
    }

    @Override
    public List<AusenciaResponse> listar(
            EstadoAusencia estado, AreaLaboral area, LocalDate desde, LocalDate hasta, UsuarioPrincipal actor) {
        LocalDate inicio = desde == null ? LocalDate.now().minusDays(30) : desde;
        LocalDate fin = hasta == null ? LocalDate.now().plusDays(120) : hasta;
        if (fin.isBefore(inicio)) {
            throw new DatosInvalidosException("La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAX_DIAS_CONSULTA) {
            throw new DatosInvalidosException("El rango no puede superar " + MAX_DIAS_CONSULTA + " días");
        }
        boolean verMotivo = actor.getRoles().contains(RolNombre.ROLE_RRHH.name());
        return ausenciaRepository.filtrar(estado, area, inicio, fin).stream()
                .map(a -> verMotivo
                        ? AusenciaResponse.from(a)
                        : AusenciaResponse.from(a).sinMotivos())
                .toList();
    }

    @Override
    @Transactional
    public AusenciaResponse aprobar(UUID id, String motivo, UsuarioPrincipal actor) {
        Ausencia ausencia = resolvible(id, actor);
        if (ausencia.getTipo() == TipoAusencia.VACACIONES) {
            SaldoVacacionesResponse saldo = calcularSaldo(
                    ausencia.getEmpleado(), ausencia.getFechaInicio().getYear());
            if (saldo.saldo() + ausencia.getDias() < ausencia.getDias()) {
                throw new ConflictException("El empleado no tiene saldo de vacaciones suficiente");
            }
        }
        cerrar(ausencia, EstadoAusencia.APROBADA, motivo, actor);
        auditoria.registrar(
                actor,
                ausencia.getEmpleado().getId(),
                AuditoriaRrhhService.AUSENCIA_APROBADA,
                ausencia.getTipo() + " del " + ausencia.getFechaInicio() + " al " + ausencia.getFechaFin());
        notificar(ausencia, "Tu solicitud de ausencia fue aprobada");
        return AusenciaResponse.from(ausencia);
    }

    @Override
    @Transactional
    public AusenciaResponse rechazar(UUID id, String motivo, UsuarioPrincipal actor) {
        if (motivo == null || motivo.isBlank()) {
            throw new DatosInvalidosException("Indique el motivo del rechazo");
        }
        Ausencia ausencia = resolvible(id, actor);
        cerrar(ausencia, EstadoAusencia.RECHAZADA, motivo, actor);
        auditoria.registrar(
                actor,
                ausencia.getEmpleado().getId(),
                AuditoriaRrhhService.AUSENCIA_RECHAZADA,
                ausencia.getTipo() + " del " + ausencia.getFechaInicio() + " al " + ausencia.getFechaFin());
        notificar(ausencia, "Tu solicitud de ausencia fue rechazada");
        return AusenciaResponse.from(ausencia);
    }

    @Override
    @Transactional
    public AusenciaResponse cancelar(UUID id, UsuarioPrincipal actor) {
        Ausencia ausencia = buscar(id);
        boolean cancelable = ausencia.getEstado() == EstadoAusencia.SOLICITADA
                || (ausencia.getEstado() == EstadoAusencia.APROBADA
                        && !ausencia.getFechaFin().isBefore(LocalDate.now()));
        if (!cancelable) {
            throw new ConflictException("La ausencia ya no se puede cancelar");
        }
        ausencia.setEstado(EstadoAusencia.CANCELADA);
        ausencia.setResueltoPor(usuarioRepository.getReferenceById(actor.getId()));
        ausencia.setResueltoEn(LocalDateTime.now());
        ausenciaRepository.saveAndFlush(ausencia);
        auditoria.registrar(
                actor,
                ausencia.getEmpleado().getId(),
                AuditoriaRrhhService.AUSENCIA_CANCELADA,
                ausencia.getTipo() + " del " + ausencia.getFechaInicio());
        return AusenciaResponse.from(ausencia);
    }

    @Override
    public SaldoVacacionesResponse saldo(UUID empleadoId) {
        Empleado empleado = empleadoRepository
                .findByIdConUsuario(empleadoId)
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", empleadoId));
        return calcularSaldo(empleado, LocalDate.now().getYear());
    }

    private Ausencia resolvible(UUID id, UsuarioPrincipal actor) {
        Ausencia ausencia = buscar(id);
        if (ausencia.getEmpleado().getUsuario().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("No puedes resolver tu propia solicitud de ausencia");
        }
        if (ausencia.getEstado() != EstadoAusencia.SOLICITADA) {
            throw new ConflictException("La solicitud ya fue resuelta");
        }
        return ausencia;
    }

    private void cerrar(Ausencia ausencia, EstadoAusencia estado, String motivo, UsuarioPrincipal actor) {
        ausencia.setEstado(estado);
        ausencia.setResueltoPor(usuarioRepository.getReferenceById(actor.getId()));
        ausencia.setResueltoEn(LocalDateTime.now());
        ausencia.setMotivoResolucion(motivo == null || motivo.isBlank() ? null : motivo.trim());
        ausenciaRepository.saveAndFlush(ausencia);
    }

    private void notificar(Ausencia ausencia, String titulo) {
        notificacionService.crear(
                ausencia.getEmpleado().getUsuario(),
                TipoNotificacion.AUSENCIA_RESUELTA,
                titulo,
                ausencia.getTipo().name().toLowerCase().replace('_', ' ') + " del " + ausencia.getFechaInicio() + " al "
                        + ausencia.getFechaFin(),
                "/ausencias");
    }

    private SaldoVacacionesResponse calcularSaldo(Empleado empleado, int anio) {
        LocalDate desde = LocalDate.of(anio, 1, 1);
        LocalDate hasta = LocalDate.of(anio, 12, 31);
        int tomados = (int) ausenciaRepository.sumarDias(
                empleado.getId(), TipoAusencia.VACACIONES, EstadoAusencia.APROBADA, desde, hasta);
        int pendientes = (int) ausenciaRepository.sumarDias(
                empleado.getId(), TipoAusencia.VACACIONES, EstadoAusencia.SOLICITADA, desde, hasta);
        int anuales = empleado.getDiasVacacionesAnuales();
        return new SaldoVacacionesResponse(
                empleado.getId(), anio, anuales, tomados, pendientes, Math.max(0, anuales - tomados - pendientes));
    }

    private Empleado empleadoDe(UsuarioPrincipal actor) {
        return empleadoRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes una ficha laboral registrada"));
    }

    private Ausencia buscar(UUID id) {
        return ausenciaRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ausencia", "id", id));
    }
}
