package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Preautorizacion;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PreautorizacionRepository extends JpaRepository<Preautorizacion, UUID> {

    String DETALLE = """
            SELECT p FROM Preautorizacion p
            JOIN FETCH p.paciente pa
            JOIN FETCH pa.usuario
            JOIN FETCH p.poliza po
            JOIN FETCH po.convenio c
            JOIN FETCH c.aseguradora
            JOIN FETCH p.solicitadaPor
            LEFT JOIN FETCH p.gestionadaPor
            """;

    @Query(DETALLE + " WHERE p.id = :id")
    Optional<Preautorizacion> findConDetalle(@Param("id") UUID id);

    @Query(DETALLE + " WHERE p.solicitadaPor.id = :usuarioId ORDER BY p.solicitadaEn DESC")
    List<Preautorizacion> deSolicitante(@Param("usuarioId") UUID usuarioId);

    @Query(DETALLE + " WHERE pa.usuario.id = :usuarioId ORDER BY p.solicitadaEn DESC")
    List<Preautorizacion> deLUsuario(@Param("usuarioId") UUID usuarioId);

    @Query(DETALLE + " ORDER BY p.solicitadaEn DESC")
    List<Preautorizacion> todas();

    @Query(DETALLE + " WHERE p.estado = :estado ORDER BY p.solicitadaEn DESC")
    List<Preautorizacion> porEstado(@Param("estado") EstadoPreautorizacion estado);

    @Query("""
            SELECT p FROM Preautorizacion p
            WHERE p.paciente.id = :pacienteId
              AND p.categoria = :categoria
              AND p.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion.APROBADA
              AND p.vigenteHasta >= :hoy
            """)
    List<Preautorizacion> aprobadasVigentes(
            @Param("pacienteId") UUID pacienteId,
            @Param("categoria") CategoriaServicio categoria,
            @Param("hoy") LocalDate hoy);

    @Query("""
            SELECT p FROM Preautorizacion p
            WHERE p.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion.APROBADA
              AND p.vigenteHasta < :hoy
            """)
    List<Preautorizacion> vencidas(@Param("hoy") LocalDate hoy);

    long countByEstado(EstadoPreautorizacion estado);

    @Query("""
            SELECT COUNT(p) FROM Preautorizacion p
            WHERE p.paciente.id = :pacienteId AND p.categoria = :categoria AND p.referenciaId = :referenciaId
              AND p.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion.SOLICITADA
            """)
    long solicitudesAbiertas(
            @Param("pacienteId") UUID pacienteId,
            @Param("categoria") CategoriaServicio categoria,
            @Param("referenciaId") UUID referenciaId);
}
