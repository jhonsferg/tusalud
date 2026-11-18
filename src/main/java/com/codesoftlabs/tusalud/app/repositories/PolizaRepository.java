package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Poliza;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolizaRepository extends JpaRepository<Poliza, UUID> {

    @Query("""
            SELECT p FROM Poliza p
            JOIN FETCH p.convenio c
            JOIN FETCH c.aseguradora
            JOIN FETCH p.paciente pa
            JOIN FETCH pa.usuario u
            WHERE p.id = :id
            """)
    Optional<Poliza> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Poliza p
            JOIN FETCH p.convenio c
            JOIN FETCH c.aseguradora
            JOIN FETCH p.paciente pa
            JOIN FETCH pa.usuario u
            WHERE pa.id = :pacienteId
            ORDER BY p.vigenciaHasta DESC
            """)
    List<Poliza> deLPaciente(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT p FROM Poliza p
            JOIN FETCH p.convenio c
            JOIN FETCH c.aseguradora
            JOIN FETCH p.paciente pa
            JOIN FETCH pa.usuario u
            WHERE pa.usuario.id = :usuarioId
            ORDER BY p.vigenciaHasta DESC
            """)
    List<Poliza> deLUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT p FROM Poliza p
            JOIN FETCH p.convenio c
            JOIN FETCH c.aseguradora
            JOIN FETCH p.paciente pa
            JOIN FETCH pa.usuario u
            WHERE LOWER(p.numero) LIKE :q
               OR pa.dni LIKE :q
               OR LOWER(CONCAT(u.nombre, ' ', u.apellido)) LIKE :q
            ORDER BY p.creadaEn DESC
            """)
    List<Poliza> buscar(@Param("q") String q, Pageable pageable);

    @Query("""
            SELECT p FROM Poliza p
            JOIN FETCH p.convenio c
            JOIN FETCH c.aseguradora
            WHERE p.paciente.id = :pacienteId
              AND p.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza.ACTIVA
              AND p.vigenciaDesde <= :hoy AND p.vigenciaHasta >= :hoy
            ORDER BY p.vigenciaHasta DESC
            """)
    List<Poliza> usablesDelPaciente(@Param("pacienteId") UUID pacienteId, @Param("hoy") LocalDate hoy);

    @Query("""
            SELECT COUNT(p) FROM Poliza p
            WHERE p.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza.ACTIVA
              AND p.vigenciaHasta >= :desde AND p.vigenciaHasta <= :hasta
            """)
    long contarPorVencer(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    boolean existsByNumeroIgnoreCaseAndConvenioId(String numero, UUID convenioId);
}
