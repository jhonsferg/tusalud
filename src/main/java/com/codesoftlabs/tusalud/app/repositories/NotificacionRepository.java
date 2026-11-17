package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Notificacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, UUID> {

    List<Notificacion> findByUsuarioIdOrderByCreatedAtDescIdDesc(UUID usuarioId, Pageable pageable);

    List<Notificacion> findByUsuarioIdAndLeidaFalseOrderByCreatedAtDescIdDesc(UUID usuarioId, Pageable pageable);

    long countByUsuarioIdAndLeidaFalse(UUID usuarioId);

    Optional<Notificacion> findByIdAndUsuarioId(UUID id, UUID usuarioId);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.id = :usuarioId AND n.leida = false")
    int marcarTodasComoLeidas(@Param("usuarioId") UUID usuarioId);

    boolean existsByUsuarioIdAndTipoAndCreatedAtGreaterThanEqual(
            UUID usuarioId,
            com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion tipo,
            java.time.LocalDateTime desde);
}
