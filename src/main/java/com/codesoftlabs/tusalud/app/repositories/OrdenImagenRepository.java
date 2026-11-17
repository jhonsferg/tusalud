package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.OrdenImagen;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
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
public interface OrdenImagenRepository extends JpaRepository<OrdenImagen, UUID> {

    @Query("""
            SELECT DISTINCT o FROM OrdenImagen o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.estudio
            WHERE o.id = :id
            """)
    Optional<OrdenImagen> findByIdConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT o FROM OrdenImagen o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            ORDER BY o.fecha DESC
            """)
    List<OrdenImagen> findRecientes(Pageable pageable);

    @Query("""
            SELECT o FROM OrdenImagen o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario mu
            WHERE mu.id = :usuarioId
            ORDER BY o.fecha DESC
            """)
    List<OrdenImagen> findDelSolicitante(@Param("usuarioId") UUID usuarioId, Pageable pageable);

    @Query("""
            SELECT o FROM OrdenImagen o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario pu
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            WHERE pu.id = :usuarioId
            ORDER BY o.fecha DESC
            """)
    List<OrdenImagen> findDelPaciente(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT o FROM OrdenImagen o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            WHERE p.id = :pacienteId
            ORDER BY o.fecha DESC
            """)
    List<OrdenImagen> findDePaciente(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT o FROM OrdenImagen o
            JOIN FETCH o.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH o.medico m
            JOIN FETCH m.usuario
            WHERE o.programadaPara >= :desde AND o.programadaPara < :hasta
              AND o.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen.CANCELADA
            ORDER BY o.programadaPara ASC
            """)
    List<OrdenImagen> findProgramadasEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    long countByEstado(EstadoOrdenImagen estado);

    @Query("""
            SELECT COUNT(o) FROM OrdenImagen o
            WHERE o.estado = :estado AND o.informadaEn >= :desde AND o.informadaEn < :hasta
            """)
    long contarInformadasEntre(
            @Param("estado") EstadoOrdenImagen estado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT COUNT(o) FROM OrdenImagen o
            WHERE o.prioridad = com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden.URGENTE
              AND o.estado IN (
                com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen.SOLICITADA,
                com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen.PROGRAMADA,
                com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen.REALIZADA)
            """)
    long contarUrgentesPendientes();

    @Query("""
            SELECT DISTINCT u FROM Usuario u JOIN u.roles r
            WHERE r.nombre = :rol AND u.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario.ACTIVO
            """)
    List<Usuario> findUsuariosActivosPorRol(@Param("rol") RolNombre rol);
}
