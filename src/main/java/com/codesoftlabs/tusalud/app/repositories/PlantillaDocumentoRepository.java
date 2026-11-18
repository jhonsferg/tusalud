package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlantillaDocumento;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlantillaDocumentoRepository extends JpaRepository<PlantillaDocumento, UUID> {

    @Query("""
            SELECT p FROM PlantillaDocumento p
            LEFT JOIN FETCH p.archivo
            LEFT JOIN FETCH p.creadoPor
            WHERE p.codigo = :codigo AND p.activa = true
            """)
    Optional<PlantillaDocumento> findActiva(@Param("codigo") String codigo);

    @Query("""
            SELECT p FROM PlantillaDocumento p
            LEFT JOIN FETCH p.archivo
            LEFT JOIN FETCH p.creadoPor
            WHERE p.codigo = :codigo
            ORDER BY p.version DESC
            """)
    List<PlantillaDocumento> findHistorial(@Param("codigo") String codigo);

    @Query("SELECT COALESCE(MAX(p.version), 0) FROM PlantillaDocumento p WHERE p.codigo = :codigo")
    int maximaVersion(@Param("codigo") String codigo);

    @Query("""
            SELECT p FROM PlantillaDocumento p
            LEFT JOIN FETCH p.archivo
            LEFT JOIN FETCH p.creadoPor
            WHERE p.activa = true
            """)
    List<PlantillaDocumento> findTodasActivas();
}
