package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.DevolucionProveedor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DevolucionProveedorRepository extends JpaRepository<DevolucionProveedor, UUID> {

    @Query("""
            SELECT DISTINCT d FROM DevolucionProveedor d
            JOIN FETCH d.proveedor
            JOIN FETCH d.recepcion r
            JOIN FETCH d.registradaPor
            LEFT JOIN FETCH d.items i
            LEFT JOIN FETCH i.lote l
            LEFT JOIN FETCH l.producto
            WHERE (:proveedorId IS NULL OR d.proveedor.id = :proveedorId)
              AND d.registradaEn >= :desde AND d.registradaEn < :hasta
            ORDER BY d.registradaEn DESC
            """)
    List<DevolucionProveedor> buscar(
            @Param("proveedorId") UUID proveedorId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);
}
