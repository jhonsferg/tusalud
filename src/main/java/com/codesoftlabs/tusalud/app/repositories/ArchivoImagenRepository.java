package com.codesoftlabs.tusalud.app.repositories;

import com.codesoftlabs.tusalud.app.entities.ArchivoImagen;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ArchivoImagenRepository extends JpaRepository<ArchivoImagen, UUID> {

    record Metadatos(UUID id, String nombre, String tipoContenido, int tamano, LocalDateTime subidoEn) {}

    @Query("""
            SELECT new com.codesoftlabs.tusalud.app.repositories.ArchivoImagenRepository$Metadatos(
                a.id, a.nombre, a.tipoContenido, a.tamano, a.subidoEn)
            FROM ArchivoImagen a
            WHERE a.orden.id = :ordenId
            ORDER BY a.posicion ASC
            """)
    List<Metadatos> metadatosDeOrden(@Param("ordenId") UUID ordenId);

    @Query("SELECT COUNT(a) FROM ArchivoImagen a WHERE a.orden.id = :ordenId")
    long contarDeOrden(@Param("ordenId") UUID ordenId);

    @Query("SELECT COALESCE(MAX(a.posicion), -1) FROM ArchivoImagen a WHERE a.orden.id = :ordenId")
    int ultimaPosicion(@Param("ordenId") UUID ordenId);

    @Query("SELECT a FROM ArchivoImagen a WHERE a.id = :id AND a.orden.id = :ordenId")
    Optional<ArchivoImagen> findDeOrden(@Param("id") UUID id, @Param("ordenId") UUID ordenId);
}
