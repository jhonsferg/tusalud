package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlanOdontologico;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPlan;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanOdontologicoRepository extends JpaRepository<PlanOdontologico, UUID> {

    @Query("""
            SELECT p FROM PlanOdontologico p
            JOIN FETCH p.paciente pa JOIN FETCH pa.usuario
            JOIN FETCH p.medico m JOIN FETCH m.usuario
            WHERE p.id = :id
            """)
    Optional<PlanOdontologico> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT p FROM PlanOdontologico p
            JOIN FETCH p.medico m JOIN FETCH m.usuario
            WHERE p.paciente.id = :pacienteId
            ORDER BY p.creadoEn DESC
            """)
    List<PlanOdontologico> delPaciente(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT p FROM PlanOdontologico p
            JOIN FETCH p.paciente pa JOIN FETCH pa.usuario
            JOIN FETCH p.medico m JOIN FETCH m.usuario
            WHERE p.estado IN :estados
            ORDER BY p.creadoEn DESC
            """)
    List<PlanOdontologico> porEstados(@Param("estados") Collection<EstadoPlan> estados);
}
