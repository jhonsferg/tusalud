package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PreferenciaCanal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenciaCanalRepository extends JpaRepository<PreferenciaCanal, UUID> {

    List<PreferenciaCanal> findByUsuarioId(UUID usuarioId);
}
