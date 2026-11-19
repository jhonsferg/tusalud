package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EntregaNotificacionRepository extends JpaRepository<EntregaNotificacion, UUID> {

    boolean existsByClaveIdempotencia(String claveIdempotencia);

    Optional<EntregaNotificacion> findByIdExterno(String idExterno);

    long countByUsuarioIdAndCanalAndTipoNotAndCreadoEnAfter(
            UUID usuarioId, Canal canal, TipoNotificacion tipo, LocalDateTime desde);

    @Query("""
            SELECT e.id FROM EntregaNotificacion e
            WHERE e.estado = :estado
              AND e.proximoIntento <= :ahora
            ORDER BY e.proximoIntento ASC
            """)
    List<UUID> idsVencidos(
            @Param("estado") EstadoEntrega estado, @Param("ahora") LocalDateTime ahora, Pageable pageable);

    @Query("""
            SELECT e FROM EntregaNotificacion e
            WHERE (:canal IS NULL OR e.canal = :canal)
              AND (:estado IS NULL OR e.estado = :estado)
              AND e.creadoEn >= :desde AND e.creadoEn < :hasta
            ORDER BY e.creadoEn DESC
            """)
    Page<EntregaNotificacion> buscar(
            @Param("canal") Canal canal,
            @Param("estado") EstadoEntrega estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable);

    @Query("""
            SELECT e.canal, e.estado, COUNT(e) FROM EntregaNotificacion e
            WHERE e.creadoEn >= :desde
            GROUP BY e.canal, e.estado
            """)
    List<Object[]> resumen(@Param("desde") LocalDateTime desde);
}
