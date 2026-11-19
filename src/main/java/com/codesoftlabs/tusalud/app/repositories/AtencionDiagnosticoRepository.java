package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.AtencionDiagnostico;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AtencionDiagnosticoRepository extends JpaRepository<AtencionDiagnostico, UUID> {

    interface FrecuenteMedico {
        String getCodigo();

        String getDescripcion();

        long getCasos();
    }

    interface FilaMorbilidad {
        String getCodigo();

        String getDescripcion();

        String getCapitulo();

        String getEspecialidad();

        LocalDate getNacimiento();

        SexoPaciente getSexo();

        LocalDateTime getFecha();
    }

    @Query("""
            SELECT d.codigo AS codigo, d.descripcion AS descripcion, COUNT(d) AS casos
            FROM AtencionDiagnostico d JOIN d.atencion a
            WHERE a.medico.usuario.id = :usuarioId AND a.fecha >= :desde
            GROUP BY d.codigo, d.descripcion
            ORDER BY COUNT(d) DESC, d.codigo
            """)
    List<FrecuenteMedico> frecuentesDelMedico(
            @Param("usuarioId") UUID usuarioId, @Param("desde") LocalDateTime desde, Pageable pageable);

    @Query("""
            SELECT d.codigo AS codigo, d.descripcion AS descripcion, c.capitulo AS capitulo,
                   e.nombre AS especialidad, p.fechaNacimiento AS nacimiento, p.sexo AS sexo, a.fecha AS fecha
            FROM AtencionDiagnostico d
            JOIN d.atencion a
            JOIN a.cita ci
            JOIN ci.especialidad e
            JOIN a.paciente p
            JOIN d.cie10 c
            WHERE d.principal = true AND a.fecha >= :desde AND a.fecha < :hasta
            """)
    List<FilaMorbilidad> filasPrincipales(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
