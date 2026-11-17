package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Ausencia;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia;
import com.codesoftlabs.tusalud.app.entities.enums.TipoAusencia;
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
public interface AusenciaRepository extends JpaRepository<Ausencia, UUID> {

    @Query("""
            SELECT a FROM Ausencia a
            JOIN FETCH a.empleado e JOIN FETCH e.usuario
            LEFT JOIN FETCH a.resueltoPor
            WHERE a.id = :id
            """)
    Optional<Ausencia> findByIdConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT a FROM Ausencia a
            JOIN FETCH a.empleado e JOIN FETCH e.usuario
            LEFT JOIN FETCH a.resueltoPor
            WHERE (:estado IS NULL OR a.estado = :estado)
              AND (:area IS NULL OR e.area = :area)
              AND a.fechaFin >= :desde AND a.fechaInicio <= :hasta
            ORDER BY a.fechaInicio ASC, a.createdAt ASC
            """)
    List<Ausencia> filtrar(
            @Param("estado") EstadoAusencia estado,
            @Param("area") AreaLaboral area,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT a FROM Ausencia a
            JOIN FETCH a.empleado e JOIN FETCH e.usuario
            LEFT JOIN FETCH a.resueltoPor
            WHERE e.id = :empleadoId
            ORDER BY a.fechaInicio DESC
            """)
    List<Ausencia> findDeEmpleado(@Param("empleadoId") UUID empleadoId);

    @Query("""
            SELECT COUNT(a) FROM Ausencia a
            WHERE a.empleado.id = :empleadoId AND a.estado IN :estados
              AND a.fechaFin >= :inicio AND a.fechaInicio <= :fin
            """)
    long contarSolapadas(
            @Param("empleadoId") UUID empleadoId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("estados") Collection<EstadoAusencia> estados);

    @Query("""
            SELECT COALESCE(SUM(a.dias), 0) FROM Ausencia a
            WHERE a.empleado.id = :empleadoId AND a.tipo = :tipo AND a.estado = :estado
              AND a.fechaInicio BETWEEN :desde AND :hasta
            """)
    long sumarDias(
            @Param("empleadoId") UUID empleadoId,
            @Param("tipo") TipoAusencia tipo,
            @Param("estado") EstadoAusencia estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT a FROM Ausencia a
            JOIN FETCH a.empleado e JOIN FETCH e.usuario
            WHERE a.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia.APROBADA AND a.fechaInicio <= :fecha AND a.fechaFin >= :fecha
            ORDER BY e.area ASC
            """)
    List<Ausencia> aprobadasEn(@Param("fecha") LocalDate fecha);

    @Query("""
            SELECT a FROM Ausencia a
            JOIN FETCH a.empleado e
            WHERE a.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoAusencia.APROBADA AND a.fechaFin >= :desde AND a.fechaInicio <= :hasta
            """)
    List<Ausencia> aprobadasEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    long countByEstado(EstadoAusencia estado);
}
