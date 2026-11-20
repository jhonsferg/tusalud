package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlanControl;
import com.codesoftlabs.tusalud.app.entities.PlanTratamiento;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanTratamientoRepository extends JpaRepository<PlanTratamiento, UUID> {

    String BASE = """
            SELECT p FROM PlanTratamiento p
            JOIN FETCH p.paciente pa JOIN FETCH pa.usuario
            JOIN FETCH p.medico m JOIN FETCH m.usuario
            """;

    @Query(BASE + " WHERE p.id = :id")
    Optional<PlanTratamiento> detalle(@Param("id") UUID id);

    @Query(BASE + " WHERE pa.usuario.id = :usuarioId ORDER BY p.inicio DESC, p.id DESC")
    List<PlanTratamiento> delPaciente(@Param("usuarioId") UUID usuarioId);

    @Query(BASE + " WHERE pa.id = :pacienteId ORDER BY p.inicio DESC, p.id DESC")
    List<PlanTratamiento> porPacienteId(@Param("pacienteId") UUID pacienteId);

    @Query(BASE + " WHERE m.usuario.id = :usuarioId AND p.estado = :activo ORDER BY p.inicio DESC, p.id DESC")
    List<PlanTratamiento> activosDelMedico(
            @Param("usuarioId") UUID usuarioId,
            @Param("activo") com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.EstadoPlan activo);

    @Query("""
            SELECT c FROM PlanControl c
            JOIN FETCH c.plan p JOIN FETCH p.paciente pa JOIN FETCH pa.usuario
            JOIN FETCH p.medico m JOIN FETCH m.usuario
            WHERE c.estado = :pendiente
              AND c.fechaProgramada < :hoy AND c.alertadoEn IS NULL
              AND p.estado = :activo
            """)
    List<PlanControl> controlesVencidosSinAlerta(
            @Param("hoy") LocalDate hoy,
            @Param("pendiente") com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.EstadoControl pendiente,
            @Param("activo") com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.EstadoPlan activo);
}
