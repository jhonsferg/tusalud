package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ContratoLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoContrato;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContratoLaboralRepository extends JpaRepository<ContratoLaboral, UUID> {

    @Query("""
            SELECT c FROM ContratoLaboral c
            JOIN FETCH c.empleado e JOIN FETCH e.usuario
            WHERE c.id = :id
            """)
    Optional<ContratoLaboral> findByIdConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT c FROM ContratoLaboral c
            JOIN FETCH c.empleado e JOIN FETCH e.usuario
            WHERE (:empleadoId IS NULL OR e.id = :empleadoId)
              AND (:estado IS NULL OR c.estado = :estado)
            ORDER BY e.usuario.apellido ASC, e.usuario.nombre ASC, c.fechaInicio DESC
            """)
    List<ContratoLaboral> filtrar(@Param("empleadoId") UUID empleadoId, @Param("estado") EstadoContrato estado);

    @Query("""
            SELECT c FROM ContratoLaboral c
            JOIN FETCH c.empleado e JOIN FETCH e.usuario
            WHERE c.fechaInicio <= :hasta AND (c.fechaFin IS NULL OR c.fechaFin >= :desde)
            ORDER BY e.id ASC, c.fechaInicio DESC
            """)
    List<ContratoLaboral> solapados(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COUNT(c) FROM ContratoLaboral c
            WHERE c.empleado.id = :empleadoId
              AND c.fechaInicio <= :fin AND (c.fechaFin IS NULL OR c.fechaFin >= :inicio)
              AND (:excluir IS NULL OR c.id <> :excluir)
            """)
    long contarSolapados(
            @Param("empleadoId") UUID empleadoId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin,
            @Param("excluir") UUID excluir);
}
