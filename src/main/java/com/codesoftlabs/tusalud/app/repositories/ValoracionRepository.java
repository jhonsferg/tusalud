package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Valoracion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ValoracionRepository extends JpaRepository<Valoracion, UUID> {

    boolean existsByCitaId(UUID citaId);

    @Query("""
            SELECT v FROM Valoracion v
            JOIN FETCH v.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH v.cita c
            WHERE c.id = :citaId
            """)
    Optional<Valoracion> findByCitaIdConDetalle(@Param("citaId") UUID citaId);

    @Query("""
            SELECT v FROM Valoracion v
            JOIN FETCH v.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH v.cita c
            WHERE v.medico.id = :medicoId
            ORDER BY v.createdAt DESC, v.id DESC
            """)
    List<Valoracion> findByMedicoIdConDetalle(@Param("medicoId") UUID medicoId);
}
