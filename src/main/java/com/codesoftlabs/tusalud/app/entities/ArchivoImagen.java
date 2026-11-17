package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "archivos_imagen", indexes = @Index(name = "idx_archivos_imagen_orden", columnList = "orden_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArchivoImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenImagen orden;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "tipo_contenido", nullable = false, length = 30)
    private String tipoContenido;

    @Column(nullable = false)
    private int tamano;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] contenido;

    @Column(nullable = false)
    private int posicion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subido_por", nullable = false)
    private Usuario subidoPor;

    @Column(name = "subido_en", nullable = false)
    private LocalDateTime subidoEn;
}
