package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlanMedicion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanMedicionRepository extends JpaRepository<PlanMedicion, UUID> {

    @Query("SELECT m FROM PlanMedicion m WHERE m.plan.id = :planId ORDER BY m.numero ASC")
    List<PlanMedicion> historial(@Param("planId") UUID planId);

    @Query("SELECT COALESCE(MAX(m.numero), 0) FROM PlanMedicion m WHERE m.plan.id = :planId")
    int ultimoNumero(@Param("planId") UUID planId);
}
