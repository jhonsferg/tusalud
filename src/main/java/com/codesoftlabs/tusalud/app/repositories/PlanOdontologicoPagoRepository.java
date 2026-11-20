package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlanOdontologicoPago;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanOdontologicoPagoRepository extends JpaRepository<PlanOdontologicoPago, UUID> {

    @Query("""
            SELECT p FROM PlanOdontologicoPago p JOIN FETCH p.registradoPor
            WHERE p.plan.id = :planId
            ORDER BY p.registradoEn
            """)
    List<PlanOdontologicoPago> delPlan(@Param("planId") UUID planId);

    @Query("SELECT COALESCE(SUM(p.monto), 0) FROM PlanOdontologicoPago p WHERE p.plan.id = :planId")
    BigDecimal totalPagado(@Param("planId") UUID planId);
}
