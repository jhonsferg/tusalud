package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.MfaCodigoRecuperacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MfaCodigoRecuperacionRepository extends JpaRepository<MfaCodigoRecuperacion, UUID> {

    List<MfaCodigoRecuperacion> findByUsuarioIdAndUsadoFalse(UUID usuarioId);

    long countByUsuarioIdAndUsadoFalse(UUID usuarioId);

    @Modifying
    @Query("DELETE FROM MfaCodigoRecuperacion c WHERE c.usuario.id = :usuarioId")
    void eliminarDeUsuario(@Param("usuarioId") UUID usuarioId);
}
