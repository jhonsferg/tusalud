package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AsientoPendiente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsientoPendienteRepository extends JpaRepository<AsientoPendiente, UUID> {

    List<AsientoPendiente> findByResueltoEnIsNullOrderByCreadoEnAsc();

    long countByResueltoEnIsNull();

    Optional<AsientoPendiente> findFirstByEventoAndReferenciaIdAndResueltoEnIsNull(String evento, UUID referenciaId);
}
