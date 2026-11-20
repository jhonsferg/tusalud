package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PrincipioActivo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PrincipioActivoRepository extends JpaRepository<PrincipioActivo, UUID> {

    Optional<PrincipioActivo> findByDciIgnoreCase(String dci);

    @Query("""
            SELECT p FROM PrincipioActivo p
            WHERE p.activo = true
              AND (:q IS NULL OR LOWER(p.dci) LIKE :q OR LOWER(COALESCE(p.claseTerapeutica, '')) LIKE :q)
            ORDER BY p.dci ASC
            """)
    List<PrincipioActivo> buscar(@Param("q") String q);

    List<PrincipioActivo> findAllByOrderByDciAsc();
}
