package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.familia = :familia AND r.revocado = false")
    int revocarFamilia(@Param("familia") UUID familia);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.sesion.id = :sesionId AND r.revocado = false")
    int revocarDeSesion(@Param("sesionId") UUID sesionId);

    @Modifying
    @Query("UPDATE RefreshToken r SET r.revocado = true WHERE r.usuario.id = :usuarioId AND r.revocado = false")
    int revocarDeUsuario(@Param("usuarioId") UUID usuarioId);

    @Modifying
    @Query("""
            UPDATE RefreshToken r SET r.revocado = true
            WHERE r.usuario.id = :usuarioId AND r.revocado = false AND r.sesion.tokenId <> :tokenActual
            """)
    int revocarDeUsuarioExcepto(@Param("usuarioId") UUID usuarioId, @Param("tokenActual") UUID tokenActual);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiraEn < :limite")
    int eliminarExpirados(@Param("limite") LocalDateTime limite);
}
