package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.CirugiaEquipo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CirugiaEquipoRepository extends JpaRepository<CirugiaEquipo, UUID> {

    @Query(
            "SELECT e FROM CirugiaEquipo e JOIN FETCH e.usuario WHERE e.cirugia.id = :cirugiaId ORDER BY e.rol, e.asignadoEn")
    List<CirugiaEquipo> deCirugia(@Param("cirugiaId") UUID cirugiaId);

    @Query("SELECT e FROM CirugiaEquipo e JOIN FETCH e.usuario WHERE e.cirugia.id IN :ids")
    List<CirugiaEquipo> deCirugias(@Param("ids") java.util.Collection<UUID> ids);

    boolean existsByCirugiaIdAndUsuarioId(UUID cirugiaId, UUID usuarioId);

    @Query(
            "SELECT COUNT(e) > 0 FROM CirugiaEquipo e WHERE e.cirugia.id = :cirugiaId AND e.usuario.id = :usuarioId AND e.rol = :rol")
    boolean existeRol(
            @Param("cirugiaId") UUID cirugiaId,
            @Param("usuarioId") UUID usuarioId,
            @Param("rol") com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.RolQuirurgico rol);

    void deleteByCirugiaId(UUID cirugiaId);
}
