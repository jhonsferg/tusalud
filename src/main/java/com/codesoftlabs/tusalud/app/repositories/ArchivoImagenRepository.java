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

    record Metadatos(UUID id, String nombre, String tipoContenido, long tamano, LocalDateTime subidoEn) {}

    @Query("""
            SELECT new com.codesoftlabs.tusalud.app.repositories.ArchivoImagenRepository$Metadatos(
                a.id, a.archivo.nombreOriginal, a.archivo.tipoContenido, a.archivo.tamano, a.archivo.createdAt)
            FROM ArchivoImagen a
            WHERE a.orden.id = :ordenId
            ORDER BY a.posicion ASC
            """)
    List<Metadatos> metadatosDeOrden(@Param("ordenId") UUID ordenId);

    @Query("SELECT COUNT(a) FROM ArchivoImagen a WHERE a.orden.id = :ordenId")
    long contarDeOrden(@Param("ordenId") UUID ordenId);

    @Query("SELECT COALESCE(MAX(a.posicion), -1) FROM ArchivoImagen a WHERE a.orden.id = :ordenId")
    int ultimaPosicion(@Param("ordenId") UUID ordenId);

    @Query("""
            SELECT a FROM ArchivoImagen a JOIN FETCH a.archivo
            WHERE a.id = :id AND a.orden.id = :ordenId
            """)
    Optional<ArchivoImagen> findDeOrden(@Param("id") UUID id, @Param("ordenId") UUID ordenId);

    @Query("SELECT a FROM ArchivoImagen a JOIN FETCH a.orden WHERE a.archivo.id = :archivoId")
    Optional<ArchivoImagen> findPorArchivo(@Param("archivoId") UUID archivoId);
}
