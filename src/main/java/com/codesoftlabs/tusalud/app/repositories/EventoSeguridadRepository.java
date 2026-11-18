package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EventoSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoSeguridadRepository
        extends JpaRepository<EventoSeguridad, UUID>, JpaSpecificationExecutor<EventoSeguridad> {

    long countByTipoAndUsuarioIdAndCreadoEnAfter(TipoEventoSeguridad tipo, UUID usuarioId, LocalDateTime desde);

    @Query("""
            SELECT e.tipo, e.severidad, COUNT(e) FROM EventoSeguridad e
            WHERE e.creadoEn >= :desde
            GROUP BY e.tipo, e.severidad
            """)
    List<Object[]> contarDesde(@Param("desde") LocalDateTime desde);

    @Query("""
            SELECT e FROM EventoSeguridad e
            LEFT JOIN FETCH e.usuario
            LEFT JOIN FETCH e.actor
            WHERE e.tipo IN :tipos AND e.creadoEn >= :desde
            ORDER BY e.creadoEn DESC
            """)
    List<EventoSeguridad> recientesDeTipos(
            @Param("tipos") Collection<TipoEventoSeguridad> tipos,
            @Param("desde") LocalDateTime desde,
            org.springframework.data.domain.Pageable pageable);

    @Modifying
    @Query("DELETE FROM EventoSeguridad e WHERE e.creadoEn < :limite")
    int eliminarAnteriores(@Param("limite") LocalDateTime limite);
}
