package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ConfiguracionFiscal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionFiscalRepository extends JpaRepository<ConfiguracionFiscal, UUID> {

    Optional<ConfiguracionFiscal> findBySede(String sede);
}
