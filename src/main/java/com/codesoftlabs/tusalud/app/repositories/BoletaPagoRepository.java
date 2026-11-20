package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.BoletaPago;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoletaPagoRepository extends JpaRepository<BoletaPago, UUID> {

    @Query("""
            SELECT b FROM BoletaPago b
            JOIN FETCH b.empleado e JOIN FETCH e.usuario JOIN FETCH b.planilla
            WHERE b.planilla.id = :planillaId
            ORDER BY b.empleadoNombre ASC
            """)
    List<BoletaPago> dePlanilla(@Param("planillaId") UUID planillaId);

    @Query("""
            SELECT b FROM BoletaPago b
            JOIN FETCH b.empleado e JOIN FETCH e.usuario JOIN FETCH b.planilla
            LEFT JOIN FETCH b.lineas
            WHERE b.id = :id
            """)
    Optional<BoletaPago> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT b FROM BoletaPago b
            JOIN FETCH b.planilla p
            WHERE b.empleado.id = :empleadoId AND p.estado IN :estados
            ORDER BY p.anio DESC, p.mes DESC
            """)
    List<BoletaPago> deEmpleado(
            @Param("empleadoId") UUID empleadoId, @Param("estados") Collection<EstadoPlanilla> estados);

    boolean existsByContratoIdAndPlanillaEstadoIn(UUID contratoId, Collection<EstadoPlanilla> estados);

    boolean existsByEmpleadoIdAndPlanillaEstadoIn(UUID empleadoId, Collection<EstadoPlanilla> estados);
}
