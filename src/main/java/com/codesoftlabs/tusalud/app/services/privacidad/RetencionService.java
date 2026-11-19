package com.codesoftlabs.tusalud.app.services.privacidad;

import com.codesoftlabs.tusalud.app.dto.request.PrivacidadRequests.PoliticaRetencionRequest;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.EjecucionRetencionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PrivacidadResponses.PoliticaRetencionResponse;
import com.codesoftlabs.tusalud.app.entities.EjecucionRetencion;
import com.codesoftlabs.tusalud.app.entities.PoliticaRetencion;
import com.codesoftlabs.tusalud.app.exceptions.ResourceNotFoundException;
import com.codesoftlabs.tusalud.app.repositories.EjecucionRetencionRepository;
import com.codesoftlabs.tusalud.app.repositories.IntercambioSunatRepository;
import com.codesoftlabs.tusalud.app.repositories.PoliticaRetencionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RetencionService {

    private static final String ANONIMIZADO = "anonimizado";

    private final PoliticaRetencionRepository politicas;
    private final EjecucionRetencionRepository ejecuciones;
    private final IntercambioSunatRepository intercambios;

    @Value("${app.privacidad.retencion-habilitada:true}")
    private boolean habilitada;

    @PersistenceContext
    private EntityManager em;

    public List<PoliticaRetencionResponse> listar() {
        return politicas.findAllByOrderByNombreAsc().stream()
                .map(PoliticaRetencionResponse::de)
                .toList();
    }

    public List<EjecucionRetencionResponse> historial() {
        return ejecuciones.recientes(PageRequest.of(0, 60)).stream()
                .map(e -> new EjecucionRetencionResponse(
                        e.getId(), e.getPolitica().getNombre(), e.getEjecutadaEn(), e.getAfectados(), e.getDetalle()))
                .toList();
    }

    @Transactional
    public PoliticaRetencionResponse actualizar(UUID id, PoliticaRetencionRequest r) {
        PoliticaRetencion p = politicas
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política de retención", "id", id));
        p.setPlazoDias(r.plazoDias());
        p.setActiva(r.activa());
        return PoliticaRetencionResponse.de(p);
    }

    @Scheduled(cron = "${app.privacidad.cron-retencion:0 15 2 * * *}")
    public void programada() {
        if (!habilitada) {
            return;
        }
        try {
            ejecutarTodas();
        } catch (RuntimeException ex) {
            log.error("Falló la tarea de retención de datos", ex);
        }
    }

    @Transactional
    public List<EjecucionRetencionResponse> ejecutarTodas() {
        List<EjecucionRetencionResponse> resultado = new ArrayList<>();
        for (PoliticaRetencion p : politicas.findAllByOrderByNombreAsc()) {
            if (p.isActiva()) {
                resultado.add(ejecutar(p));
            }
        }
        return resultado;
    }

    @Transactional
    public EjecucionRetencionResponse ejecutarUna(UUID id) {
        PoliticaRetencion p = politicas
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Política de retención", "id", id));
        return ejecutar(p);
    }

    private EjecucionRetencionResponse ejecutar(PoliticaRetencion p) {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime corte = ahora.minusDays(p.getPlazoDias());
        int afectados;
        String detalle;
        switch (p.getTipoDato()) {
            case "ENTREGAS_NOTIFICACION" -> {
                afectados = em.createQuery(
                                "UPDATE EntregaNotificacion e SET e.destinoMascara = :a, e.idExterno = null, e.motivo = null"
                                        + " WHERE e.creadoEn < :corte AND e.destinoMascara <> :a")
                        .setParameter("a", ANONIMIZADO)
                        .setParameter("corte", corte)
                        .executeUpdate();
                detalle = "Entregas anteriores al " + corte.toLocalDate() + " anonimizadas";
            }
            case "SESIONES_DISPOSITIVO" -> {
                afectados = em.createQuery(
                                "DELETE FROM SesionUsuario s WHERE (s.revocada = true OR s.expiraEn < :ahora)"
                                        + " AND s.ultimoAcceso < :corte")
                        .setParameter("ahora", ahora)
                        .setParameter("corte", corte)
                        .executeUpdate();
                detalle = "Sesiones cerradas o vencidas antes del " + corte.toLocalDate() + " depuradas";
            }
            case "EVENTOS_SEGURIDAD" -> {
                afectados = em.createQuery("UPDATE EventoSeguridad e SET e.ipEnmascarada = null"
                                + " WHERE e.creadoEn < :corte AND e.ipEnmascarada IS NOT NULL")
                        .setParameter("corte", corte)
                        .executeUpdate();
                detalle = "Direcciones de red de eventos anteriores al " + corte.toLocalDate() + " anonimizados";
            }
            case "INTERCAMBIOS_SUNAT" -> {
                afectados = intercambios.depurarAnteriores(corte);
                detalle = "Intercambios con SUNAT anteriores al " + corte.toLocalDate() + " depurados";
            }
            case "HISTORIA_CLINICA" -> {
                Long candidatas = em.createQuery(
                                "SELECT COUNT(p) FROM Paciente p WHERE NOT EXISTS"
                                        + " (SELECT a.id FROM Atencion a WHERE a.paciente = p AND a.fecha >= :corte)"
                                        + " AND NOT EXISTS (SELECT c.id FROM Cita c WHERE c.paciente = p AND c.fechaHora >= :corte)",
                                Long.class)
                        .setParameter("corte", corte)
                        .getSingleResult();
                afectados = candidatas.intValue();
                detalle = "Historias inactivas desde el " + corte.toLocalDate()
                        + " marcadas para revisión. No se elimina nada de forma automática";
            }
            default -> {
                afectados = 0;
                detalle = "El tipo de dato no tiene un procesamiento definido";
            }
        }
        p.setUltimaEjecucion(ahora);
        EjecucionRetencion e = ejecuciones.save(EjecucionRetencion.builder()
                .politica(p)
                .ejecutadaEn(ahora)
                .afectados(afectados)
                .detalle(detalle)
                .build());
        log.info("Retención {}: {} registros. {}", p.getTipoDato(), afectados, detalle);
        return new EjecucionRetencionResponse(e.getId(), p.getNombre(), ahora, afectados, detalle);
    }
}
