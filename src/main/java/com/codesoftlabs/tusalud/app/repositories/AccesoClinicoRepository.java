package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Cita;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AccesoClinicoRepository extends Repository<Cita, UUID> {

    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.paciente.id = :pacienteId
              AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA
              AND c.fechaHora BETWEEN :desde AND :hasta
            """)
    boolean tieneCitaEntre(
            @Param("pacienteId") UUID pacienteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT COUNT(i) > 0 FROM IngresoHospitalario i
            WHERE i.paciente.id = :pacienteId
              AND i.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp.ACTIVO
            """)
    boolean tieneIngresoActivo(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT DISTINCT p FROM Paciente p JOIN FETCH p.usuario u
            WHERE p.id IN (
                SELECT c.paciente.id FROM Cita c
                WHERE c.medico.usuario.id = :usuarioId
                  AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA)
              AND (LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :q OR p.dni LIKE :q)
            ORDER BY u.apellido, u.nombre
            """)
    List<Paciente> pacientesDelMedico(@Param("usuarioId") UUID usuarioId, @Param("q") String q);

    @Query("""
            SELECT DISTINCT p FROM Paciente p JOIN FETCH p.usuario u
            WHERE (p.id IN (
                SELECT c.paciente.id FROM Cita c
                WHERE c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA
                  AND c.fechaHora BETWEEN :desde AND :hasta)
                OR p.id IN (
                SELECT i.paciente.id FROM IngresoHospitalario i
                WHERE i.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp.ACTIVO))
              AND (LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :q OR p.dni LIKE :q)
            ORDER BY u.apellido, u.nombre
            """)
    List<Paciente> pacientesEnAtencion(
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, @Param("q") String q);

    @Query("""
            SELECT p FROM Paciente p JOIN FETCH p.usuario
            WHERE p.fechaNacimiento IS NOT NULL AND p.fechaNacimiento >= :desde
            """)
    List<Paciente> menoresNacidosDesde(@Param("desde") java.time.LocalDate desde);
}
