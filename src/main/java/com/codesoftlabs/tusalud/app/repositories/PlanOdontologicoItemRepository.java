package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlanOdontologicoItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanOdontologicoItemRepository extends JpaRepository<PlanOdontologicoItem, UUID> {

    @Query("""
            SELECT i FROM PlanOdontologicoItem i JOIN FETCH i.procedimiento
            WHERE i.plan.id = :planId
            ORDER BY i.posicion
            """)
    List<PlanOdontologicoItem> delPlan(@Param("planId") UUID planId);
}
