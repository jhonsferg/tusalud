package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.IntencionPago;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIntencionPago;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface IntencionPagoRepository extends JpaRepository<IntencionPago, Long> {

    @Query("""
            SELECT i FROM IntencionPago i
            JOIN FETCH i.cita c
            LEFT JOIN FETCH i.pago
            WHERE i.codigo = :codigo AND i.usuario.id = :usuarioId
            """)
    Optional<IntencionPago> findByCodigoAndUsuarioId(
            @Param("codigo") String codigo, @Param("usuarioId") Long usuarioId);

    List<IntencionPago> findByCitaIdAndEstado(Long citaId, EstadoIntencionPago estado);
}
