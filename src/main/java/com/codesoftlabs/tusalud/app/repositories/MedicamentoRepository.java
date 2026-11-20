package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Medicamento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicamentoRepository extends JpaRepository<Medicamento, UUID> {

    @Query("""
            SELECT m FROM Medicamento m JOIN FETCH m.principioActivo p
            WHERE m.activo = true
              AND (:q IS NULL
                   OR LOWER(p.dci) LIKE :q
                   OR LOWER(COALESCE(m.nombreComercial, '')) LIKE :q
                   OR LOWER(COALESCE(p.claseTerapeutica, '')) LIKE :q)
            ORDER BY p.dci ASC, m.concentracion ASC
            """)
    List<Medicamento> buscar(@Param("q") String q, Pageable pageable);

    @Query("SELECT m FROM Medicamento m JOIN FETCH m.principioActivo WHERE m.id = :id")
    Optional<Medicamento> findConPrincipio(@Param("id") UUID id);

    @Query("SELECT m FROM Medicamento m JOIN FETCH m.principioActivo WHERE m.id IN :ids")
    List<Medicamento> findConPrincipioPorIds(@Param("ids") Collection<UUID> ids);

    @Query("""
            SELECT COUNT(m) > 0 FROM Medicamento m
            WHERE m.principioActivo.id = :principioId
              AND LOWER(m.formaFarmaceutica) = LOWER(:forma)
              AND LOWER(m.concentracion) = LOWER(:concentracion)
            """)
    boolean existeCombinacion(
            @Param("principioId") UUID principioId,
            @Param("forma") String forma,
            @Param("concentracion") String concentracion);

    @Query("""
            SELECT m FROM Medicamento m JOIN FETCH m.principioActivo
            WHERE m.principioActivo.id = :principioId
              AND LOWER(m.formaFarmaceutica) = LOWER(:forma)
              AND LOWER(m.concentracion) = LOWER(:concentracion)
            """)
    Optional<Medicamento> findCombinacion(
            @Param("principioId") UUID principioId,
            @Param("forma") String forma,
            @Param("concentracion") String concentracion);

    long countByActivoTrue();
}
