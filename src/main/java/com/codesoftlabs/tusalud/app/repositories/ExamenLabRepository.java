package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ExamenLab;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamenLabRepository extends JpaRepository<ExamenLab, Long> {

    boolean existsByCodigoIgnoreCase(String codigo);

    Optional<ExamenLab> findByCodigoIgnoreCase(String codigo);

    @Query("""
            SELECT DISTINCT e FROM ExamenLab e
            LEFT JOIN FETCH e.parametros
            ORDER BY e.nombre ASC
            """)
    List<ExamenLab> findAllConParametros();

    @Query("""
            SELECT e FROM ExamenLab e
            LEFT JOIN FETCH e.parametros
            WHERE e.id = :id
            """)
    Optional<ExamenLab> findByIdConParametros(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT e FROM ExamenLab e
            LEFT JOIN FETCH e.parametros
            WHERE e.id IN :ids
            """)
    List<ExamenLab> findAllByIdConParametros(@Param("ids") java.util.Collection<Long> ids);
}
