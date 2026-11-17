package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.MetodoPago;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MetodoPagoRepository extends JpaRepository<MetodoPago, UUID> {

    List<MetodoPago> findByUsuarioIdOrderByPredeterminadoDescCreatedAtDesc(UUID usuarioId);

    Optional<MetodoPago> findByIdAndUsuarioId(UUID id, UUID usuarioId);
}
