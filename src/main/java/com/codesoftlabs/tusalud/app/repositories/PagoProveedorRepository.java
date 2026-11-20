package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PagoProveedor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, UUID> {

    @Query("""
            SELECT p FROM PagoProveedor p
            JOIN FETCH p.registradoPor
            WHERE p.comprobante.id = :comprobanteId
            ORDER BY p.registradoEn ASC, p.id ASC
            """)
    List<PagoProveedor> deComprobante(@Param("comprobanteId") UUID comprobanteId);
}
