package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CirugiaEvento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CirugiaEventoRepository extends JpaRepository<CirugiaEvento, UUID> {

    @Query("SELECT e FROM CirugiaEvento e JOIN FETCH e.usuario WHERE e.cirugia.id = :cirugiaId ORDER BY e.fecha")
    List<CirugiaEvento> deCirugia(@Param("cirugiaId") UUID cirugiaId);
}
