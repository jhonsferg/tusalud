package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EsquemaVacunacion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EsquemaVacunacionRepository extends JpaRepository<EsquemaVacunacion, UUID> {

    @Query("""
            SELECT e FROM EsquemaVacunacion e
            WHERE e.activo = true AND e.vigenteDesde <= :hoy
              AND (e.vigenteHasta IS NULL OR e.vigenteHasta >= :hoy)
            ORDER BY e.vigenteDesde DESC
            """)
    List<EsquemaVacunacion> vigentes(@Param("hoy") LocalDate hoy);

    List<EsquemaVacunacion> findAllByOrderByVigenteDesdeDesc();
}
