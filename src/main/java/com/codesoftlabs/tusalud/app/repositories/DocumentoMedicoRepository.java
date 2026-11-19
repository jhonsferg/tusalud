package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.DocumentoMedico;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentoMedicoRepository extends JpaRepository<DocumentoMedico, UUID> {

    @Query("""
            SELECT d FROM DocumentoMedico d
            JOIN FETCH d.paciente p JOIN FETCH p.usuario
            JOIN FETCH d.medico m JOIN FETCH m.usuario
            WHERE d.id = :id
            """)
    Optional<DocumentoMedico> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT d FROM DocumentoMedico d
            JOIN FETCH d.paciente p JOIN FETCH p.usuario
            JOIN FETCH d.medico m JOIN FETCH m.usuario
            WHERE d.codigoVerificacion = :codigo
            """)
    Optional<DocumentoMedico> findByCodigo(@Param("codigo") String codigo);

    @Query("""
            SELECT d FROM DocumentoMedico d
            JOIN FETCH d.medico m JOIN FETCH m.usuario
            WHERE d.paciente.id = :pacienteId
            ORDER BY d.creadoEn DESC
            """)
    List<DocumentoMedico> findDelPaciente(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT d FROM DocumentoMedico d
            JOIN FETCH d.paciente p JOIN FETCH p.usuario
            JOIN FETCH d.medico m JOIN FETCH m.usuario
            WHERE d.medico.id = :medicoId
            ORDER BY d.creadoEn DESC
            """)
    List<DocumentoMedico> findDelMedico(@Param("medicoId") UUID medicoId);

    @Query("""
            SELECT d FROM DocumentoMedico d
            JOIN FETCH d.medico m JOIN FETCH m.usuario
            WHERE d.ingreso.id = :ingresoId
            ORDER BY d.creadoEn DESC
            """)
    List<DocumentoMedico> findDelIngreso(@Param("ingresoId") UUID ingresoId);

    @Query("""
            SELECT d FROM DocumentoMedico d
            WHERE d.referenciaId = :referenciaId
            ORDER BY d.creadoEn DESC
            """)
    List<DocumentoMedico> findContrarreferencias(@Param("referenciaId") UUID referenciaId);

    boolean existsByDocumentoOrigenId(UUID documentoOrigenId);
}
