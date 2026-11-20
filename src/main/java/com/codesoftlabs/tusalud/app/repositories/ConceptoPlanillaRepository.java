package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ConceptoPlanilla;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConceptoPlanillaRepository extends JpaRepository<ConceptoPlanilla, UUID> {

    List<ConceptoPlanilla> findAllByOrderByOrdenAscNombreAsc();

    Optional<ConceptoPlanilla> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
