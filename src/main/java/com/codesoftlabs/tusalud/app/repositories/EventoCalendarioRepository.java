package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.EventoCalendario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoCalendarioRepository extends JpaRepository<EventoCalendario, UUID> {

    Optional<EventoCalendario> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Query("""
            SELECT e FROM EventoCalendario e
            WHERE e.usuario.id = :usuarioId
              AND e.inicio < :fin
              AND e.fin > :inicio
            ORDER BY e.inicio ASC
            """)
    List<EventoCalendario> findSolapados(
            @Param("usuarioId") UUID usuarioId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT e FROM EventoCalendario e
            WHERE e.usuario.id = :usuarioId
              AND e.tipo = com.codesoftlabs.tusalud.app.entities.enums.TipoEvento.BLOQUEO
              AND e.inicio < :fin
              AND e.fin > :inicio
            ORDER BY e.inicio ASC
            """)
    List<EventoCalendario> findBloqueosSolapados(
            @Param("usuarioId") UUID usuarioId, @Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT e FROM EventoCalendario e
            JOIN FETCH e.usuario
            WHERE e.recordatorioMinutos IS NOT NULL
              AND e.recordatorioEnviado = false
              AND e.inicio > :ahora
              AND e.inicio <= :limite
            """)
    List<EventoCalendario> findRecordatoriosPendientes(
            @Param("ahora") LocalDateTime ahora, @Param("limite") LocalDateTime limite);
}
