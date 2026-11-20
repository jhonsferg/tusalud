package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.QuirofanoEquipo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuirofanoEquipoRepository extends JpaRepository<QuirofanoEquipo, UUID> {

    @Query("SELECT e FROM QuirofanoEquipo e WHERE e.quirofano.id IN :ids ORDER BY e.nombre")
    List<QuirofanoEquipo> deQuirofanos(@Param("ids") java.util.Collection<UUID> ids);

    void deleteByQuirofanoId(UUID quirofanoId);
}
