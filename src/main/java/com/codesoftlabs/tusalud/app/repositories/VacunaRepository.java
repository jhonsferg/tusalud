package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Vacuna;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VacunaRepository extends JpaRepository<Vacuna, UUID> {

    List<Vacuna> findByActivoTrueOrderByNombre();

    Optional<Vacuna> findByCodigo(String codigo);
}
