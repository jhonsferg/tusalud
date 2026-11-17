package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Dispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoDispensacion;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DispensacionRepository extends JpaRepository<Dispensacion, UUID> {

    @Query("""
            SELECT d FROM Dispensacion d
            JOIN FETCH d.usuario
            LEFT JOIN FETCH d.paciente p
            LEFT JOIN FETCH p.usuario pu
            WHERE d.fecha >= :desde AND d.fecha < :hasta
              AND (:q IS NULL OR LOWER(d.comprobante) LIKE :q OR LOWER(COALESCE(pu.nombre, '')) LIKE :q
                   OR LOWER(COALESCE(pu.apellido, '')) LIKE :q OR COALESCE(p.dni, '') LIKE :q)
            ORDER BY d.fecha DESC, d.id DESC
            """)
    List<Dispensacion> buscar(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT d FROM Dispensacion d
            JOIN FETCH d.usuario
            LEFT JOIN FETCH d.paciente p
            LEFT JOIN FETCH p.usuario
            LEFT JOIN FETCH d.atencion
            WHERE d.id = :id
            """)
    Optional<Dispensacion> findByIdConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT d FROM Dispensacion d
            JOIN FETCH d.usuario
            JOIN FETCH d.paciente p
            JOIN FETCH p.usuario
            WHERE p.id = :pacienteId
            ORDER BY d.fecha DESC, d.id DESC
            """)
    List<Dispensacion> findByPacienteId(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT COALESCE(SUM(d.total), 0) FROM Dispensacion d
            WHERE d.estado = :estado AND d.fecha >= :desde AND d.fecha < :hasta
            """)
    BigDecimal sumaTotal(
            @Param("estado") EstadoDispensacion estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    long countByEstadoAndFechaGreaterThanEqualAndFechaLessThan(
            EstadoDispensacion estado, LocalDateTime desde, LocalDateTime hasta);
}
