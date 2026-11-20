package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CuentaContableRepository extends JpaRepository<CuentaContable, UUID> {

    Optional<CuentaContable> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<CuentaContable> findAllByOrderByCodigoAsc();
}
