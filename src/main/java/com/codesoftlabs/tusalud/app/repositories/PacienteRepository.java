package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Paciente;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteRepository extends JpaRepository<Paciente, Long> {

    @Query("""
            SELECT p FROM Paciente p
            JOIN FETCH p.usuario u
            WHERE LOWER(u.email) = LOWER(:email)
            """)
    Optional<Paciente> findByUsuarioEmail(@Param("email") String email);

    @Query("SELECT p FROM Paciente p JOIN FETCH p.usuario WHERE p.id = :id")
    Optional<Paciente> findByIdWithUsuario(@Param("id") Long id);

    boolean existsByDni(String dni);

    @Query("SELECT COALESCE(MAX(p.id), 0) + 1 FROM Paciente p")
    Long nextCorrelativo();
}
