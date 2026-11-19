package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.FinalidadTratamiento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FinalidadTratamientoRepository extends JpaRepository<FinalidadTratamiento, UUID> {

    @Query("SELECT f FROM FinalidadTratamiento f LEFT JOIN FETCH f.banco ORDER BY f.nombre ASC")
    List<FinalidadTratamiento> todas();

    @Query("SELECT f FROM FinalidadTratamiento f LEFT JOIN FETCH f.banco WHERE f.activa = true ORDER BY f.nombre ASC")
    List<FinalidadTratamiento> activas();

    boolean existsByCodigo(String codigo);
}
