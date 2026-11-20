package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.TipoComprobanteProveedor;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteProveedorRepository extends JpaRepository<ComprobanteProveedor, UUID> {

    @Query("""
            SELECT c FROM ComprobanteProveedor c
            JOIN FETCH c.proveedor
            LEFT JOIN FETCH c.orden
            WHERE (:estado IS NULL OR c.estado = :estado)
              AND (:proveedorId IS NULL OR c.proveedor.id = :proveedorId)
              AND c.fechaEmision >= :desde AND c.fechaEmision <= :hasta
              AND (:soloVencidos = false OR (c.fechaVencimiento < :hoy
                   AND c.estado IN (com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor.PENDIENTE,
                                    com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor.PARCIAL)))
            ORDER BY c.fechaVencimiento ASC, c.id ASC
            """)
    List<ComprobanteProveedor> buscar(
            @Param("estado") EstadoComprobanteProveedor estado,
            @Param("proveedorId") UUID proveedorId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("soloVencidos") boolean soloVencidos,
            @Param("hoy") LocalDate hoy,
            Pageable pageable);

    @Query("""
            SELECT c FROM ComprobanteProveedor c
            JOIN FETCH c.proveedor
            LEFT JOIN FETCH c.orden
            WHERE c.id = :id
            """)
    Optional<ComprobanteProveedor> findConDetalle(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM ComprobanteProveedor c WHERE c.id = :id")
    Optional<ComprobanteProveedor> findParaActualizar(@Param("id") UUID id);

    @Query("""
            SELECT c FROM ComprobanteProveedor c
            JOIN FETCH c.proveedor
            WHERE c.estado IN :estados
            """)
    List<ComprobanteProveedor> deEstados(@Param("estados") Collection<EstadoComprobanteProveedor> estados);

    @Query("""
            SELECT c FROM ComprobanteProveedor c
            JOIN FETCH c.proveedor
            WHERE c.estado <> com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor.ANULADO
              AND c.fechaEmision >= :desde AND c.fechaEmision <= :hasta
              AND (:proveedorId IS NULL OR c.proveedor.id = :proveedorId)
            """)
    List<ComprobanteProveedor> emitidosEnPeriodo(
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("proveedorId") UUID proveedorId);

    boolean existsByProveedorIdAndTipoAndSerieAndNumero(
            UUID proveedorId, TipoComprobanteProveedor tipo, String serie, String numero);

    @Query("""
            SELECT COALESCE(SUM(c.total), 0) FROM ComprobanteProveedor c
            WHERE c.orden.id = :ordenId
              AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor.ANULADO
            """)
    java.math.BigDecimal totalFacturadoDeOrden(@Param("ordenId") UUID ordenId);
}
