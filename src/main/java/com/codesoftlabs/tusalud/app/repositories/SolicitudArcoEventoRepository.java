package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SolicitudArcoEvento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudArcoEventoRepository extends JpaRepository<SolicitudArcoEvento, UUID> {

    @Query("""
            SELECT e FROM SolicitudArcoEvento e LEFT JOIN FETCH e.actor
            WHERE e.solicitud.id = :solicitudId ORDER BY e.registradoEn ASC, e.id ASC
            """)
    List<SolicitudArcoEvento> deSolicitud(@Param("solicitudId") UUID solicitudId);
}
