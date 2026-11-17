package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Producto;
import com.codesoftlabs.tusalud.app.entities.enums.TipoProducto;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @Query("""
            SELECT p FROM Producto p
            WHERE (:tipo IS NULL OR p.tipo = :tipo)
              AND (:activo IS NULL OR p.activo = :activo)
              AND (:q IS NULL OR LOWER(p.nombre) LIKE :q OR LOWER(p.codigo) LIKE :q
                   OR LOWER(COALESCE(p.principioActivo, '')) LIKE :q)
            ORDER BY p.nombre ASC
            """)
    List<Producto> buscar(
            @Param("q") String q, @Param("tipo") TipoProducto tipo, @Param("activo") Boolean activo, Pageable pageable);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    long countByActivoTrue();
}
