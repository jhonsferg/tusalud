package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Pago;
import java.math.BigDecimal;
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
public interface PagoRepository extends JpaRepository<Pago, UUID> {

    @Query("""
            SELECT p FROM Pago p
            JOIN FETCH p.cita c
            JOIN FETCH c.medico m
            JOIN FETCH m.usuario
            JOIN FETCH c.especialidad
            JOIN FETCH c.paciente pa
            JOIN FETCH pa.usuario
            WHERE p.id = :id
            """)
    Optional<Pago> findByIdConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT p FROM Pago p
            JOIN FETCH p.cita c
            JOIN FETCH c.medico m
            JOIN FETCH m.usuario
            JOIN FETCH c.especialidad
            JOIN FETCH c.paciente pa
            JOIN FETCH pa.usuario
            WHERE pa.usuario.id = :usuarioId
            ORDER BY p.fechaPago DESC, p.id DESC
            """)
    List<Pago> findDelPaciente(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT p FROM Pago p
            JOIN FETCH p.cita c
            JOIN FETCH c.medico m
            JOIN FETCH m.usuario
            JOIN FETCH c.especialidad
            JOIN FETCH c.paciente pa
            JOIN FETCH pa.usuario
            WHERE p.fechaPago >= :inicio AND p.fechaPago < :fin
            ORDER BY p.fechaPago DESC, p.id DESC
            """)
    List<Pago> findEntreFechas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT SUM(p.total) FROM Pago p
            WHERE p.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoPago.PAGADO
              AND p.fechaPago >= :inicio AND p.fechaPago < :fin
            """)
    BigDecimal sumarPagados(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Modifying
    @Query("UPDATE Pago p SET p.metodoPago = null WHERE p.metodoPago.id = :metodoId")
    int desvincularMetodo(@Param("metodoId") UUID metodoId);
}
