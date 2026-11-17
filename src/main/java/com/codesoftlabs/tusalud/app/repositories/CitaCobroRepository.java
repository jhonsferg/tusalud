package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.Cita;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CitaCobroRepository extends JpaRepository<Cita, UUID> {

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.paciente pa JOIN FETCH pa.usuario
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            JOIN FETCH c.especialidad
            LEFT JOIN FETCH c.pago
            WHERE c.id = :id
            """)
    Optional<Cita> findParaCobro(@Param("id") UUID id);

    @Query("""
            SELECT c FROM Cita c
            JOIN FETCH c.paciente pa JOIN FETCH pa.usuario pu
            JOIN FETCH c.medico m JOIN FETCH m.usuario
            JOIN FETCH c.especialidad
            LEFT JOIN c.pago p
            WHERE c.estado IN (
                com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.PENDIENTE,
                com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.CONFIRMADA,
                com.codesoftlabs.tusalud.app.entities.enums.EstadoCita.ATENDIDA)
              AND (p IS NULL OR p.estado = com.codesoftlabs.tusalud.app.entities.enums.EstadoPago.REEMBOLSADO)
              AND (:q IS NULL OR LOWER(pu.nombre) LIKE :q OR LOWER(pu.apellido) LIKE :q OR pa.dni LIKE :q)
            ORDER BY c.fechaHora DESC
            """)
    List<Cita> pendientesDeCobro(@Param("q") String q, Pageable pageable);
}
