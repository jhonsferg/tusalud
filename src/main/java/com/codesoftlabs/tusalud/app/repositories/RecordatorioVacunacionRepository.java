package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.RecordatorioVacunacion;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecordatorioVacunacionRepository extends JpaRepository<RecordatorioVacunacion, UUID> {

    boolean existsByPacienteIdAndVacunaIdAndNumeroDosisAndFechaProgramada(
            UUID pacienteId, UUID vacunaId, int numeroDosis, LocalDate fechaProgramada);
}
