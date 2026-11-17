package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AuditoriaClinica;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditoriaClinicaRepository extends JpaRepository<AuditoriaClinica, UUID> {

    List<AuditoriaClinica> findByPacienteIdOrderByFechaDesc(UUID pacienteId);
}
