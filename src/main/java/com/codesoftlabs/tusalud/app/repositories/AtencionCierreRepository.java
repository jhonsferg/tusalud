package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AtencionCierre;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AtencionCierreRepository extends JpaRepository<AtencionCierre, UUID> {

    boolean existsByAtencionId(UUID atencionId);

    @Query("SELECT c FROM AtencionCierre c JOIN FETCH c.cerradoPor WHERE c.atencion.id = :atencionId")
    Optional<AtencionCierre> findByAtencionId(@Param("atencionId") UUID atencionId);
}
