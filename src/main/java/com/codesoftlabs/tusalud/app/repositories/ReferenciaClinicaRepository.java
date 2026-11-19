package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ReferenciaClinica;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReferenciaClinicaRepository extends JpaRepository<ReferenciaClinica, UUID> {

    @Query("""
            SELECT r FROM ReferenciaClinica r JOIN FETCH r.destino JOIN FETCH r.documento d
            WHERE d.id = :documentoId
            """)
    Optional<ReferenciaClinica> findPorDocumento(@Param("documentoId") UUID documentoId);

    @Query("""
            SELECT r FROM ReferenciaClinica r JOIN FETCH r.destino JOIN FETCH r.documento d
            WHERE d.id IN :documentoIds
            """)
    List<ReferenciaClinica> findPorDocumentos(@Param("documentoIds") List<UUID> documentoIds);
}
