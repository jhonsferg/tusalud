package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SetInstrumental;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SetInstrumentalRepository extends JpaRepository<SetInstrumental, UUID> {

    List<SetInstrumental> findAllByOrderByCodigoAsc();

    boolean existsByCodigoIgnoreCase(String codigo);
}
