package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EjecucionRetencion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EjecucionRetencionRepository extends JpaRepository<EjecucionRetencion, UUID> {

    @Query("SELECT e FROM EjecucionRetencion e JOIN FETCH e.politica ORDER BY e.ejecutadaEn DESC")
    List<EjecucionRetencion> recientes(Pageable pageable);
}
