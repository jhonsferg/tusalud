package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.MovimientoTesoreria;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenMovimiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoTesoreriaRepository extends JpaRepository<MovimientoTesoreria, UUID> {

    Optional<MovimientoTesoreria> findByOrigenAndReferenciaId(OrigenMovimiento origen, UUID referenciaId);

    @Query("""
            SELECT m FROM MovimientoTesoreria m
            JOIN FETCH m.cuenta c
            WHERE (:cuentaId IS NULL OR c.id = :cuentaId)
              AND (:desde IS NULL OR m.fecha >= :desde)
              AND (:hasta IS NULL OR m.fecha <= :hasta)
              AND (:conciliado IS NULL OR m.conciliado = :conciliado)
            ORDER BY m.fecha DESC, m.registradoEn DESC, m.id DESC
            """)
    List<MovimientoTesoreria> filtrar(
            @Param("cuentaId") UUID cuentaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("conciliado") Boolean conciliado,
            Pageable pageable);

    @Query("""
            SELECT COALESCE(SUM(CASE WHEN m.tipo = com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.TipoMovimientoTesoreria.INGRESO
                                     THEN m.monto ELSE -m.monto END), 0)
            FROM MovimientoTesoreria m
            WHERE m.cuenta.id = :cuentaId AND m.fecha <= :hasta
            """)
    BigDecimal saldoMovimientos(@Param("cuentaId") UUID cuentaId, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT m FROM MovimientoTesoreria m
            WHERE m.cuenta.id = :cuentaId AND m.conciliado = false
              AND m.fecha BETWEEN :desde AND :hasta
            ORDER BY m.fecha ASC, m.id ASC
            """)
    List<MovimientoTesoreria> sinConciliar(
            @Param("cuentaId") UUID cuentaId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
