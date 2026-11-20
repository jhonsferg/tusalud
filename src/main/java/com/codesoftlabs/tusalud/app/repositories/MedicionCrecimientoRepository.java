package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.MedicionCrecimiento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicionCrecimientoRepository extends JpaRepository<MedicionCrecimiento, UUID> {

    @Query("""
            SELECT m FROM MedicionCrecimiento m
            WHERE m.paciente.id = :pacienteId AND m.anulada = false
            ORDER BY m.fecha ASC, m.creadoEn ASC
            """)
    List<MedicionCrecimiento> vigentesDelPaciente(@Param("pacienteId") UUID pacienteId);
}
