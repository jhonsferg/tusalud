package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SecuenciaQuirofano;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecuenciaQuirofanoRepository extends JpaRepository<SecuenciaQuirofano, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaQuirofano s WHERE s.nombre = :nombre")
    Optional<SecuenciaQuirofano> bloquear(@Param("nombre") String nombre);
}
