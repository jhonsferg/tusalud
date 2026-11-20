package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Cotizacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotizacionRepository extends JpaRepository<Cotizacion, UUID> {

    @Query("""
            SELECT DISTINCT c FROM Cotizacion c
            JOIN FETCH c.proveedor
            JOIN FETCH c.registradaPor
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.producto
            WHERE c.requisicion.id = :requisicionId
            ORDER BY c.creadaEn ASC
            """)
    List<Cotizacion> deRequisicion(@Param("requisicionId") UUID requisicionId);

    @Query("""
            SELECT DISTINCT c FROM Cotizacion c
            JOIN FETCH c.proveedor
            JOIN FETCH c.requisicion
            LEFT JOIN FETCH c.items i
            LEFT JOIN FETCH i.producto
            WHERE c.id = :id
            """)
    Optional<Cotizacion> findConDetalle(@Param("id") UUID id);

    boolean existsByRequisicionIdAndProveedorId(UUID requisicionId, UUID proveedorId);
}
