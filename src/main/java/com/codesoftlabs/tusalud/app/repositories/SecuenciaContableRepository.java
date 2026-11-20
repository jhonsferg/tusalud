package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SecuenciaContable;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecuenciaContableRepository extends JpaRepository<SecuenciaContable, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaContable s WHERE s.tipo = :tipo AND s.anio = :anio")
    Optional<SecuenciaContable> bloquear(@Param("tipo") String tipo, @Param("anio") int anio);
}
