package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PeriodoContable;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PeriodoContableRepository extends JpaRepository<PeriodoContable, UUID> {

    Optional<PeriodoContable> findByAnioAndMes(int anio, int mes);

    List<PeriodoContable> findAllByOrderByAnioDescMesDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PeriodoContable p WHERE p.id = :id")
    Optional<PeriodoContable> bloquear(@Param("id") UUID id);
}
