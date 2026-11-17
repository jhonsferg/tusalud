package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PacienteCompartido;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PacienteCompartidoRepository extends JpaRepository<PacienteCompartido, UUID> {

    boolean existsByPacienteIdAndMedicoDestinoId(UUID pacienteId, UUID medicoDestinoId);

    @Query("""
            SELECT COUNT(pc) > 0 FROM PacienteCompartido pc
            WHERE pc.paciente.id = :pacienteId AND pc.medicoDestino.usuario.id = :usuarioId
            """)
    boolean existsParaUsuario(@Param("pacienteId") UUID pacienteId, @Param("usuarioId") UUID usuarioId);

    Optional<PacienteCompartido> findByPacienteIdAndMedicoDestinoId(UUID pacienteId, UUID medicoDestinoId);

    @Query("""
            SELECT DISTINCT pc FROM PacienteCompartido pc
            JOIN FETCH pc.medicoDestino md
            JOIN FETCH md.usuario
            LEFT JOIN FETCH md.especialidades
            JOIN FETCH pc.medicoOrigen
            WHERE pc.paciente.id = :pacienteId
            ORDER BY pc.createdAt DESC
            """)
    List<PacienteCompartido> findByPacienteIdConDetalle(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT pc FROM PacienteCompartido pc
            JOIN FETCH pc.paciente p
            JOIN FETCH p.usuario
            JOIN FETCH pc.medicoOrigen mo
            JOIN FETCH mo.usuario
            WHERE pc.medicoDestino.usuario.id = :usuarioId
            ORDER BY pc.createdAt DESC
            """)
    List<PacienteCompartido> findParaUsuarioConDetalle(@Param("usuarioId") UUID usuarioId);
}
