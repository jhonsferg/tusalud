package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.DocumentoElectronico;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoElectronicoRepository extends JpaRepository<DocumentoElectronico, UUID> {

    @Query("""
            SELECT DISTINCT d FROM DocumentoElectronico d
            LEFT JOIN FETCH d.items
            LEFT JOIN FETCH d.documentoReferencia
            LEFT JOIN FETCH d.paciente p LEFT JOIN FETCH p.usuario
            WHERE d.id = :id
            """)
    Optional<DocumentoElectronico> findConDetalle(@Param("id") UUID id);

    @Query("SELECT d FROM DocumentoElectronico d WHERE d.comprobante.id = :comprobanteId")
    Optional<DocumentoElectronico> findDeComprobante(@Param("comprobanteId") UUID comprobanteId);

    @Query("""
            SELECT d FROM DocumentoElectronico d
            WHERE d.emitidoEn >= :desde AND d.emitidoEn < :hasta
              AND (:tipo IS NULL OR d.tipo = :tipo)
              AND (:estado IS NULL OR d.estado = :estado)
              AND (:q IS NULL OR LOWER(d.clienteNombre) LIKE :q OR COALESCE(d.clienteDoc, '') LIKE :q
                   OR LOWER(d.serie) LIKE :q)
            ORDER BY d.emitidoEn DESC, d.id DESC
            """)
    List<DocumentoElectronico> buscar(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("tipo") TipoCpe tipo,
            @Param("estado") EstadoCpe estado,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT d.id FROM DocumentoElectronico d
            WHERE d.estado = :estado AND d.proximoIntento IS NOT NULL AND d.proximoIntento <= :ahora
              AND d.resumen IS NULL
            ORDER BY d.proximoIntento ASC
            """)
    List<UUID> listosParaEnviar(
            @Param("estado") EstadoCpe estado, @Param("ahora") LocalDateTime ahora, Pageable pageable);

    @Query("""
            SELECT d FROM DocumentoElectronico d
            WHERE d.tipo = :tipo AND d.estado = :estado AND d.resumen IS NULL
              AND d.emitidoEn >= :desde AND d.emitidoEn < :hasta
            ORDER BY d.serie ASC, d.numero ASC
            """)
    List<DocumentoElectronico> paraResumen(
            @Param("tipo") TipoCpe tipo,
            @Param("estado") EstadoCpe estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("SELECT d FROM DocumentoElectronico d WHERE d.resumen.id = :resumenId OR d.bajaResumen.id = :resumenId")
    List<DocumentoElectronico> deResumen(@Param("resumenId") UUID resumenId);

    @Query("""
            SELECT COALESCE(SUM(n.total), 0) FROM DocumentoElectronico n
            WHERE n.documentoReferencia.id = :referenciaId AND n.tipo = :tipo
              AND n.estado IN :estados
            """)
    BigDecimal sumaNotas(
            @Param("referenciaId") UUID referenciaId,
            @Param("tipo") TipoCpe tipo,
            @Param("estados") Collection<EstadoCpe> estados);

    @Query("""
            SELECT d FROM DocumentoElectronico d
            WHERE d.emitidoEn >= :desde AND d.emitidoEn < :hasta AND d.estado IN :estados
            ORDER BY d.emitidoEn ASC, d.serie ASC, d.numero ASC
            """)
    List<DocumentoElectronico> paraRegistro(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estados") Collection<EstadoCpe> estados);

    @Query("""
            SELECT d FROM DocumentoElectronico d
            WHERE d.paciente.id = :pacienteId
            ORDER BY d.emitidoEn DESC
            """)
    List<DocumentoElectronico> delPaciente(@Param("pacienteId") UUID pacienteId, Pageable pageable);

    @Query("SELECT d.estado, COUNT(d) FROM DocumentoElectronico d WHERE d.emitidoEn >= :desde GROUP BY d.estado")
    List<Object[]> conteoPorEstado(@Param("desde") LocalDateTime desde);

    long countByTipo(TipoCpe tipo);
}
