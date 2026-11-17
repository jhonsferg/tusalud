package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Atencion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AtencionRepository extends JpaRepository<Atencion, Long> {

    boolean existsByCitaId(Long citaId);

    @Query("""
            SELECT a FROM Atencion a
            JOIN FETCH a.cita c
            JOIN FETCH a.medico m
            JOIN FETCH m.usuario
            JOIN FETCH c.especialidad
            JOIN FETCH a.paciente pa
            JOIN FETCH pa.usuario
            WHERE c.id = :citaId
            """)
    Optional<Atencion> findByCitaIdConDetalle(@Param("citaId") Long citaId);

    @Query("""
            SELECT a FROM Atencion a
            JOIN FETCH a.cita c
            JOIN FETCH a.medico m
            JOIN FETCH m.usuario
            JOIN FETCH c.especialidad
            JOIN FETCH a.paciente pa
            JOIN FETCH pa.usuario
            WHERE pa.id = :pacienteId
            ORDER BY a.fecha DESC, a.id DESC
            """)
    List<Atencion> findByPacienteIdConDetalle(@Param("pacienteId") Long pacienteId);
}
