package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PlantillaMensaje;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantillaMensajeRepository extends JpaRepository<PlantillaMensaje, UUID> {

    Optional<PlantillaMensaje> findByTipoAndCanalAndActivaTrue(TipoNotificacion tipo, Canal canal);

    List<PlantillaMensaje> findByActivaTrue();

    List<PlantillaMensaje> findByTipoAndCanalOrderByVersionDesc(TipoNotificacion tipo, Canal canal);
}
