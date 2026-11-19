package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PoliticaRetencion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaRetencionRepository extends JpaRepository<PoliticaRetencion, UUID> {

    List<PoliticaRetencion> findAllByOrderByNombreAsc();

    Optional<PoliticaRetencion> findByTipoDato(String tipoDato);
}
