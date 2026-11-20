package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "atencion_cierres")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtencionCierre {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_id", nullable = false, unique = true)
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cerrado_por_id", nullable = false)
    private Usuario cerradoPor;

    @Column(name = "cerrado_en", nullable = false)
    @Builder.Default
    private LocalDateTime cerradoEn = LocalDateTime.now();

    @Column(name = "contenido_canonico", nullable = false, columnDefinition = "text")
    private String contenidoCanonico;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(nullable = false, columnDefinition = "text")
    private String firma;

    @Column(name = "firma_algoritmo", nullable = false, length = 20)
    private String firmaAlgoritmo;

    @Column(name = "certificado_huella", nullable = false, length = 64)
    private String certificadoHuella;

    @Column(name = "certificado_sujeto", nullable = false, length = 300)
    private String certificadoSujeto;

    @Column(name = "certificado_der", nullable = false, columnDefinition = "text")
    private String certificadoDer;

    @Column(name = "certificado_de_prueba", nullable = false)
    private boolean certificadoDePrueba;
}
