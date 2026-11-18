package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Convenio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConvenioRepository extends JpaRepository<Convenio, UUID> {

    @Query("""
            SELECT DISTINCT c FROM Convenio c
            JOIN FETCH c.aseguradora
            LEFT JOIN FETCH c.reglas
            ORDER BY c.nombre ASC
            """)
    List<Convenio> todos();

    @Query("""
            SELECT c FROM Convenio c
            JOIN FETCH c.aseguradora
            LEFT JOIN FETCH c.reglas
            WHERE c.id = :id
            """)
    Optional<Convenio> findConDetalle(@Param("id") UUID id);
}
