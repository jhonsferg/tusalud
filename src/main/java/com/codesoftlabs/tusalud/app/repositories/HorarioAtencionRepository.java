package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.HorarioAtencion;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface HorarioAtencionRepository extends JpaRepository<HorarioAtencion, Long> {

    @Query("""
            SELECT h FROM HorarioAtencion h
            WHERE h.medico.id = :medicoId AND h.diaSemana = :diaSemana
            ORDER BY h.horaInicio ASC
            """)
    List<HorarioAtencion> findByMedicoAndDia(@Param("medicoId") Long medicoId, @Param("diaSemana") DayOfWeek diaSemana);

    @Query("""
            SELECT h FROM HorarioAtencion h
            WHERE h.medico.id = :medicoId
            ORDER BY h.diaSemana ASC, h.horaInicio ASC
            """)
    List<HorarioAtencion> findByMedicoId(@Param("medicoId") Long medicoId);

    @Query("""
            SELECT COUNT(h) > 0 FROM HorarioAtencion h
            WHERE h.medico.id = :medicoId
              AND h.diaSemana = :diaSemana
              AND h.horaInicio < :horaFin
              AND h.horaFin > :horaInicio
            """)
    boolean existsSolapamiento(
            @Param("medicoId") Long medicoId,
            @Param("diaSemana") DayOfWeek diaSemana,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin);
}
