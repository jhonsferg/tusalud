package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Turno;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadTurno;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoRepository extends JpaRepository<Turno, UUID> {

    @Query("""
            SELECT t FROM Turno t
            JOIN FETCH t.paciente p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH t.medico m
            LEFT JOIN FETCH m.usuario
            WHERE t.fecha = :fecha
            ORDER BY t.creadoEn ASC
            """)
    List<Turno> findDelDia(@Param("fecha") LocalDate fecha);

    @Query("""
            SELECT t FROM Turno t
            JOIN FETCH t.paciente p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH t.medico m
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH t.cita
            WHERE t.id = :id
            """)
    Optional<Turno> findByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT t FROM Turno t JOIN FETCH t.cita c WHERE c.id IN :citaIds")
    List<Turno> findByCitaIds(@Param("citaIds") Collection<UUID> citaIds);

    @Query("SELECT COUNT(t) > 0 FROM Turno t WHERE t.cita.id = :citaId")
    boolean existsByCitaId(@Param("citaId") UUID citaId);

    @Query("""
            SELECT COUNT(t) > 0 FROM Turno t
            WHERE t.paciente.id = :pacienteId
              AND t.fecha = :fecha
              AND t.estado IN :estados
            """)
    boolean existsAbiertoDelPaciente(
            @Param("pacienteId") UUID pacienteId,
            @Param("fecha") LocalDate fecha,
            @Param("estados") Collection<EstadoTurno> estados);

    @Query("""
            SELECT COALESCE(MAX(t.secuencia), 0) FROM Turno t
            WHERE t.fecha = :fecha AND t.prioridad = :prioridad
            """)
    int maxSecuencia(@Param("fecha") LocalDate fecha, @Param("prioridad") PrioridadTurno prioridad);
}
