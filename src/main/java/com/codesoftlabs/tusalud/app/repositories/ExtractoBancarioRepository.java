package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ExtractoBancario;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractoBancarioRepository extends JpaRepository<ExtractoBancario, UUID> {

    @Query("""
            SELECT e FROM ExtractoBancario e
            JOIN FETCH e.cuenta
            JOIN FETCH e.importadoPor
            WHERE (:cuentaId IS NULL OR e.cuenta.id = :cuentaId)
            ORDER BY e.importadoEn DESC
            """)
    List<ExtractoBancario> recientes(@Param("cuentaId") UUID cuentaId);
}
