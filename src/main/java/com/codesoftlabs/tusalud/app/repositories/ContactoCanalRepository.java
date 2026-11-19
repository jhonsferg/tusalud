package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ContactoCanal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContactoCanalRepository extends JpaRepository<ContactoCanal, UUID> {

    Optional<ContactoCanal> findByUsuarioIdAndCanal(UUID usuarioId, Canal canal);

    List<ContactoCanal> findByUsuarioId(UUID usuarioId);
}
