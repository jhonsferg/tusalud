package com.codesoftlabs.tusalud.app.services.impl;

import com.codesoftlabs.tusalud.app.dto.response.PreferenciasResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.EventoCalendario;
import com.codesoftlabs.tusalud.app.entities.Medico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.PreferenciaUsuario;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import com.codesoftlabs.tusalud.app.repositories.CitaRepository;
import com.codesoftlabs.tusalud.app.repositories.EventoCalendarioRepository;
import com.codesoftlabs.tusalud.app.repositories.PreferenciaUsuarioRepository;
import com.codesoftlabs.tusalud.app.services.interfaces.NotificacionService;
import com.codesoftlabs.tusalud.app.services.interfaces.RecordatorioService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordatorioServiceImpl implements RecordatorioService {

    private static final Duration HORIZONTE = Duration.ofHours(72);
    private static final Duration HORIZONTE_EVENTOS = Duration.ofDays(7);
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CitaRepository citaRepository;
    private final PreferenciaUsuarioRepository preferenciaRepository;
    private final EventoCalendarioRepository eventoRepository;
    private final NotificacionService notificacionService;

    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES, initialDelay = 1)
    @Transactional
    public void ejecutarProgramado() {
        int enviados = enviarRecordatorios(LocalDateTime.now());
        log.debug("Recordatorios de citas enviados: {}", enviados);
    }

    @Override
    @Transactional
    public int enviarRecordatorios(LocalDateTime ahora) {
        List<Cita> candidatas = citaRepository.findRecordatoriosPendientes(ahora, ahora.plus(HORIZONTE));
        int enviados = 0;
        for (Cita cita : candidatas) {
            Paciente paciente = cita.getPaciente();
            Medico medico = cita.getMedico();
            PreferenciaUsuario preferencia = preferenciaRepository
                    .findByUsuarioId(paciente.getUsuario().getId())
                    .orElse(null);
            boolean activos = preferencia == null || preferencia.isRecordatoriosActivos();
            int minutos = preferencia == null
                    ? PreferenciasResponse.MINUTOS_POR_DEFECTO
                    : preferencia.getMinutosAntelacionRecordatorio();
            if (!activos || cita.getFechaHora().isAfter(ahora.plusMinutes(minutos))) {
                continue;
            }
            String fecha = cita.getFechaHora().format(FORMATO_FECHA);
            String enlace = "/citas/" + cita.getId();
            notificacionService.crear(
                    paciente.getUsuario(),
                    TipoNotificacion.RECORDATORIO_CITA,
                    "Recordatorio de cita",
                    "Tiene una cita con " + medico.getUsuario().getNombreCompleto() + " el " + fecha + ".",
                    enlace);
            notificacionService.crear(
                    medico.getUsuario(),
                    TipoNotificacion.RECORDATORIO_CITA,
                    "Recordatorio de cita",
                    "Tiene una cita con " + paciente.getUsuario().getNombreCompleto() + " el " + fecha + ".",
                    enlace);
            cita.setRecordatorioEnviado(true);
            enviados++;
        }
        return enviados + enviarRecordatoriosDeEventos(ahora);
    }

    private int enviarRecordatoriosDeEventos(LocalDateTime ahora) {
        int enviados = 0;
        for (EventoCalendario evento :
                eventoRepository.findRecordatoriosPendientes(ahora, ahora.plus(HORIZONTE_EVENTOS))) {
            if (evento.getInicio().minusMinutes(evento.getRecordatorioMinutos()).isAfter(ahora)) {
                continue;
            }
            notificacionService.crear(
                    evento.getUsuario(),
                    TipoNotificacion.RECORDATORIO_EVENTO,
                    "Recordatorio: " + evento.getTitulo(),
                    "Empieza el " + evento.getInicio().format(FORMATO_FECHA) + ".",
                    "/calendario");
            evento.setRecordatorioEnviado(true);
            enviados++;
        }
        return enviados;
    }
}
