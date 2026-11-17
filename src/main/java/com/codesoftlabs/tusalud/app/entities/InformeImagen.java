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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "informes_imagen",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_informes_imagen_orden_version",
                        columnNames = {"orden_id", "version"}),
        indexes = @Index(name = "idx_informes_imagen_orden", columnList = "orden_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InformeImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "orden_id", nullable = false)
    private OrdenImagen orden;

    @Column(nullable = false)
    private int version;

    @Column(nullable = false, length = 4000)
    private String hallazgos;

    @Column(nullable = false, length = 2000)
    private String conclusion;

    @Column(name = "hallazgo_critico", nullable = false)
    private boolean hallazgoCritico;

    @Column(name = "firma_nombre", nullable = false, length = 150)
    private String firmaNombre;

    @Column(name = "firma_colegiatura", nullable = false, length = 30)
    private String firmaColegiatura;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "autor_id", nullable = false)
    private Usuario autor;

    @Column(name = "motivo_correccion", length = 300)
    private String motivoCorreccion;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
