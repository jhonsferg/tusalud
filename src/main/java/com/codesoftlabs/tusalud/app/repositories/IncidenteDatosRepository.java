package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.IncidenteDatos;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidenteDatosRepository extends JpaRepository<IncidenteDatos, UUID> {

    @Query("""
            SELECT i FROM IncidenteDatos i LEFT JOIN FETCH i.responsable JOIN FETCH i.registradoPor
            ORDER BY i.detectadoEn DESC
            """)
    List<IncidenteDatos> todos();

    @Query("""
            SELECT i FROM IncidenteDatos i LEFT JOIN FETCH i.responsable JOIN FETCH i.registradoPor
            WHERE i.id = :id
            """)
    Optional<IncidenteDatos> findConDetalle(@Param("id") UUID id);

    @Query("SELECT COUNT(i) FROM IncidenteDatos i WHERE i.detectadoEn >= :desde AND i.detectadoEn < :hasta")
    long contarEntre(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
