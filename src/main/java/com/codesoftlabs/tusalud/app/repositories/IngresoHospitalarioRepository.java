package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.IngresoHospitalario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.TipoIngresoHosp;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IngresoHospitalarioRepository extends JpaRepository<IngresoHospitalario, Long> {

    @Query("""
            SELECT i FROM IngresoHospitalario i
            JOIN FETCH i.paciente p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH i.cama
            LEFT JOIN FETCH i.medico m
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH i.triajeRegistradoPor
            WHERE i.id = :id
            """)
    Optional<IngresoHospitalario> findByIdConDetalle(@Param("id") Long id);

    @Query("""
            SELECT i FROM IngresoHospitalario i
            JOIN FETCH i.paciente p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH i.cama
            LEFT JOIN FETCH i.medico m
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH i.triajeRegistradoPor
            ORDER BY i.fechaIngreso DESC
            """)
    List<IngresoHospitalario> findRecientes(Pageable pageable);

    @Query("""
            SELECT i FROM IngresoHospitalario i
            JOIN FETCH i.paciente p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH i.cama
            LEFT JOIN FETCH i.medico m
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH i.triajeRegistradoPor
            WHERE i.estado = :estado
            ORDER BY i.fechaIngreso ASC
            """)
    List<IngresoHospitalario> findPorEstado(@Param("estado") EstadoIngresoHosp estado);

    @Query("""
            SELECT i FROM IngresoHospitalario i
            JOIN FETCH i.paciente p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH i.cama
            LEFT JOIN FETCH i.medico m
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH i.triajeRegistradoPor
            WHERE p.usuario.id = :usuarioId
            ORDER BY i.fechaIngreso DESC
            """)
    List<IngresoHospitalario> findDelUsuarioPaciente(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT i FROM IngresoHospitalario i
            JOIN FETCH i.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH i.cama c
            WHERE i.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp.ACTIVO
            """)
    List<IngresoHospitalario> findActivosConCama();

    @Query("""
            SELECT i FROM IngresoHospitalario i
            JOIN FETCH i.paciente p
            JOIN FETCH p.usuario
            LEFT JOIN FETCH i.cama
            LEFT JOIN FETCH i.medico m
            LEFT JOIN FETCH m.usuario
            LEFT JOIN FETCH i.triajeRegistradoPor
            WHERE i.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp.ACTIVO
              AND i.tipo = com.codesoftlabs.tusalud.app.entities.enums.TipoIngresoHosp.EMERGENCIA
            """)
    List<IngresoHospitalario> findEmergenciasActivas();

    boolean existsByPacienteIdAndEstado(Long pacienteId, EstadoIngresoHosp estado);

    long countByEstado(EstadoIngresoHosp estado);

    long countByEstadoAndTipoAndCamaIsNull(EstadoIngresoHosp estado, TipoIngresoHosp tipo);
}
