package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Teleconsulta;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTeleconsulta;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TeleconsultaRepository extends JpaRepository<Teleconsulta, UUID> {

    @Query("""
            SELECT t FROM Teleconsulta t
            JOIN FETCH t.cita c
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.especialidad
            WHERE t.id = :id
            """)
    Optional<Teleconsulta> findDetalle(@Param("id") UUID id);

    @Query("""
            SELECT t FROM Teleconsulta t
            JOIN FETCH t.cita c
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.especialidad
            WHERE p.usuario.id = :usuarioId
            ORDER BY c.fechaHora DESC
            """)
    List<Teleconsulta> findDelPaciente(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT t FROM Teleconsulta t
            JOIN FETCH t.cita c
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.especialidad
            WHERE m.usuario.id = :usuarioId AND c.fechaHora >= :desde
            ORDER BY c.fechaHora ASC
            """)
    List<Teleconsulta> findDelMedico(@Param("usuarioId") UUID usuarioId, @Param("desde") LocalDateTime desde);

    @Query("""
            SELECT t FROM Teleconsulta t
            JOIN FETCH t.cita c
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.especialidad
            WHERE t.estado IN :estados AND c.fechaHora BETWEEN :desde AND :hasta AND t.recordatorioEnviado = false
              AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA
            """)
    List<Teleconsulta> findParaRecordar(
            @Param("estados") Collection<EstadoTeleconsulta> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT t FROM Teleconsulta t
            JOIN FETCH t.cita c
            WHERE t.estado IN :estados AND c.fechaHora < :limite
            """)
    List<Teleconsulta> findVencidas(
            @Param("estados") Collection<EstadoTeleconsulta> estados, @Param("limite") LocalDateTime limite);

    Optional<Teleconsulta> findByCitaId(UUID citaId);
}
