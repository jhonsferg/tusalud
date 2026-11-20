package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PoliticaConservacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PoliticaConservacionRepository extends JpaRepository<PoliticaConservacion, UUID> {

    List<PoliticaConservacion> findAllByOrderByTipoRegistroAsc();

    Optional<PoliticaConservacion> findByTipoRegistro(String tipoRegistro);
}
