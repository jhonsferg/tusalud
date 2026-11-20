package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.LoteEsteril;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteEsterilRepository extends JpaRepository<LoteEsteril, UUID> {

    @Query("""
            SELECT l FROM LoteEsteril l
            JOIN FETCH l.setInstrumental JOIN FETCH l.ciclo c JOIN FETCH c.esterilizador
            WHERE l.id = :id
            """)
    Optional<LoteEsteril> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT l FROM LoteEsteril l
            JOIN FETCH l.setInstrumental JOIN FETCH l.ciclo c JOIN FETCH c.esterilizador
            WHERE l.id = :id
            """)
    Optional<LoteEsteril> findParaUsar(@Param("id") UUID id);

    @Query("""
            SELECT l FROM LoteEsteril l
            JOIN FETCH l.setInstrumental JOIN FETCH l.ciclo c JOIN FETCH c.esterilizador
            WHERE l.estado <> com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoLoteEsteril.USADO
            ORDER BY l.venceEn
            """)
    List<LoteEsteril> vigentes();

    @Query("""
            SELECT l FROM LoteEsteril l
            JOIN FETCH l.setInstrumental JOIN FETCH l.ciclo c JOIN FETCH c.esterilizador
            WHERE c.id = :cicloId
            ORDER BY l.codigo
            """)
    List<LoteEsteril> deCiclo(@Param("cicloId") UUID cicloId);

    @Query("""
            SELECT l FROM LoteEsteril l
            JOIN FETCH l.setInstrumental JOIN FETCH l.ciclo c JOIN FETCH c.esterilizador
            ORDER BY l.esterilizadoEn DESC
            """)
    List<LoteEsteril> recientes(org.springframework.data.domain.Pageable pagina);
}
