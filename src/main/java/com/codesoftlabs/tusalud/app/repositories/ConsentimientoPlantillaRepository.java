package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ConsentimientoPlantilla;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentimientoPlantillaRepository extends JpaRepository<ConsentimientoPlantilla, UUID> {

    List<ConsentimientoPlantilla> findAllByOrderByNombreAsc();

    List<ConsentimientoPlantilla> findByActivaTrueOrderByNombreAsc();

    boolean existsByCodigoIgnoreCase(String codigo);

    java.util.Optional<com.codesoftlabs.tusalud.app.entities.ConsentimientoPlantilla> findByCodigo(String codigo);
}
