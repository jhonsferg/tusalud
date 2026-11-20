package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CarneVacunacion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarneVacunacionRepository extends JpaRepository<CarneVacunacion, UUID> {

    @Query("""
            SELECT c FROM CarneVacunacion c
            JOIN FETCH c.paciente p JOIN FETCH c.emitidoPor
            WHERE c.codigoVerificacion = :codigo
            """)
    Optional<CarneVacunacion> findByCodigo(@Param("codigo") String codigo);
}
