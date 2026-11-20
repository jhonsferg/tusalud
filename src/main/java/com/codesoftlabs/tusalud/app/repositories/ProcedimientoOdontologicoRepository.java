package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ProcedimientoOdontologico;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcedimientoOdontologicoRepository extends JpaRepository<ProcedimientoOdontologico, UUID> {

    List<ProcedimientoOdontologico> findByActivoTrueOrderByCategoriaAscNombreAsc();

    List<ProcedimientoOdontologico> findAllByOrderByCategoriaAscNombreAsc();
}
