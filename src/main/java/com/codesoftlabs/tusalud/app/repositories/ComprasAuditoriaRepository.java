package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ComprasAuditoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprasAuditoriaRepository extends JpaRepository<ComprasAuditoria, UUID> {

    @Query("""
            SELECT a FROM ComprasAuditoria a
            JOIN FETCH a.usuario
            WHERE a.entidadId = :entidadId
            ORDER BY a.fecha ASC, a.id ASC
            """)
    List<ComprasAuditoria> deEntidad(@Param("entidadId") UUID entidadId);

    @Query("""
            SELECT a FROM ComprasAuditoria a
            JOIN FETCH a.usuario
            WHERE (:entidad IS NULL OR a.entidad = :entidad)
            ORDER BY a.fecha DESC, a.id DESC
            """)
    List<ComprasAuditoria> recientes(@Param("entidad") String entidad, Pageable pageable);
}
