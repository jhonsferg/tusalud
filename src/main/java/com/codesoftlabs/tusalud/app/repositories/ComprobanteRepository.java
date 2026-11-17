package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Comprobante;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoComprobante;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    @Query("SELECT COALESCE(MAX(c.numero), 0) FROM Comprobante c WHERE c.serie = :serie")
    int ultimoNumero(@Param("serie") String serie);

    boolean existsByOrigenAndReferenciaIdAndEstado(
            OrigenComprobante origen, Long referenciaId, EstadoComprobante estado);

    @Query("""
            SELECT c FROM Comprobante c
            JOIN FETCH c.caja ca JOIN FETCH ca.cajero
            LEFT JOIN FETCH c.paciente
            WHERE c.id = :id
            """)
    Optional<Comprobante> findByIdConDetalle(@Param("id") Long id);

    @Query("""
            SELECT c FROM Comprobante c
            JOIN FETCH c.caja ca JOIN FETCH ca.cajero
            WHERE c.origen = :origen AND c.referenciaId = :referenciaId AND c.estado = :estado
            """)
    Optional<Comprobante> findVigente(
            @Param("origen") OrigenComprobante origen,
            @Param("referenciaId") Long referenciaId,
            @Param("estado") EstadoComprobante estado);

    @Query("""
            SELECT c FROM Comprobante c
            JOIN FETCH c.caja ca JOIN FETCH ca.cajero
            WHERE c.fecha >= :desde AND c.fecha < :hasta
              AND (:estado IS NULL OR c.estado = :estado)
              AND (:q IS NULL OR LOWER(c.clienteNombre) LIKE :q OR COALESCE(c.clienteDoc, '') LIKE :q
                   OR LOWER(c.codigo) LIKE :q)
            ORDER BY c.fecha DESC, c.id DESC
            """)
    List<Comprobante> buscar(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estado") EstadoComprobante estado,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT c FROM Comprobante c
            WHERE c.caja.id = :cajaId AND c.estado = :estado
            """)
    List<Comprobante> findDeCaja(@Param("cajaId") Long cajaId, @Param("estado") EstadoComprobante estado);

    @Query("""
            SELECT c FROM Comprobante c
            WHERE c.fecha >= :desde AND c.fecha < :hasta AND c.estado = :estado
            ORDER BY c.fecha ASC
            """)
    List<Comprobante> findEntreFechas(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("estado") EstadoComprobante estado);
}
