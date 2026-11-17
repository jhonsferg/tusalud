package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.InformeImagen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InformeImagenRepository extends JpaRepository<InformeImagen, UUID> {

    List<InformeImagen> findByOrdenIdOrderByVersionDesc(UUID ordenId);

    Optional<InformeImagen> findFirstByOrdenIdOrderByVersionDesc(UUID ordenId);

    Optional<InformeImagen> findFirstByAutorIdOrderByCreatedAtDesc(UUID autorId);
}
