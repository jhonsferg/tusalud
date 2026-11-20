package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CirugiaInsumo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CirugiaInsumoRepository extends JpaRepository<CirugiaInsumo, UUID> {

    @Query(
            "SELECT i FROM CirugiaInsumo i JOIN FETCH i.producto JOIN FETCH i.registradoPor WHERE i.cirugia.id = :cirugiaId ORDER BY i.registradoEn")
    List<CirugiaInsumo> deCirugia(@Param("cirugiaId") UUID cirugiaId);
}
