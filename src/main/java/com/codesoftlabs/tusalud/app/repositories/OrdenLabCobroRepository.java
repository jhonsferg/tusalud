package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.OrdenLaboratorio;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenLabCobroRepository extends JpaRepository<OrdenLaboratorio, Long> {

    @Query("""
            SELECT o FROM OrdenLaboratorio o
            JOIN FETCH o.paciente p JOIN FETCH p.usuario
            WHERE o.id = :id
            """)
    Optional<OrdenLaboratorio> findParaCobro(@Param("id") Long id);

    @Query("""
            SELECT o FROM OrdenLaboratorio o
            JOIN FETCH o.paciente p JOIN FETCH p.usuario pu
            WHERE o.cobrado = false
              AND o.estado <> com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenLab.CANCELADA
              AND (:q IS NULL OR LOWER(pu.nombre) LIKE :q OR LOWER(pu.apellido) LIKE :q OR p.dni LIKE :q
                   OR LOWER(o.codigo) LIKE :q)
            ORDER BY o.fecha DESC
            """)
    List<OrdenLaboratorio> pendientesDeCobro(@Param("q") String q, Pageable pageable);
}
