package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.IntercambioSunat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IntercambioSunatRepository extends JpaRepository<IntercambioSunat, UUID> {

    List<IntercambioSunat> findByDocumentoIdOrderByCreadoEnAsc(UUID documentoId);

    List<IntercambioSunat> findByResumenIdOrderByCreadoEnAsc(UUID resumenId);

    @Modifying
    @Query("DELETE FROM IntercambioSunat i WHERE i.creadoEn < :corte")
    int depurarAnteriores(@Param("corte") LocalDateTime corte);
}
