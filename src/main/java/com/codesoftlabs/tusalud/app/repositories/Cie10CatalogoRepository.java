package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Cie10Catalogo;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface Cie10CatalogoRepository extends JpaRepository<Cie10Catalogo, UUID> {

    Optional<Cie10Catalogo> findByCodigo(String codigo);

    List<Cie10Catalogo> findByCodigoIn(Collection<String> codigos);

    long countByActivoTrue();

    long countByMuestraTrue();

    @Query("""
            SELECT c FROM Cie10Catalogo c
            WHERE c.activo = true AND c.textoBusqueda LIKE :patron
            ORDER BY CASE
                WHEN LOWER(c.codigo) = :exacto THEN 0
                WHEN LOWER(c.codigo) LIKE :prefijo THEN 1
                WHEN c.textoBusqueda LIKE :inicio THEN 2
                ELSE 3 END,
                c.codigo
            """)
    List<Cie10Catalogo> buscar(
            @Param("patron") String patron,
            @Param("exacto") String exacto,
            @Param("prefijo") String prefijo,
            @Param("inicio") String inicio,
            Pageable pageable);
}
