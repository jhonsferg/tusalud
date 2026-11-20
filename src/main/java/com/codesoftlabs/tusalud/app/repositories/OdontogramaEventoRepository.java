package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.OdontogramaEvento;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

    @Query("SELECT MAX(e.registradoEn) FROM OdontogramaEvento e WHERE e.paciente.id = :pacienteId")
    Optional<LocalDateTime> ultimoRegistro(@Param("pacienteId") UUID pacienteId);
}
