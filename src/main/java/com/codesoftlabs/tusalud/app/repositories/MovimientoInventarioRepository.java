package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.MovimientoInventario;
import com.codesoftlabs.tusalud.app.entities.enums.MotivoMovimiento;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMovimiento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, UUID> {

    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.producto
            JOIN FETCH m.lote
            JOIN FETCH m.usuario
            WHERE (:productoId IS NULL OR m.producto.id = :productoId)
              AND (:tipo IS NULL OR m.tipo = :tipo)
              AND m.fecha >= :desde AND m.fecha < :hasta
            ORDER BY m.fecha DESC, m.id DESC
            """)
    List<MovimientoInventario> buscar(
            @Param("productoId") UUID productoId,
            @Param("tipo") TipoMovimiento tipo,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("""
            SELECT m FROM MovimientoInventario m
            JOIN FETCH m.lote
            JOIN FETCH m.producto
            WHERE m.referencia = :referencia AND m.tipo = :tipo AND m.motivo = :motivo
            ORDER BY m.fecha ASC
            """)
    List<MovimientoInventario> porReferencia(
            @Param("referencia") String referencia,
            @Param("tipo") TipoMovimiento tipo,
            @Param("motivo") MotivoMovimiento motivo);
}
