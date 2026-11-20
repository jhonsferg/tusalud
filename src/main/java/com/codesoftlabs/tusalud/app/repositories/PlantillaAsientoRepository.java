package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlantillaAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EventoContable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlantillaAsientoRepository extends JpaRepository<PlantillaAsiento, UUID> {

    @Query("""
            SELECT DISTINCT p FROM PlantillaAsiento p
            LEFT JOIN FETCH p.lineas l
            LEFT JOIN FETCH l.cuenta
            WHERE p.evento = :evento
            """)
    Optional<PlantillaAsiento> conLineas(@Param("evento") EventoContable evento);

    @Query("""
            SELECT DISTINCT p FROM PlantillaAsiento p
            LEFT JOIN FETCH p.lineas l
            LEFT JOIN FETCH l.cuenta
            WHERE p.id = :id
            """)
    Optional<PlantillaAsiento> conLineas(@Param("id") UUID id);

    @Query("""
            SELECT DISTINCT p FROM PlantillaAsiento p
            LEFT JOIN FETCH p.lineas l
            LEFT JOIN FETCH l.cuenta
            ORDER BY p.nombre
            """)
    List<PlantillaAsiento> todasConLineas();
}
