package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SecuenciaCompra;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecuenciaCompraRepository extends JpaRepository<SecuenciaCompra, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SecuenciaCompra s WHERE s.tipo = :tipo AND s.anio = :anio")
    Optional<SecuenciaCompra> bloquear(@Param("tipo") String tipo, @Param("anio") int anio);
}
