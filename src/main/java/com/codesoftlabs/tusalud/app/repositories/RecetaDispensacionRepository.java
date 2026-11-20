package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.RecetaDispensacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecetaDispensacionRepository extends JpaRepository<RecetaDispensacion, UUID> {

    @Query("""
            SELECT d FROM RecetaDispensacion d JOIN FETCH d.item i JOIN FETCH i.receta
            WHERE d.dispensacionId = :dispensacionId AND d.revertida = false
            """)
    List<RecetaDispensacion> vigentesDeLaDispensacion(@Param("dispensacionId") UUID dispensacionId);

    @Query("""
            SELECT d FROM RecetaDispensacion d
            WHERE d.item.receta.id = :recetaId
            ORDER BY d.fecha ASC
            """)
    List<RecetaDispensacion> deLaReceta(@Param("recetaId") UUID recetaId);
}
