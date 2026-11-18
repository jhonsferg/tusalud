package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.VerificacionCobertura;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificacionCoberturaRepository extends JpaRepository<VerificacionCobertura, UUID> {

    long countByPolizaId(UUID polizaId);
}
