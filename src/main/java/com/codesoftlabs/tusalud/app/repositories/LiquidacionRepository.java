package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Liquidacion;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoLiquidacion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, UUID> {

    String DETALLE = """
            SELECT l FROM Liquidacion l
            JOIN FETCH l.aseguradora
            JOIN FETCH l.creadaPor
            """;

    @Query(DETALLE + " ORDER BY l.creadaEn DESC")
    List<Liquidacion> todas();

    @Query(DETALLE + " WHERE l.id = :id")
    Optional<Liquidacion> findConDetalle(@Param("id") UUID id);

    long countByEstadoIn(Collection<EstadoLiquidacion> estados);

    long countByCodigoStartingWith(String prefijo);
}
