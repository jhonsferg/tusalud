package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SesionOdontologica;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SesionOdontologicaRepository extends JpaRepository<SesionOdontologica, UUID> {

    @Query("""
            SELECT s FROM SesionOdontologica s JOIN FETCH s.medico m JOIN FETCH m.usuario
            WHERE s.plan.id = :planId
            ORDER BY s.fecha
            """)
    List<SesionOdontologica> delPlan(@Param("planId") UUID planId);
}
