package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EncargadoTratamiento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EncargadoTratamientoRepository extends JpaRepository<EncargadoTratamiento, UUID> {

    List<EncargadoTratamiento> findAllByOrderByRazonSocialAsc();
}
