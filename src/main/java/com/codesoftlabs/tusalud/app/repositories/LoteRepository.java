package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Lote;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    @Query("""
            SELECT l.producto.id AS productoId, SUM(l.cantidadActual) AS stock, MIN(l.fechaVencimiento) AS proximo
            FROM Lote l
            WHERE l.producto.id IN :ids AND l.cantidadActual > 0 AND l.fechaVencimiento >= :hoy
            GROUP BY l.producto.id
            """)
    List<StockProyeccion> stockDe(@Param("ids") Collection<Long> ids, @Param("hoy") LocalDate hoy);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM Lote l
            WHERE l.producto.id = :productoId AND l.cantidadActual > 0 AND l.fechaVencimiento >= :hoy
            ORDER BY l.fechaVencimiento ASC, l.id ASC
            """)
    List<Lote> vigentesParaDescontar(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT l FROM Lote l
            WHERE l.producto.id = :productoId AND l.cantidadActual > 0 AND l.fechaVencimiento < :hoy
            ORDER BY l.fechaVencimiento ASC, l.id ASC
            """)
    List<Lote> vencidosParaDescontar(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    @Query("""
            SELECT l FROM Lote l
            JOIN FETCH l.producto
            LEFT JOIN FETCH l.proveedor
            WHERE l.producto.id = :productoId
            ORDER BY l.fechaVencimiento ASC, l.id ASC
            """)
    List<Lote> findByProductoIdConDetalle(@Param("productoId") Long productoId);

    @Query("""
            SELECT l FROM Lote l
            JOIN FETCH l.producto
            LEFT JOIN FETCH l.proveedor
            WHERE l.ingreso.id IN :ingresoIds
            ORDER BY l.id ASC
            """)
    List<Lote> findByIngresoIds(@Param("ingresoIds") Collection<Long> ingresoIds);

    @Query("""
            SELECT l FROM Lote l
            JOIN FETCH l.producto
            LEFT JOIN FETCH l.proveedor
            WHERE l.cantidadActual > 0 AND l.fechaVencimiento >= :hoy AND l.fechaVencimiento <= :limite
            ORDER BY l.fechaVencimiento ASC, l.id ASC
            """)
    List<Lote> porVencer(@Param("hoy") LocalDate hoy, @Param("limite") LocalDate limite);

    @Query("""
            SELECT l FROM Lote l
            JOIN FETCH l.producto
            LEFT JOIN FETCH l.proveedor
            WHERE l.cantidadActual > 0 AND l.fechaVencimiento < :hoy
            ORDER BY l.fechaVencimiento ASC, l.id ASC
            """)
    List<Lote> vencidosConStock(@Param("hoy") LocalDate hoy);

    @Query("""
            SELECT COALESCE(SUM(l.cantidadActual * l.costoUnitario), 0) FROM Lote l
            WHERE l.cantidadActual > 0 AND l.fechaVencimiento >= :hoy
            """)
    BigDecimal valorInventario(@Param("hoy") LocalDate hoy);

    boolean existsByProductoIdAndNumeroLote(Long productoId, String numeroLote);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Lote l JOIN FETCH l.producto WHERE l.id = :id")
    Optional<Lote> findByIdParaActualizar(@Param("id") Long id);
}
