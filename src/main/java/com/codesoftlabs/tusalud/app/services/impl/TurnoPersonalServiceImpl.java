package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.request.TurnoPersonalRequest;
import com.codesoftlabs.tusalud.app.dto.response.PlantillaTurnoResponse;
import com.codesoftlabs.tusalud.app.dto.response.SemanaTurnosResponse;
import com.codesoftlabs.tusalud.app.dto.response.TurnoPersonalResponse;
import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.PlantillaTurno;
import com.codesoftlabs.tusalud.app.entities.TurnoPersonal;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.AusenciaRepository;
import com.codesoftlabs.tusalud.app.repositories.EmpleadoRepository;
import com.codesoftlabs.tusalud.app.repositories.PlantillaTurnoRepository;
import com.codesoftlabs.tusalud.app.repositories.TurnoPersonalRepository;
import com.codesoftlabs.tusalud.app.repositories.UsuarioRepository;
import com.codesoftlabs.tusalud.app.security.UsuarioPrincipal;
import com.codesoftlabs.tusalud.app.services.interfaces.AuditoriaRrhhService;
import com.codesoftlabs.tusalud.app.services.interfaces.TurnoPersonalService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
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
public class TurnoPersonalServiceImpl implements TurnoPersonalService {

    private static final double MAX_HORAS_SEMANALES = 48.0;
    private static final long MAX_DIAS_CONSULTA = 62;

    private final TurnoPersonalRepository turnoRepository;
    private final PlantillaTurnoRepository plantillaRepository;
    private final EmpleadoRepository empleadoRepository;
    private final AusenciaRepository ausenciaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaRrhhService auditoria;

    @Override
    public List<PlantillaTurnoResponse> plantillas() {
        return plantillaRepository.findByActivaTrueOrderByHoraInicioAsc().stream()
                .map(PlantillaTurnoResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public TurnoPersonalResponse asignar(TurnoPersonalRequest request, UsuarioPrincipal actor) {
        Empleado empleado = empleadoRepository
                .findByIdConUsuario(request.empleadoId())
                .orElseThrow(() -> new ResourceNotFoundException("Empleado", "id", request.empleadoId()));
        if (empleado.getEstado() != EstadoEmpleado.ACTIVO) {
            throw new ConflictException("Solo se asignan turnos a empleados activos");
        }
        PlantillaTurno plantilla = plantillaRepository
                .findById(request.plantillaId())
                .filter(PlantillaTurno::isActiva)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla de turno", "id", request.plantillaId()));
        LocalDate fecha = request.fecha();
        if (fecha.isBefore(LocalDate.now())) {
            throw new DatosInvalidosException("No se pueden asignar turnos en fechas pasadas");
        }
        long ausencias = ausenciaRepository.contarSolapadas(
                empleado.getId(), fecha, fecha, EnumSet.of(EstadoAusencia.APROBADA, EstadoAusencia.SOLICITADA));
        if (ausencias > 0) {
            throw new ConflictException("El empleado tiene una ausencia aprobada o solicitada en esa fecha");
        }
        TurnoPersonal nuevo = TurnoPersonal.builder()
                .empleado(empleado)
                .plantilla(plantilla)
                .fecha(fecha)
                .notas(
                        request.notas() == null || request.notas().isBlank()
                                ? null
                                : request.notas().trim())
                .creadoPor(usuarioRepository.getReferenceById(actor.getId()))
                .build();
        for (TurnoPersonal existente :
                turnoRepository.findDeEmpleado(empleado.getId(), fecha.minusDays(1), fecha.plusDays(1))) {
            if (existente.inicio().isBefore(nuevo.fin()) && nuevo.inicio().isBefore(existente.fin())) {
                throw new ConflictException("El turno se cruza con otro turno del empleado: "
                        + existente.getPlantilla().getNombre() + " del " + existente.getFecha());
            }
        }
        LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        double horas = turnoRepository.findDeEmpleado(empleado.getId(), lunes, lunes.plusDays(6)).stream()
                        .mapToDouble(t -> t.getPlantilla().duracion().toMinutes() / 60.0)
                        .sum()
                + plantilla.duracion().toMinutes() / 60.0;
        if (horas > MAX_HORAS_SEMANALES) {
            throw new ConflictException(
                    "El turno supera el máximo de " + (int) MAX_HORAS_SEMANALES + " horas semanales del empleado");
        }
        TurnoPersonal guardado = turnoRepository.saveAndFlush(nuevo);
        auditoria.registrar(
                actor, empleado.getId(), AuditoriaRrhhService.TURNO_ASIGNADO, plantilla.getNombre() + " el " + fecha);
        log.info("Turno {} asignado al empleado {} por {}", guardado.getId(), empleado.getId(), actor.getId());
        return TurnoPersonalResponse.from(guardado);
    }

    @Override
    @Transactional
    public void eliminar(UUID id, UsuarioPrincipal actor) {
        TurnoPersonal turno = turnoRepository
                .findByIdConDetalle(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turno", "id", id));
        if (turno.getFecha().isBefore(LocalDate.now())) {
            throw new ConflictException("No se pueden eliminar turnos de fechas pasadas");
        }
        turnoRepository.delete(turno);
        auditoria.registrar(
                actor,
                turno.getEmpleado().getId(),
                AuditoriaRrhhService.TURNO_ELIMINADO,
                turno.getPlantilla().getNombre() + " del " + turno.getFecha());
        log.info("Turno {} eliminado por {}", id, actor.getId());
    }

    @Override
    public List<TurnoPersonalResponse> listar(LocalDate desde, LocalDate hasta, AreaLaboral area, UUID empleadoId) {
        LocalDate inicio =
                desde == null ? LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) : desde;
        LocalDate fin = hasta == null ? inicio.plusDays(6) : hasta;
        validarRango(inicio, fin);
        return turnoRepository.findEntre(inicio, fin, area, empleadoId).stream()
                .map(TurnoPersonalResponse::from)
                .toList();
    }

    @Override
    public SemanaTurnosResponse semana(LocalDate fecha, AreaLaboral area) {
        LocalDate lunes =
                (fecha == null ? LocalDate.now() : fecha).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);
        List<TurnoPersonal> turnos = turnoRepository.findEntre(lunes, domingo, area, null);
        List<Ausencia> ausencias = ausenciaRepository.aprobadasEntre(lunes, domingo);
        List<SemanaTurnosResponse.Fila> filas = new ArrayList<>();
        for (Empleado e : empleadoRepository.activosPorArea(area)) {
            List<SemanaTurnosResponse.Dia> dias = new ArrayList<>();
            double horas = 0;
            for (int i = 0; i < 7; i++) {
                LocalDate dia = lunes.plusDays(i);
                List<TurnoPersonalResponse> deldia = turnos.stream()
                        .filter(t -> t.getEmpleado().getId().equals(e.getId())
                                && t.getFecha().equals(dia))
                        .map(TurnoPersonalResponse::from)
                        .toList();
                horas += deldia.stream()
                        .mapToDouble(TurnoPersonalResponse::horas)
                        .sum();
                var ausencia = ausencias.stream()
                        .filter(a -> a.getEmpleado().getId().equals(e.getId())
                                && !a.getFechaInicio().isAfter(dia)
                                && !a.getFechaFin().isBefore(dia))
                        .findFirst()
                        .map(Ausencia::getTipo)
                        .orElse(null);
                dias.add(new SemanaTurnosResponse.Dia(dia, deldia, ausencia));
            }
            filas.add(new SemanaTurnosResponse.Fila(
                    e.getId(), e.getUsuario().getNombreCompleto(), e.getCargo(), e.getArea(), horas, dias));
        }
        return new SemanaTurnosResponse(lunes, domingo, area, filas);
    }

    @Override
    public List<TurnoPersonalResponse> misTurnos(UsuarioPrincipal actor, LocalDate desde, LocalDate hasta) {
        Empleado empleado = empleadoRepository
                .findByUsuarioId(actor.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No tienes una ficha laboral registrada"));
        LocalDate inicio = desde == null ? LocalDate.now() : desde;
        LocalDate fin = hasta == null ? inicio.plusDays(13) : hasta;
        validarRango(inicio, fin);
        return turnoRepository.findDeEmpleado(empleado.getId(), inicio, fin).stream()
                .map(TurnoPersonalResponse::from)
                .toList();
    }

    private static void validarRango(LocalDate inicio, LocalDate fin) {
        if (fin.isBefore(inicio)) {
            throw new DatosInvalidosException("La fecha final no puede ser anterior a la inicial");
        }
        if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAX_DIAS_CONSULTA) {
            throw new DatosInvalidosException("El rango no puede superar " + MAX_DIAS_CONSULTA + " días");
        }
    }
}
