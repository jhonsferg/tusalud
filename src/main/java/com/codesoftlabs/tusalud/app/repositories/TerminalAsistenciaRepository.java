package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.TerminalAsistencia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TerminalAsistenciaRepository extends JpaRepository<TerminalAsistencia, UUID> {

    Optional<TerminalAsistencia> findByTokenHashAndActivoTrue(String tokenHash);

    boolean existsByNombreIgnoreCase(String nombre);

    List<TerminalAsistencia> findAllByOrderByNombreAsc();
}
