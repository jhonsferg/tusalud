package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.RecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecetaElectronicaRepository extends JpaRepository<RecetaElectronica, UUID> {

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE r.id = :id
            """)
    Optional<RecetaElectronica> findDetalle(@Param("id") UUID id);

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE r.codigoVerificacion = :codigo
            """)
    Optional<RecetaElectronica> findByCodigo(@Param("codigo") String codigo);

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE r.numero = :numero
            """)
    Optional<RecetaElectronica> findByNumero(@Param("numero") String numero);

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE r.paciente.id = :pacienteId
            ORDER BY r.emitidaEn DESC, r.numero DESC
            """)
    List<RecetaElectronica> delPaciente(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE r.paciente.id = :pacienteId
              AND r.estado IN :estados
              AND r.vigenteHasta >= :ahora
            ORDER BY r.emitidaEn DESC
            """)
    List<RecetaElectronica> activasDelPaciente(
            @Param("pacienteId") UUID pacienteId,
            @Param("estados") Collection<EstadoRecetaElectronica> estados,
            @Param("ahora") LocalDateTime ahora);

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE r.atencion.id = :atencionId AND r.estado <> :excluido
            ORDER BY r.emitidaEn DESC
            """)
    List<RecetaElectronica> deLaAtencion(
            @Param("atencionId") UUID atencionId, @Param("excluido") EstadoRecetaElectronica excluido);

    boolean existsByAtencionIdAndEstadoNot(UUID atencionId, EstadoRecetaElectronica estado);

    List<RecetaElectronica> findByAtencionIdOrderByEmitidaEnDesc(UUID atencionId);

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE r.estado IN :estados AND r.vigenteHasta >= :ahora
              AND (:q IS NULL
                   OR LOWER(r.numero) LIKE :q
                   OR LOWER(p.dni) LIKE :q
                   OR LOWER(p.usuario.nombre) LIKE :q
                   OR LOWER(p.usuario.apellido) LIKE :q)
            ORDER BY r.emitidaEn DESC
            """)
    List<RecetaElectronica> paraDispensar(
            @Param("estados") Collection<EstadoRecetaElectronica> estados,
            @Param("ahora") LocalDateTime ahora,
            @Param("q") String q,
            Pageable pageable);

    @Query("""
            SELECT r FROM RecetaElectronica r
            JOIN FETCH r.paciente p JOIN FETCH p.usuario
            JOIN FETCH r.medico m JOIN FETCH m.usuario
            WHERE m.usuario.id = :usuarioId
            ORDER BY r.emitidaEn DESC, r.numero DESC
            """)
    List<RecetaElectronica> delMedico(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT MAX(r.emitidaEn) FROM RecetaElectronica r WHERE r.paciente.id = :pacienteId")
    Optional<LocalDateTime> ultimaEmision(@Param("pacienteId") UUID pacienteId);
}
