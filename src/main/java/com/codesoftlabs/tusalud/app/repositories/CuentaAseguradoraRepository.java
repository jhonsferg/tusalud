package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CuentaAseguradoraRepository extends JpaRepository<CuentaAseguradora, UUID> {

    String DETALLE = """
            SELECT c FROM CuentaAseguradora c
            JOIN FETCH c.aseguradora
            LEFT JOIN FETCH c.comprobante
            LEFT JOIN FETCH c.liquidacion
            """;

    @Query(DETALLE + " WHERE c.estado = :estado ORDER BY c.fecha ASC")
    List<CuentaAseguradora> porEstado(@Param("estado") EstadoCuentaAseguradora estado);

    @Query(
            DETALLE
                    + " WHERE c.aseguradora.id = :aseguradoraId AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora.REVERTIDA ORDER BY c.fecha DESC")
    List<CuentaAseguradora> deAseguradora(@Param("aseguradoraId") UUID aseguradoraId);

    @Query(DETALLE + " WHERE c.liquidacion.id = :liquidacionId ORDER BY c.fecha ASC")
    List<CuentaAseguradora> deLiquidacion(@Param("liquidacionId") UUID liquidacionId);

    @Query("""
            SELECT c FROM CuentaAseguradora c
            WHERE c.aseguradora.id = :aseguradoraId
              AND c.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora.PENDIENTE
              AND c.fecha >= :desde AND c.fecha < :hasta
            ORDER BY c.fecha ASC
            """)
    List<CuentaAseguradora> pendientesEnRango(
            @Param("aseguradoraId") UUID aseguradoraId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT COALESCE(SUM(c.montoCubierto), 0) FROM CuentaAseguradora c
            WHERE c.paciente.id = :pacienteId AND c.convenio.id = :convenioId
              AND c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora.REVERTIDA
              AND c.fecha >= :desde AND c.fecha < :hasta
            """)
    BigDecimal consumido(
            @Param("pacienteId") UUID pacienteId,
            @Param("convenioId") UUID convenioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query(DETALLE + " WHERE c.comprobante.id = :comprobanteId")
    Optional<CuentaAseguradora> findByComprobante(@Param("comprobanteId") UUID comprobanteId);

    @Query("""
            SELECT c FROM CuentaAseguradora c
            JOIN FETCH c.aseguradora
            WHERE c.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora.REVERTIDA
              AND c.fecha >= :desde AND c.fecha < :hasta
            """)
    List<CuentaAseguradora> vigentesEnRango(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
