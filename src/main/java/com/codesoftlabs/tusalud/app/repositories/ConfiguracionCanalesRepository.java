package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ConfiguracionCanales;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionCanalesRepository extends JpaRepository<ConfiguracionCanales, UUID> {

    Optional<ConfiguracionCanales> findByUsuarioId(UUID usuarioId);
}
