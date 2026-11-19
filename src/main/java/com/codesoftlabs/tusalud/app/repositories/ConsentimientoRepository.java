package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, UUID> {

    @Query("""
            SELECT c FROM Consentimiento c
            JOIN FETCH c.plantilla JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            WHERE c.id = :id
            """)
    Optional<Consentimiento> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT c FROM Consentimiento c
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            WHERE c.codigoVerificacion = :codigo
            """)
    Optional<Consentimiento> findByCodigo(@Param("codigo") String codigo);

    @Query("""
            SELECT c FROM Consentimiento c JOIN FETCH c.medico m JOIN FETCH m.usuario
            WHERE c.paciente.id = :pacienteId ORDER BY c.creadoEn DESC
            """)
    List<Consentimiento> findDelPaciente(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT c FROM Consentimiento c JOIN FETCH c.medico m JOIN FETCH m.usuario
            WHERE c.ingreso.id = :ingresoId ORDER BY c.creadoEn DESC
            """)
    List<Consentimiento> findDelIngreso(@Param("ingresoId") UUID ingresoId);
}
