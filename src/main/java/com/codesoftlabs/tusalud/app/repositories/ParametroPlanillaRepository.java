package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ParametroPlanilla;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ParametroPlanillaRepository extends JpaRepository<ParametroPlanilla, UUID> {

    @Query("""
            SELECT p FROM ParametroPlanilla p
            WHERE p.vigenteDesde <= :fecha AND (p.vigenteHasta IS NULL OR p.vigenteHasta >= :fecha)
            ORDER BY p.codigo ASC
            """)
    List<ParametroPlanilla> vigentesEn(@Param("fecha") LocalDate fecha);

    @Query("""
            SELECT p FROM ParametroPlanilla p
            WHERE (:codigo IS NULL OR p.codigo = :codigo)
            ORDER BY p.codigo ASC, p.vigenteDesde DESC
            """)
    List<ParametroPlanilla> historial(@Param("codigo") String codigo);

    Optional<ParametroPlanilla> findFirstByCodigoAndVigenteHastaIsNullOrderByVigenteDesdeDesc(String codigo);

    boolean existsByCodigoAndVigenteDesde(String codigo, LocalDate vigenteDesde);

    boolean existsByCodigo(String codigo);
}
