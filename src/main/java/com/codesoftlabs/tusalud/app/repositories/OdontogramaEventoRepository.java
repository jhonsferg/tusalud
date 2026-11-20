package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.OdontogramaEvento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OdontogramaEventoRepository extends JpaRepository<OdontogramaEvento, UUID> {

    @Query("""
            SELECT e FROM OdontogramaEvento e JOIN FETCH e.registradoPor
            WHERE e.paciente.id = :pacienteId
            ORDER BY e.registradoEn ASC
            """)
    List<OdontogramaEvento> historial(@Param("pacienteId") UUID pacienteId);
}
