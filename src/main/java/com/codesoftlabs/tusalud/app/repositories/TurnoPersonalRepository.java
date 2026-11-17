package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.TurnoPersonal;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TurnoPersonalRepository extends JpaRepository<TurnoPersonal, UUID> {

    @Query("""
            SELECT t FROM TurnoPersonal t
            JOIN FETCH t.empleado e JOIN FETCH e.usuario JOIN FETCH t.plantilla
            WHERE t.fecha BETWEEN :desde AND :hasta
              AND (:area IS NULL OR e.area = :area)
              AND (:empleadoId IS NULL OR e.id = :empleadoId)
            ORDER BY t.fecha ASC, t.plantilla.horaInicio ASC
            """)
    List<TurnoPersonal> findEntre(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("area") AreaLaboral area,
            @Param("empleadoId") UUID empleadoId);

    @Query("""
            SELECT t FROM TurnoPersonal t
            JOIN FETCH t.empleado e JOIN FETCH e.usuario JOIN FETCH t.plantilla
            WHERE e.id = :empleadoId AND t.fecha BETWEEN :desde AND :hasta
            ORDER BY t.fecha ASC, t.plantilla.horaInicio ASC
            """)
    List<TurnoPersonal> findDeEmpleado(
            @Param("empleadoId") UUID empleadoId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT t FROM TurnoPersonal t
            JOIN FETCH t.empleado e JOIN FETCH e.usuario JOIN FETCH t.plantilla
            WHERE t.id = :id
            """)
    Optional<TurnoPersonal> findByIdConDetalle(@Param("id") UUID id);
}
