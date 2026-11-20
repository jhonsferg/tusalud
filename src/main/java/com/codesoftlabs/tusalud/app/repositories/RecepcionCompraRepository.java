package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.RecepcionCompra;
import com.codesoftlabs.tusalud.app.entities.RecepcionItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecepcionCompraRepository extends JpaRepository<RecepcionCompra, UUID> {

    @Query("""
            SELECT r FROM RecepcionCompra r
            JOIN FETCH r.orden o
            JOIN FETCH o.proveedor
            JOIN FETCH r.recibidaPor
            WHERE (:ordenId IS NULL OR o.id = :ordenId)
              AND (:soloDiscrepancias = false OR r.conDiscrepancia = true)
              AND r.recibidaEn >= :desde AND r.recibidaEn < :hasta
            ORDER BY r.recibidaEn DESC, r.id DESC
            """)
    List<RecepcionCompra> buscar(
            @Param("ordenId") UUID ordenId,
            @Param("soloDiscrepancias") boolean soloDiscrepancias,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("""
            SELECT DISTINCT r FROM RecepcionCompra r
            JOIN FETCH r.orden o
            JOIN FETCH o.proveedor
            JOIN FETCH r.recibidaPor
            LEFT JOIN FETCH r.items i
            LEFT JOIN FETCH i.producto
            LEFT JOIN FETCH i.lote
            WHERE r.id = :id
            """)
    Optional<RecepcionCompra> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT i FROM RecepcionItem i
            JOIN FETCH i.recepcion r
            JOIN FETCH r.orden o
            JOIN FETCH i.producto
            JOIN FETCH i.lote
            WHERE i.id IN :ids
            """)
    List<RecepcionItem> itemsConLote(@Param("ids") List<UUID> ids);
}
