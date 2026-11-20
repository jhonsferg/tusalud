package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EventoAccion;
import com.codesoftlabs.tusalud.app.entities.EventoAdverso;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoAdversoRepository extends JpaRepository<EventoAdverso, UUID> {

    @Query("""
            SELECT e FROM EventoAdverso e JOIN FETCH e.notificante
            ORDER BY e.notificadoEn DESC, e.id DESC
            """)
    List<EventoAdverso> todos();

    @Query("""
            SELECT e FROM EventoAdverso e JOIN FETCH e.notificante
            WHERE e.notificante.id = :usuarioId
            ORDER BY e.notificadoEn DESC, e.id DESC
            """)
    List<EventoAdverso> delNotificante(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT e FROM EventoAdverso e JOIN FETCH e.notificante
            LEFT JOIN FETCH e.paciente p LEFT JOIN FETCH p.usuario
            WHERE e.id = :id
            """)
    Optional<EventoAdverso> detalle(@Param("id") UUID id);

    @Query("SELECT COALESCE(MAX(e.numero), 0) FROM EventoAdverso e WHERE e.anio = :anio")
    int ultimoNumero(@Param("anio") int anio);

    @Query("SELECT a FROM EventoAccion a JOIN FETCH a.responsable JOIN FETCH a.evento WHERE a.id = :id")
    Optional<EventoAccion> accion(@Param("id") UUID id);
}
