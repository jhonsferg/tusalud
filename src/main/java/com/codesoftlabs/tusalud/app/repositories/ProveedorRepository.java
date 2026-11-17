package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Proveedor;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProveedorRepository extends JpaRepository<Proveedor, UUID> {

    @Query("""
            SELECT p FROM Proveedor p
            WHERE (:activo IS NULL OR p.activo = :activo)
              AND (:q IS NULL OR LOWER(p.razonSocial) LIKE :q OR p.ruc LIKE :q)
            ORDER BY p.razonSocial ASC
            """)
    List<Proveedor> buscar(@Param("q") String q, @Param("activo") Boolean activo);

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, UUID id);
}
