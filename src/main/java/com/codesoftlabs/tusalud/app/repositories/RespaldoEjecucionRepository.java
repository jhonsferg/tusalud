package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.RespaldoEjecucion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.TipoEjecucionRespaldo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RespaldoEjecucionRepository extends JpaRepository<RespaldoEjecucion, UUID> {

    List<RespaldoEjecucion> findByTipoOrderByIniciadoEnDesc(TipoEjecucionRespaldo tipo, Pageable pageable);

    List<RespaldoEjecucion> findAllByOrderByIniciadoEnDesc(Pageable pageable);

    Optional<RespaldoEjecucion> findFirstByTipoAndEstadoOrderByFinalizadoEnDesc(
            TipoEjecucionRespaldo tipo, EstadoEjecucionRespaldo estado);

    Optional<RespaldoEjecucion> findFirstByTipoOrderByFinalizadoEnDesc(TipoEjecucionRespaldo tipo);
}
