package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Ingreso;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface IngresoRepository extends JpaRepository<Ingreso, Long> {

    @Query("""
            SELECT i FROM Ingreso i
            LEFT JOIN FETCH i.proveedor
            JOIN FETCH i.usuario
            ORDER BY i.fecha DESC, i.id DESC
            """)
    List<Ingreso> recientes(Pageable pageable);
}
