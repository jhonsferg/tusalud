package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EstudioImagen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstudioImagenRepository extends JpaRepository<EstudioImagen, UUID> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<EstudioImagen> findByCodigoIgnoreCase(String codigo);

    List<EstudioImagen> findAllByOrderByNombreAsc();
}
