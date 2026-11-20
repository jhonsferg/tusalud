package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EsquemaDosis;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EsquemaDosisRepository extends JpaRepository<EsquemaDosis, UUID> {

    @Query("""
            SELECT d FROM EsquemaDosis d JOIN FETCH d.vacuna v
            WHERE d.esquema.id = :esquemaId
            ORDER BY d.edadRecomendadaDias, v.nombre, d.numeroDosis
            """)
    List<EsquemaDosis> delEsquema(@Param("esquemaId") UUID esquemaId);
}
