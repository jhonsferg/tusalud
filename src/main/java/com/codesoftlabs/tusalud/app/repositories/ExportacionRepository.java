package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Exportacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExportacionRepository extends JpaRepository<Exportacion, UUID> {

    @Query("""
            SELECT e FROM Exportacion e
            LEFT JOIN FETCH e.archivo
            WHERE e.solicitadoPor.id = :usuarioId
            ORDER BY e.createdAt DESC
            """)
    List<Exportacion> findDeUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT e FROM Exportacion e
            LEFT JOIN FETCH e.archivo
            LEFT JOIN FETCH e.solicitadoPor
            WHERE e.id = :id
            """)
    Optional<Exportacion> findConArchivo(@Param("id") UUID id);

    long countBySolicitadoPorIdAndEstadoIn(UUID usuarioId, Collection<EstadoExportacion> estados);

    long countBySolicitadoPorIdAndCreatedAtAfter(UUID usuarioId, LocalDateTime desde);

    @Query("""
            SELECT e FROM Exportacion e
            LEFT JOIN FETCH e.archivo
            WHERE e.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion.LISTA
              AND e.expiraEn < :ahora
            """)
    List<Exportacion> findVencidas(@Param("ahora") LocalDateTime ahora);

    @Query("""
            SELECT e FROM Exportacion e
            WHERE e.estado IN (com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion.SOLICITADA,
                               com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion.PROCESANDO)
              AND e.createdAt < :limite
            """)
    List<Exportacion> findAtascadas(@Param("limite") LocalDateTime limite);
}
