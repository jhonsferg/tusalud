package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.HceImpresion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HceImpresionRepository extends JpaRepository<HceImpresion, UUID> {

    Optional<HceImpresion> findByCodigoVerificacion(String codigo);
}
