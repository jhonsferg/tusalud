package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Asiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AsientoRepository extends JpaRepository<Asiento, UUID> {

    interface SaldoCuenta {
        UUID getCuentaId();

        BigDecimal getDebe();

        BigDecimal getHaber();
    }

    @Query("""
            SELECT DISTINCT a FROM Asiento a
            JOIN FETCH a.periodo
            LEFT JOIN FETCH a.lineas l
            LEFT JOIN FETCH l.cuenta
            LEFT JOIN FETCH a.creadoPor
            LEFT JOIN FETCH a.contabilizadoPor
            LEFT JOIN FETCH a.revierteA
            WHERE a.id = :id
            """)
    Optional<Asiento> conDetalle(@Param("id") UUID id);

    Optional<Asiento> findByOrigenAndReferenciaId(OrigenAsiento origen, UUID referenciaId);

    boolean existsByOrigenAndReferenciaId(OrigenAsiento origen, UUID referenciaId);

    Optional<Asiento> findByRevierteAId(UUID revierteAId);

    @Query("""
            SELECT a FROM Asiento a
            JOIN FETCH a.periodo p
            WHERE (:periodoId IS NULL OR p.id = :periodoId)
              AND (:estado IS NULL OR a.estado = :estado)
              AND (:origen IS NULL OR a.origen = :origen)
              AND (:desde IS NULL OR a.fecha >= :desde)
              AND (:hasta IS NULL OR a.fecha <= :hasta)
              AND (:q IS NULL OR LOWER(a.glosa) LIKE :q
                   OR LOWER(COALESCE(a.numero, '')) LIKE :q)
            ORDER BY a.fecha DESC, a.creadoEn DESC, a.id DESC
            """)
    List<Asiento> filtrar(
            @Param("periodoId") UUID periodoId,
            @Param("estado") EstadoAsiento estado,
            @Param("origen") OrigenAsiento origen,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT l.cuenta.id AS cuentaId, COALESCE(SUM(l.debe), 0) AS debe, COALESCE(SUM(l.haber), 0) AS haber
            FROM AsientoLinea l
            WHERE l.asiento.estado <> com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento.BORRADOR
              AND l.asiento.fecha BETWEEN :desde AND :hasta
            GROUP BY l.cuenta.id
            """)
    List<SaldoCuenta> saldosEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT l.cuenta.id AS cuentaId, COALESCE(SUM(l.debe), 0) AS debe, COALESCE(SUM(l.haber), 0) AS haber
            FROM AsientoLinea l
            WHERE l.asiento.estado <> com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento.BORRADOR
              AND l.asiento.fecha < :antes
            GROUP BY l.cuenta.id
            """)
    List<SaldoCuenta> saldosAntesDe(@Param("antes") LocalDate antes);

    @Query("""
            SELECT l FROM AsientoLinea l
            JOIN FETCH l.asiento a
            WHERE l.cuenta.id = :cuentaId
              AND a.estado <> com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento.BORRADOR
              AND a.fecha BETWEEN :desde AND :hasta
            ORDER BY a.fecha ASC, a.numero ASC, l.posicion ASC
            """)
    List<com.codesoftlabs.tusalud.app.entities.AsientoLinea> movimientosDeCuenta(
            @Param("cuentaId") UUID cuentaId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT DISTINCT a FROM Asiento a
            LEFT JOIN FETCH a.lineas l
            LEFT JOIN FETCH l.cuenta
            WHERE a.estado <> com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento.BORRADOR
              AND a.fecha BETWEEN :desde AND :hasta
            ORDER BY a.fecha ASC, a.numero ASC
            """)
    List<Asiento> contabilizadosEntre(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COUNT(a) FROM Asiento a
            WHERE a.periodo.id = :periodoId
              AND a.estado = com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento.BORRADOR
            """)
    long contarBorradores(@Param("periodoId") UUID periodoId);

    @Query("""
            SELECT COUNT(a) FROM Asiento a
            WHERE a.periodo.id = :periodoId
              AND (a.creadoPor.id = :usuarioId OR a.contabilizadoPor.id = :usuarioId)
            """)
    long contarRegistradosPor(@Param("periodoId") UUID periodoId, @Param("usuarioId") UUID usuarioId);

    long countByPeriodoIdAndEstadoNot(UUID periodoId, EstadoAsiento estado);
}
