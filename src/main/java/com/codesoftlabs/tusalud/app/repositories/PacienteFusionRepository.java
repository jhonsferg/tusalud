package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.PacienteFusion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PacienteFusionRepository extends JpaRepository<PacienteFusion, UUID> {

    @Query("""
            SELECT f FROM PacienteFusion f JOIN FETCH f.destino d JOIN FETCH d.usuario JOIN FETCH f.realizadoPor
            ORDER BY f.realizadoEn DESC, f.id DESC
            """)
    List<PacienteFusion> recientes();
}
