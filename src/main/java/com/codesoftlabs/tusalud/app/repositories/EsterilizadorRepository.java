package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Esterilizador;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EsterilizadorRepository extends JpaRepository<Esterilizador, UUID> {

    List<Esterilizador> findAllByOrderByCodigoAsc();

    boolean existsByCodigoIgnoreCase(String codigo);
}
