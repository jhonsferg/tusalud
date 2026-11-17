package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EvolucionHospitalaria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EvolucionHospitalariaRepository extends JpaRepository<EvolucionHospitalaria, Long> {

    @Query("""
            SELECT e FROM EvolucionHospitalaria e
            JOIN FETCH e.autor
            WHERE e.ingreso.id = :ingresoId
            ORDER BY e.fecha ASC, e.id ASC
            """)
    List<EvolucionHospitalaria> findDelIngreso(@Param("ingresoId") Long ingresoId);

    long countByIngresoId(Long ingresoId);
}
