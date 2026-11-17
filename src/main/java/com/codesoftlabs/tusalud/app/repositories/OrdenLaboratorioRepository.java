package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.OrdenLaboratorio;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab;
import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
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
public interface OrdenLaboratorioRepository extends JpaRepository<OrdenLaboratorio, UUID> {

    @Query("""
            SELECT o FROM OrdenLaboratorio o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            WHERE o.id = :id
            """)
    Optional<OrdenLaboratorio> findByIdConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT o FROM OrdenLaboratorio o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            ORDER BY o.fecha DESC
            """)
    List<OrdenLaboratorio> findRecientes(Pageable pageable);

    @Query("""
            SELECT o FROM OrdenLaboratorio o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario mu
            WHERE mu.id = :usuarioId
            ORDER BY o.fecha DESC
            """)
    List<OrdenLaboratorio> findDelSolicitante(@Param("usuarioId") UUID usuarioId, Pageable pageable);

    @Query("""
            SELECT o FROM OrdenLaboratorio o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario pu
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            WHERE pu.id = :usuarioId
            ORDER BY o.fecha DESC
            """)
    List<OrdenLaboratorio> findDelPaciente(@Param("usuarioId") UUID usuarioId);

    long countByEstado(EstadoOrdenLab estado);

    @Query("""
            SELECT COUNT(o) FROM OrdenLaboratorio o
            WHERE o.estado = :estado AND o.completadaEn >= :desde AND o.completadaEn < :hasta
            """)
    long contarCompletadasEntre(
            @Param("estado") EstadoOrdenLab estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT COUNT(o) FROM OrdenLaboratorio o
            WHERE o.prioridad = com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden.URGENTE
              AND o.estado IN (
                  com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab.SOLICITADA,
                  com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab.MUESTRA_TOMADA,
                  com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab.EN_PROCESO)
            """)
    long contarUrgentesPendientes();

    @Query("""
            SELECT DISTINCT u FROM Usuario u JOIN u.roles r
            WHERE r.nombre = :rol AND u.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario.ACTIVO
            """)
    List<Usuario> findUsuariosActivosPorRol(@Param("rol") RolNombre rol);
}
