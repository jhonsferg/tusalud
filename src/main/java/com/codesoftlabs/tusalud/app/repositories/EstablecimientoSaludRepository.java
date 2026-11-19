package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EstablecimientoSalud;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstablecimientoSaludRepository extends JpaRepository<EstablecimientoSalud, UUID> {

    List<EstablecimientoSalud> findByActivoTrueOrderByNombreAsc();

    List<EstablecimientoSalud> findAllByOrderByNombreAsc();

    boolean existsByCodigoIpress(String codigoIpress);
}
