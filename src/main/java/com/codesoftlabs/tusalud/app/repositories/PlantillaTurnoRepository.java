package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlantillaTurno;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PlantillaTurnoRepository extends JpaRepository<PlantillaTurno, UUID> {

    List<PlantillaTurno> findByActivaTrueOrderByHoraInicioAsc();
}
