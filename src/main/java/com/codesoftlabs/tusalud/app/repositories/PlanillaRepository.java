package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Planilla;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanillaRepository extends JpaRepository<Planilla, UUID> {

    Optional<Planilla> findByAnioAndMes(int anio, int mes);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Planilla p WHERE p.id = :id")
    Optional<Planilla> bloquear(@Param("id") UUID id);

    List<Planilla> findAllByOrderByAnioDescMesDesc();
}
