package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.OrdenImagen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenImagenCobroRepository extends JpaRepository<OrdenImagen, UUID> {

    @Query("""
            SELECT DISTINCT o FROM OrdenImagen o
            JOIN FETCH o.paciente p JOIN FETCH p.usuario
            LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.estudio
            WHERE o.id = :id
            """)
    Optional<OrdenImagen> findParaCobro(@Param("id") UUID id);

    @Query("""
            SELECT o FROM OrdenImagen o
            JOIN FETCH o.paciente p JOIN FETCH p.usuario pu
            WHERE o.cobrado = false
              AND o.estado IN (
                com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen.REALIZADA,
                com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen.INFORMADA)
              AND (:q IS NULL OR LOWER(pu.nombre) LIKE :q OR LOWER(pu.apellido) LIKE :q OR p.dni LIKE :q
                   OR LOWER(o.codigo) LIKE :q)
            ORDER BY o.fecha DESC
            """)
    List<OrdenImagen> pendientesDeCobro(@Param("q") String q, Pageable pageable);
}
