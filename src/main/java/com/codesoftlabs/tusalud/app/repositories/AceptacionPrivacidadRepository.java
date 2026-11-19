package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AceptacionPrivacidad;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AceptacionPrivacidadRepository extends JpaRepository<AceptacionPrivacidad, UUID> {

    @Query("SELECT a FROM AceptacionPrivacidad a WHERE a.usuario.id = :usuarioId AND a.politica.id = :politicaId")
    Optional<AceptacionPrivacidad> deUsuario(@Param("usuarioId") UUID usuarioId, @Param("politicaId") UUID politicaId);

    @Query("""
            SELECT a FROM AceptacionPrivacidad a JOIN FETCH a.politica
            WHERE a.usuario.id = :usuarioId ORDER BY a.aceptadaEn DESC
            """)
    List<AceptacionPrivacidad> historial(@Param("usuarioId") UUID usuarioId);

    @Query("SELECT COUNT(a) FROM AceptacionPrivacidad a WHERE a.politica.id = :politicaId")
    long contarDePolitica(@Param("politicaId") UUID politicaId);
}
