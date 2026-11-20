package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Cirugia;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CirugiaRepository extends JpaRepository<Cirugia, UUID> {

    @Query("""
            SELECT c FROM Cirugia c
            JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.solicitante m JOIN FETCH m.usuario
            LEFT JOIN FETCH c.quirofano
            WHERE c.id = :id
            """)
    Optional<Cirugia> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT c FROM Cirugia c
            JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.solicitante m JOIN FETCH m.usuario
            LEFT JOIN FETCH c.quirofano
            WHERE (c.inicioProgramado >= :desde AND c.inicioProgramado < :hasta)
               OR c.estado = com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.SOLICITADA
            ORDER BY c.inicioProgramado, c.creadoEn
            """)
    List<Cirugia> enRango(@Param("desde") java.time.LocalDateTime desde, @Param("hasta") java.time.LocalDateTime hasta);

    @Query("""
            SELECT c FROM Cirugia c
            JOIN FETCH c.paciente p JOIN FETCH p.usuario
            JOIN FETCH c.solicitante m JOIN FETCH m.usuario
            LEFT JOIN FETCH c.quirofano
            WHERE p.id = :pacienteId
            ORDER BY c.creadoEn DESC
            """)
    List<Cirugia> deLaPersona(@Param("pacienteId") UUID pacienteId);

    @Query("""
            SELECT c FROM Cirugia c
            WHERE c.quirofano.id = :quirofanoId
              AND c.id <> :excluirId
              AND c.estado IN (
                  com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.PROGRAMADA,
                  com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.EN_PREPARACION,
                  com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.EN_CURSO)
              AND c.inicioProgramado < :fin AND c.finProgramado > :inicio
            """)
    List<Cirugia> solapadasEnQuirofano(
            @Param("quirofanoId") UUID quirofanoId,
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fin") java.time.LocalDateTime fin,
            @Param("excluirId") UUID excluirId);

    @Query("""
            SELECT DISTINCT c FROM Cirugia c JOIN CirugiaEquipo e ON e.cirugia = c
            WHERE e.usuario.id = :usuarioId
              AND c.id <> :excluirId
              AND c.estado IN (
                  com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.PROGRAMADA,
                  com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.EN_PREPARACION,
                  com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.EN_CURSO)
              AND c.inicioProgramado < :fin AND c.finProgramado > :inicio
            """)
    List<Cirugia> solapadasDeUsuario(
            @Param("usuarioId") UUID usuarioId,
            @Param("inicio") java.time.LocalDateTime inicio,
            @Param("fin") java.time.LocalDateTime fin,
            @Param("excluirId") UUID excluirId);

    @Query("""
            SELECT c FROM Cirugia c
            WHERE c.estado = com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.SUSPENDIDA OR c.estado = com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.SOLICITADA
            ORDER BY c.creadoEn
            """)
    List<Cirugia> pendientesDeProgramar();
}
