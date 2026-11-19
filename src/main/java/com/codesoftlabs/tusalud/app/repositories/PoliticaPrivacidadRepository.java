package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PoliticaPrivacidad;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PoliticaPrivacidadRepository extends JpaRepository<PoliticaPrivacidad, UUID> {

    @Query("""
            SELECT p FROM PoliticaPrivacidad p
            WHERE p.publicada = true AND p.vigenteDesde <= :hoy
            ORDER BY p.version DESC
            """)
    List<PoliticaPrivacidad> vigentes(@Param("hoy") LocalDate hoy, Pageable pageable);

    default Optional<PoliticaPrivacidad> vigente(LocalDate hoy) {
        return vigentes(hoy, Pageable.ofSize(1)).stream().findFirst();
    }

    @Query("SELECT COALESCE(MAX(p.version), 0) FROM PoliticaPrivacidad p")
    int ultimaVersion();

    List<PoliticaPrivacidad> findAllByOrderByVersionDesc();
}
