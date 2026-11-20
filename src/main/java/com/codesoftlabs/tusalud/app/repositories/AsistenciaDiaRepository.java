package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AsistenciaDia;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsistenciaDiaRepository extends JpaRepository<AsistenciaDia, UUID> {

    Optional<AsistenciaDia> findByEmpleadoIdAndFecha(UUID empleadoId, LocalDate fecha);

    @Query("""
            SELECT a FROM AsistenciaDia a
            JOIN FETCH a.empleado e JOIN FETCH e.usuario
            WHERE a.id = :id
            """)
    Optional<AsistenciaDia> findByIdConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT a FROM AsistenciaDia a
            JOIN FETCH a.empleado e JOIN FETCH e.usuario
            WHERE a.fecha BETWEEN :desde AND :hasta
              AND (:empleadoId IS NULL OR e.id = :empleadoId)
            ORDER BY a.fecha ASC, e.usuario.apellido ASC, e.usuario.nombre ASC
            """)
    List<AsistenciaDia> entre(
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("empleadoId") UUID empleadoId);
}
