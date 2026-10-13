package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.dto.response.CitasPorEstadoResponse;
import com.codesoftlabs.tusalud.app.entities.Cita;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.medico.id = :medicoId
              AND c.fechaHora = :fechaHora
              AND c.estado NOT IN (com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA)
            """)
    boolean existsSolapamiento(@Param("medicoId") Long medicoId, @Param("fechaHora") LocalDateTime fechaHora);

    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.medico.id = :medicoId
              AND c.fechaHora > :desde
              AND c.fechaHora < :hasta
              AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA
            """)
    boolean existsCitaMedicoEnFranja(
            @Param("medicoId") Long medicoId, @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT COUNT(c) > 0 FROM Cita c
            WHERE c.paciente.id = :pacienteId
              AND c.fechaHora > :desde
              AND c.fechaHora < :hasta
              AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA
            """)
    boolean existsCitaPacienteEnFranja(
            @Param("pacienteId") Long pacienteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.medico m
            JOIN FETCH m.usuario
            JOIN FETCH m.especialidad
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario
            WHERE c.paciente.id = :pacienteId
              AND c.fechaHora BETWEEN :inicio AND :fin
            ORDER BY c.fechaHora ASC
            """)
    List<Cita> findByPacienteAndRango(
            @Param("pacienteId") Long pacienteId,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH c.medico m
            JOIN FETCH m.usuario
            JOIN FETCH m.especialidad
            WHERE m.id = :medicoId
              AND c.fechaHora >= :inicio
              AND c.fechaHora < :fin
            ORDER BY c.fechaHora ASC
            """)
    List<Cita> findAgendaMedico(
            @Param("medicoId") Long medicoId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    default List<Cita> findAgendaMedicoPorFecha(Long medicoId, LocalDate fecha) {
        return findAgendaMedico(
                medicoId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }

    @Query("""
            SELECT c.fechaHora FROM Cita c
            WHERE c.medico.id = :medicoId
              AND c.fechaHora >= :inicio
              AND c.fechaHora < :fin
              AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CANCELADA
            """)
    List<LocalDateTime> findHorasOcupadas(
            @Param("medicoId") Long medicoId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.medico m
            JOIN FETCH m.usuario
            JOIN FETCH m.especialidad
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario
            WHERE c.id = :id
            """)
    Optional<Cita> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT new com.codesoftlabs.tusalud.app.dto.response.CitasPorEstadoResponse(c.estado, COUNT(c))
            FROM Cita c
            GROUP BY c.estado
            ORDER BY c.estado
            """)
    List<CitasPorEstadoResponse> contarPorEstado();

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.medico m
            JOIN FETCH m.usuario
            JOIN FETCH m.especialidad
            JOIN FETCH c.paciente p
            JOIN FETCH p.usuario
            WHERE c.fechaHora BETWEEN :inicio AND :fin
            ORDER BY c.fechaHora ASC
            """)
    List<Cita> findAllByRango(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
