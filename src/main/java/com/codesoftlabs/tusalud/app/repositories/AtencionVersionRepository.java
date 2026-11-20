package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AtencionVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtencionVersionRepository extends JpaRepository<AtencionVersion, UUID> {

    @Query("""
            SELECT v FROM AtencionVersion v JOIN FETCH v.autor
            WHERE v.atencion.id = :atencionId
            ORDER BY v.numero ASC
            """)
    List<AtencionVersion> historial(@Param("atencionId") UUID atencionId);

    @Query("SELECT COALESCE(MAX(v.numero), 0) FROM AtencionVersion v WHERE v.atencion.id = :atencionId")
    int ultimoNumero(@Param("atencionId") UUID atencionId);
}
