package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AsientoLinea;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsientoLineaRepository extends JpaRepository<AsientoLinea, UUID> {

    boolean existsByCuentaId(UUID cuentaId);
}
