package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ResumenSunat;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoResumen;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoResumen;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumenSunatRepository extends JpaRepository<ResumenSunat, UUID> {

    @Query("SELECT COUNT(r) FROM ResumenSunat r WHERE r.tipo = :tipo AND r.fechaGeneracion = :fecha")
    long contarDelDia(@Param("tipo") TipoResumen tipo, @Param("fecha") LocalDate fecha);

    @Query("""
            SELECT r.id FROM ResumenSunat r
            WHERE r.estado IN :estados AND r.proximoIntento IS NOT NULL AND r.proximoIntento <= :ahora
            ORDER BY r.proximoIntento ASC
            """)
    List<UUID> listos(
            @Param("estados") Collection<EstadoResumen> estados,
            @Param("ahora") LocalDateTime ahora,
            Pageable pageable);

    @Query("SELECT r FROM ResumenSunat r ORDER BY r.creadoEn DESC")
    List<ResumenSunat> recientes(Pageable pageable);

    boolean existsByTipoAndFechaReferenciaAndEstadoIn(
            TipoResumen tipo, LocalDate fechaReferencia, Collection<EstadoResumen> estados);
}
