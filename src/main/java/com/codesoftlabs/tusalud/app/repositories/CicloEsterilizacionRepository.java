package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CicloEsterilizacion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CicloEsterilizacionRepository extends JpaRepository<CicloEsterilizacion, UUID> {

    @Query("""
            SELECT c FROM CicloEsterilizacion c
            JOIN FETCH c.esterilizador JOIN FETCH c.responsable
            WHERE c.id = :id
            """)
    Optional<CicloEsterilizacion> findConDetalle(@Param("id") UUID id);

    @Query("""
            SELECT c FROM CicloEsterilizacion c
            JOIN FETCH c.esterilizador JOIN FETCH c.responsable
            ORDER BY c.iniciadoEn DESC
            """)
    List<CicloEsterilizacion> recientes(org.springframework.data.domain.Pageable pagina);

    @Query("""
            SELECT c FROM CicloEsterilizacion c
            JOIN FETCH c.esterilizador JOIN FETCH c.responsable
            WHERE c.resultado = :resultado AND c.iniciadoEn < :antes
            ORDER BY c.iniciadoEn
            """)
    List<CicloEsterilizacion> enProcesoAntesDe(
            @Param("resultado") com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoCiclo resultado,
            @Param("antes") java.time.LocalDateTime antes);
}
