package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EvolucionHospitalaria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EvolucionHospitalariaRepository extends JpaRepository<EvolucionHospitalaria, UUID> {

    @Query("""
            SELECT e FROM EvolucionHospitalaria e
            JOIN FETCH e.autor
            WHERE e.ingreso.id = :ingresoId
            ORDER BY e.fecha ASC, e.id ASC
            """)
    List<EvolucionHospitalaria> findDelIngreso(@Param("ingresoId") UUID ingresoId);

    long countByIngresoId(UUID ingresoId);

    @Query("""
            SELECT e.ingreso.id, MAX(e.fecha) FROM EvolucionHospitalaria e
            WHERE e.ingreso.id IN :ingresoIds AND e.tipo = :tipo
            GROUP BY e.ingreso.id
            """)
    List<Object[]> ultimaPorIngreso(
            @Param("ingresoIds") java.util.Collection<UUID> ingresoIds,
            @Param("tipo") com.codesoftlabs.tusalud.app.entities.enums.TipoEvolucion tipo);
}
