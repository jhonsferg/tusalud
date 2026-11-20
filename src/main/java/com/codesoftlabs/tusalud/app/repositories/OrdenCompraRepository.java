package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.OrdenCompra;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrdenCompraRepository extends JpaRepository<OrdenCompra, UUID> {

    @Query("""
            SELECT o FROM OrdenCompra o
            JOIN FETCH o.proveedor
            JOIN FETCH o.creadaPor
            WHERE (:estado IS NULL OR o.estado = :estado)
              AND (:proveedorId IS NULL OR o.proveedor.id = :proveedorId)
              AND o.creadaEn >= :desde AND o.creadaEn < :hasta
            ORDER BY o.creadaEn DESC, o.id DESC
            """)
    List<OrdenCompra> buscar(
            @Param("estado") EstadoOrdenCompra estado,
            @Param("proveedorId") UUID proveedorId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM OrdenCompra o
            JOIN FETCH o.proveedor
            JOIN FETCH o.creadaPor
            LEFT JOIN FETCH o.aprobadaPor
            LEFT JOIN FETCH o.requisicion
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.producto
            WHERE o.id = :id
            """)
    Optional<OrdenCompra> findConDetalle(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM OrdenCompra o WHERE o.id = :id")
    Optional<OrdenCompra> findParaActualizar(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT o FROM OrdenCompra o
            JOIN FETCH o.proveedor
            LEFT JOIN FETCH o.items i
            WHERE o.estado IN :estados
              AND o.aprobadaEn >= :desde AND o.aprobadaEn < :hasta
              AND (:proveedorId IS NULL OR o.proveedor.id = :proveedorId)
            """)
    List<OrdenCompra> aprobadasEnPeriodo(
            @Param("estados") Collection<EstadoOrdenCompra> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("proveedorId") UUID proveedorId);

    List<OrdenCompra> findByRequisicionId(UUID requisicionId);

    long countByEstado(EstadoOrdenCompra estado);
}
