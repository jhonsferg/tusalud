package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Archivo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchivoRepository extends JpaRepository<Archivo, UUID> {

    @Query("""
            SELECT a.bucket, a.proposito, COUNT(a), COALESCE(SUM(a.tamano), 0) FROM Archivo a
            WHERE a.eliminado = false
            GROUP BY a.bucket, a.proposito
            ORDER BY a.bucket, a.proposito
            """)
    List<Object[]> usoPorBucketYProposito();

    @Query("SELECT a FROM Archivo a WHERE a.id = :id AND a.eliminado = false")
    Optional<Archivo> findActivo(@Param("id") UUID id);

    @Query("""
            SELECT a FROM Archivo a
            WHERE a.referenciaTipo = :tipo AND a.referenciaId = :referenciaId AND a.eliminado = false
            ORDER BY a.createdAt DESC
            """)
    List<Archivo> findActivosDeReferencia(@Param("tipo") String tipo, @Param("referenciaId") UUID referenciaId);
}
