package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ConsentimientoTratamiento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsentimientoTratamientoRepository extends JpaRepository<ConsentimientoTratamiento, UUID> {

    @Query("""
            SELECT c FROM ConsentimientoTratamiento c JOIN FETCH c.finalidad
            WHERE c.usuario.id = :usuarioId
            ORDER BY c.registradoEn DESC, c.id DESC
            """)
    List<ConsentimientoTratamiento> historial(@Param("usuarioId") UUID usuarioId);
}
