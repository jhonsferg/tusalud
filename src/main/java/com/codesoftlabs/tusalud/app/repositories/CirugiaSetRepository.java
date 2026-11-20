package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CirugiaSet;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CirugiaSetRepository extends JpaRepository<CirugiaSet, UUID> {

    @Query("""
            SELECT s FROM CirugiaSet s
            JOIN FETCH s.lote l JOIN FETCH l.setInstrumental JOIN FETCH l.ciclo c JOIN FETCH c.esterilizador
            JOIN FETCH s.registradoPor
            WHERE s.cirugia.id = :cirugiaId ORDER BY s.registradoEn
            """)
    List<CirugiaSet> deCirugia(@Param("cirugiaId") UUID cirugiaId);

    @Query("""
            SELECT s FROM CirugiaSet s
            JOIN FETCH s.cirugia c LEFT JOIN FETCH c.quirofano
            WHERE s.lote.id IN :loteIds
            """)
    List<CirugiaSet> deLotes(@Param("loteIds") java.util.Collection<UUID> loteIds);
}
