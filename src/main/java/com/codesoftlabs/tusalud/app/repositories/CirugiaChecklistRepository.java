package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CirugiaChecklist;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CirugiaChecklistRepository extends JpaRepository<CirugiaChecklist, UUID> {

    @Query(
            "SELECT k FROM CirugiaChecklist k JOIN FETCH k.completadoPor WHERE k.cirugia.id = :cirugiaId ORDER BY k.completadoEn")
    List<CirugiaChecklist> deCirugia(@Param("cirugiaId") UUID cirugiaId);

    boolean existsByCirugiaIdAndMomento(
            UUID cirugiaId, com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.MomentoChecklist momento);
}
