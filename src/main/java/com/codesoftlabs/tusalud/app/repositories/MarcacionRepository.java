package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Marcacion;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoMarcacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MarcacionRepository extends JpaRepository<Marcacion, UUID> {

    @Query("""
            SELECT m FROM Marcacion m
            JOIN FETCH m.empleado e JOIN FETCH e.usuario
            WHERE (:empleadoId IS NULL OR e.id = :empleadoId)
              AND m.fechaHora >= :desde AND m.fechaHora < :hasta
            ORDER BY m.fechaHora ASC
            """)
    List<Marcacion> entre(
            @Param("empleadoId") UUID empleadoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT m FROM Marcacion m
            WHERE m.empleado.id = :empleadoId AND m.fechaHora >= :desde AND m.fechaHora < :hasta
            ORDER BY m.fechaHora DESC
            """)
    List<Marcacion> delDia(
            @Param("empleadoId") UUID empleadoId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    boolean existsByEmpleadoIdAndFechaHoraAndTipo(UUID empleadoId, LocalDateTime fechaHora, TipoMarcacion tipo);

    Optional<Marcacion> findFirstByEmpleadoIdOrderByFechaHoraDesc(UUID empleadoId);
}
