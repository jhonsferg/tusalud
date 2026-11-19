package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "catalogo_cie10", uniqueConstraints = @UniqueConstraint(name = "uk_cie10_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cie10Catalogo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 8)
    private String codigo;

    @Column(nullable = false, length = 300)
    private String descripcion;

    @Column(name = "texto_busqueda", nullable = false, length = 400)
    private String textoBusqueda;

    @Column(length = 200)
    private String capitulo;

    @Column(length = 160)
    private String grupo;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private SexoPaciente sexo;

    @Column(name = "edad_minima")
    private Integer edadMinima;

    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    @Column(nullable = false)
    private boolean activo;

    @Column(nullable = false)
    private boolean muestra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "version_id")
    private Cie10Version version;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
