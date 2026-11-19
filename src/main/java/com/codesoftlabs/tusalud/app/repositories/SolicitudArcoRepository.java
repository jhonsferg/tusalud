package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SolicitudArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoArco;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SolicitudArcoRepository extends JpaRepository<SolicitudArco, UUID> {

    @Query("""
            SELECT s FROM SolicitudArco s
            JOIN FETCH s.usuario LEFT JOIN FETCH s.responsable LEFT JOIN FETCH s.finalidad
            WHERE s.id = :id
            """)
    Optional<SolicitudArco> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT s FROM SolicitudArco s
            JOIN FETCH s.usuario LEFT JOIN FETCH s.responsable
            WHERE s.usuario.id = :usuarioId ORDER BY s.recibidaEn DESC
            """)
    List<SolicitudArco> deUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT s FROM SolicitudArco s
            JOIN FETCH s.usuario LEFT JOIN FETCH s.responsable
            WHERE (:estado IS NULL OR s.estado = :estado) AND (:tipo IS NULL OR s.tipo = :tipo)
            ORDER BY s.venceEn ASC, s.recibidaEn ASC
            """)
    List<SolicitudArco> bandeja(@Param("estado") EstadoArco estado, @Param("tipo") TipoArco tipo, Pageable pageable);

    @Query("SELECT COUNT(s) FROM SolicitudArco s WHERE s.recibidaEn >= :desde AND s.recibidaEn < :hasta")
    long contarEntre(@Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta);

    @Query(
            "SELECT COUNT(s) FROM SolicitudArco s WHERE s.usuario.id = :usuarioId AND s.tipo = :tipo AND s.estado IN :estados")
    long abiertasDelUsuario(
            @Param("usuarioId") UUID usuarioId,
            @Param("tipo") TipoArco tipo,
            @Param("estados") java.util.Collection<EstadoArco> estados);
}
