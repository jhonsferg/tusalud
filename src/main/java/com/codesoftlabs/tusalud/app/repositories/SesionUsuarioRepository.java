package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.SesionUsuario;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, UUID> {

    Optional<SesionUsuario> findByTokenId(UUID tokenId);

    Optional<SesionUsuario> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    boolean existsByUsuarioId(UUID usuarioId);

    List<SesionUsuario> findByUsuarioIdAndRevocadaFalseAndExpiraEnAfterOrderByUltimoAccesoDesc(
            UUID usuarioId, LocalDateTime ahora);

    @Modifying
    @Query("UPDATE SesionUsuario s SET s.revocada = true WHERE s.usuario.id = :usuarioId AND s.revocada = false")
    int revocarTodasDelUsuario(@Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query("""
            UPDATE SesionUsuario s SET s.revocada = true
            WHERE s.usuario.id = :usuarioId AND s.revocada = false AND s.tokenId <> :tokenActual
            """)
    int revocarTodasExcepto(@Param("usuarioId") UUID usuarioId, @Param("tokenActual") UUID tokenActual);

    @Query("SELECT s.usuario.id, MAX(s.ultimoAcceso) FROM SesionUsuario s GROUP BY s.usuario.id")
    List<Object[]> findUltimoAccesoPorUsuario();
}
