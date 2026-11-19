package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SerieComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SerieComprobanteRepository extends JpaRepository<SerieComprobante, UUID> {

    List<SerieComprobante> findBySedeOrderBySerieAsc(String sede);

    Optional<SerieComprobante> findBySedeAndSerie(String sede, String serie);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SerieComprobante s WHERE s.sede = :sede AND s.serie = :serie")
    Optional<SerieComprobante> bloquear(@Param("sede") String sede, @Param("serie") String serie);

    @Query("""
            SELECT s FROM SerieComprobante s
            WHERE s.sede = :sede AND s.tipo = :tipo AND s.activa = true AND s.serie LIKE :prefijo
            ORDER BY s.serie ASC
            """)
    List<SerieComprobante> activas(
            @Param("sede") String sede, @Param("tipo") TipoCpe tipo, @Param("prefijo") String prefijo);
}
