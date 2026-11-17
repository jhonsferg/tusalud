package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Empleado;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import com.codesoftlabs.tusalud.app.entities.enums.AreaLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmpleadoRepository extends JpaRepository<Empleado, UUID> {

    @Query("""
            SELECT e FROM Empleado e JOIN FETCH e.usuario u
            WHERE u.id = :usuarioId
            """)
    Optional<Empleado> findByUsuarioId(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT e FROM Empleado e JOIN FETCH e.usuario u
            WHERE e.id = :id
            """)
    Optional<Empleado> findByIdConUsuario(@Param("id") UUID id);

    @Query("""
            SELECT e FROM Empleado e JOIN FETCH e.usuario u
            WHERE (:area IS NULL OR e.area = :area)
              AND (:estado IS NULL OR e.estado = :estado)
              AND (:q IS NULL OR LOWER(u.nombre) LIKE :q OR LOWER(u.apellido) LIKE :q
                   OR LOWER(e.cargo) LIKE :q OR LOWER(u.email) LIKE :q)
            ORDER BY u.apellido ASC, u.nombre ASC
            """)
    List<Empleado> buscar(
            @Param("q") String q, @Param("area") AreaLaboral area, @Param("estado") EstadoEmpleado estado);

    @Query("""
            SELECT e FROM Empleado e JOIN FETCH e.usuario u
            WHERE e.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado.ACTIVO AND (:area IS NULL OR e.area = :area)
            ORDER BY u.apellido ASC, u.nombre ASC
            """)
    List<Empleado> activosPorArea(@Param("area") AreaLaboral area);

    @Query("SELECT e.estado, COUNT(e) FROM Empleado e GROUP BY e.estado")
    List<Object[]> contarPorEstado();

    @Query("""
            SELECT e.area, COUNT(e) FROM Empleado e
            WHERE e.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado.CESADO
            GROUP BY e.area ORDER BY e.area
            """)
    List<Object[]> contarPorArea();

    @Query("""
            SELECT e FROM Empleado e JOIN FETCH e.usuario
            WHERE e.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado.CESADO
              AND e.fechaFinContrato IS NOT NULL AND e.fechaFinContrato BETWEEN :desde AND :hasta
            ORDER BY e.fechaFinContrato ASC
            """)
    List<Empleado> contratosPorVencer(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query(
            "SELECT COALESCE(SUM(e.sueldo), 0) FROM Empleado e WHERE e.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoEmpleado.ACTIVO")
    BigDecimal masaSalarial();

    @Query("""
            SELECT DISTINCT u FROM Usuario u JOIN FETCH u.roles r
            WHERE u.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario.ACTIVO
              AND NOT EXISTS (SELECT 1 FROM Empleado e WHERE e.usuario = u)
              AND EXISTS (SELECT 1 FROM u.roles r2 WHERE r2.nombre <> com.codesoftlabs.tusalud.app.entities.enums.RolNombre.ROLE_PACIENTE)
            ORDER BY u.apellido ASC, u.nombre ASC
            """)
    List<Usuario> usuariosSinFicha();
}
