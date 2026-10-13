package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Medico;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MedicoRepository extends JpaRepository<Medico, Long> {

    @Query("""
            SELECT m FROM Medico m
            JOIN FETCH m.usuario u
            JOIN FETCH m.especialidad e
            WHERE e.id = :especialidadId
              AND u.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoUsuario.ACTIVO
            ORDER BY u.apellido ASC, u.nombre ASC
            """)
    List<Medico> findByEspecialidadId(@Param("especialidadId") Long especialidadId);

    @Query("""
            SELECT m FROM Medico m
            JOIN FETCH m.usuario u
            JOIN FETCH m.especialidad e
            ORDER BY u.apellido ASC, u.nombre ASC
            """)
    List<Medico> findAllWithDetails();

    @Query("""
            SELECT m FROM Medico m
            JOIN FETCH m.usuario
            JOIN FETCH m.especialidad
            WHERE m.id = :id
            """)
    Optional<Medico> findByIdWithDetails(@Param("id") Long id);

    @Query("""
            SELECT m FROM Medico m
            JOIN FETCH m.usuario u
            JOIN FETCH m.especialidad
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<Medico> findByUsuarioEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Medico m WHERE m.id = :id")
    Optional<Medico> findByIdForUpdate(@Param("id") Long id);

    boolean existsByColegiaturaIgnoreCase(String colegiatura);
}
