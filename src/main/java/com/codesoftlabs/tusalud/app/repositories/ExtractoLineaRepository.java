package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ExtractoLinea;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoLineaExtracto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExtractoLineaRepository extends JpaRepository<ExtractoLinea, UUID> {

    boolean existsByCuentaIdAndHuella(UUID cuentaId, String huella);

    @Query("""
            SELECT l FROM ExtractoLinea l
            LEFT JOIN FETCH l.movimiento
            WHERE l.cuenta.id = :cuentaId
              AND (:estado IS NULL OR l.estado = :estado)
            ORDER BY l.fecha DESC, l.id DESC
            """)
    List<ExtractoLinea> deCuenta(@Param("cuentaId") UUID cuentaId, @Param("estado") EstadoLineaExtracto estado);

    long countByCuentaIdAndEstado(UUID cuentaId, EstadoLineaExtracto estado);
}
