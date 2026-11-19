package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ConsentimientoTelemedicina;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConsentimientoTelemedicinaRepository extends JpaRepository<ConsentimientoTelemedicina, UUID> {

    boolean existsByPacienteIdAndVersion(UUID pacienteId, String version);
}
