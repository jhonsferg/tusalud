package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Requisicion;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RequisicionRepository extends JpaRepository<Requisicion, UUID> {

    @Query("""
            SELECT r FROM Requisicion r
            JOIN FETCH r.solicitante
            WHERE (:estado IS NULL OR r.estado = :estado)
              AND (:solicitanteId IS NULL OR r.solicitante.id = :solicitanteId)
              AND r.creadaEn >= :desde AND r.creadaEn < :hasta
            ORDER BY r.creadaEn DESC, r.id DESC
            """)
    List<Requisicion> buscar(
            @Param("estado") EstadoRequisicion estado,
            @Param("solicitanteId") UUID solicitanteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM Requisicion r
            JOIN FETCH r.solicitante
            LEFT JOIN FETCH r.items i
            LEFT JOIN FETCH i.producto
            WHERE r.id = :id
            """)
    Optional<Requisicion> findConDetalle(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Requisicion r WHERE r.id = :id")
    Optional<Requisicion> findParaActualizar(@Param("id") UUID id);

    @Query("""
            SELECT COUNT(r) FROM Requisicion r
            WHERE r.estado IN (com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion.ENVIADA,
                               com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoRequisicion.EN_COTIZACION)
            """)
    long contarAbiertas();
}
