package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.RegistroVacunacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RegistroVacunacionRepository extends JpaRepository<RegistroVacunacion, UUID> {

    @Query("""
            SELECT r FROM RegistroVacunacion r
            JOIN FETCH r.vacuna JOIN FETCH r.responsable
            WHERE r.paciente.id = :pacienteId
            ORDER BY r.fechaAplicacion DESC, r.creadoEn DESC
            """)
    List<RegistroVacunacion> delPaciente(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT r FROM RegistroVacunacion r
            JOIN FETCH r.vacuna JOIN FETCH r.responsable
            WHERE r.id = :id
            """)
    Optional<RegistroVacunacion> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT COUNT(r) > 0 FROM RegistroVacunacion r
            WHERE r.paciente.id = :pacienteId AND r.vacuna.id = :vacunaId AND r.numeroDosis = :dosis
              AND r.estado = com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoRegistroVacunacion.VIGENTE
            """)
    boolean existeVigente(
            @Param("pacienteId") UUID pacienteId, @Param("vacunaId") UUID vacunaId, @Param("dosis") int dosis);
}
