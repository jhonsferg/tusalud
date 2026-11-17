package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SesionCaja;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCaja;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {

    @Query("""
            SELECT s FROM SesionCaja s JOIN FETCH s.cajero
            WHERE s.cajero.id = :cajeroId AND s.estado = :estado
            """)
    Optional<SesionCaja> findDelCajero(@Param("cajeroId") Long cajeroId, @Param("estado") EstadoCaja estado);

    @Query("""
            SELECT s FROM SesionCaja s JOIN FETCH s.cajero
            WHERE (:cajeroId IS NULL OR s.cajero.id = :cajeroId)
            ORDER BY s.apertura DESC, s.id DESC
            """)
    List<SesionCaja> historial(@Param("cajeroId") Long cajeroId, Pageable pageable);
}
