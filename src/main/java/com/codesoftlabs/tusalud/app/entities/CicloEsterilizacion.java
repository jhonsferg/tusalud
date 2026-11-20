package com.codesoftlabs.tusalud.app.entities;

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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ciclos_esterilizacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CicloEsterilizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "esterilizador_id", nullable = false)
    private Esterilizador esterilizador;

    @Column(name = "iniciado_en", nullable = false)
    private LocalDateTime iniciadoEn;

    @Column(name = "temperatura_c", nullable = false, precision = 5, scale = 1)
    private java.math.BigDecimal temperaturaC;

    @Column(name = "presion_kpa", precision = 6, scale = 1)
    private java.math.BigDecimal presionKpa;

    @Column(name = "duracion_min", nullable = false)
    private int duracionMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicador_quimico", nullable = false, length = 15)
    @Builder.Default
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador indicadorQuimico =
            com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicador_biologico", nullable = false, length = 15)
    @Builder.Default
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador indicadorBiologico =
            com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoIndicador.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoCiclo resultado =
            com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.ResultadoCiclo.EN_PROCESO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsable_id", nullable = false)
    private Usuario responsable;

    @Column(length = 300)
    private String observaciones;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
