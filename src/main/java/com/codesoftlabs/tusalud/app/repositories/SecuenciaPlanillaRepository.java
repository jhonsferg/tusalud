package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SecuenciaPlanilla;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecuenciaPlanillaRepository extends JpaRepository<SecuenciaPlanilla, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaPlanilla s WHERE s.tipo = :tipo AND s.anio = :anio")
    Optional<SecuenciaPlanilla> bloquear(@Param("tipo") String tipo, @Param("anio") int anio);
}
