package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EmpleadoConcepto;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmpleadoConceptoRepository extends JpaRepository<EmpleadoConcepto, UUID> {

    @Query("""
            SELECT ec FROM EmpleadoConcepto ec
            JOIN FETCH ec.concepto
            WHERE ec.empleado.id = :empleadoId
            ORDER BY ec.vigenteDesde DESC
            """)
    List<EmpleadoConcepto> deEmpleado(@Param("empleadoId") UUID empleadoId);

    @Query("""
            SELECT ec FROM EmpleadoConcepto ec
            JOIN FETCH ec.concepto c
            WHERE ec.empleado.id = :empleadoId AND ec.activo = true AND c.activo = true
              AND ec.vigenteDesde <= :hasta AND (ec.vigenteHasta IS NULL OR ec.vigenteHasta >= :desde)
            ORDER BY c.orden ASC
            """)
    List<EmpleadoConcepto> aplicables(
            @Param("empleadoId") UUID empleadoId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
