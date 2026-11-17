package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.MfaUsuario;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MfaUsuarioRepository extends JpaRepository<MfaUsuario, UUID> {

    Optional<MfaUsuario> findByUsuarioId(UUID usuarioId);
}
