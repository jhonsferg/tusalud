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
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cirugias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cirugia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Medico solicitante;

    @Column(name = "cie10_id")
    private UUID cie10Id;

    @Column(name = "diagnostico_codigo", nullable = false, length = 20)
    private String diagnosticoCodigo;

    @Column(name = "diagnostico_descripcion", nullable = false, length = 300)
    private String diagnosticoDescripcion;

    @Column(nullable = false, length = 200)
    private String procedimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoCirugia tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia estado =
            com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoCirugia.SOLICITADA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quirofano_id")
    private Quirofano quirofano;

    @Column(name = "inicio_programado")
    private LocalDateTime inicioProgramado;

    @Column(name = "fin_programado")
    private LocalDateTime finProgramado;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_anestesia", nullable = false, length = 15)
    @Builder.Default
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoAnestesia tipoAnestesia =
            com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.TipoAnestesia.GENERAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingreso_id")
    private IngresoHospitalario ingreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consentimiento_quirurgico_id")
    private Consentimiento consentimientoQuirurgico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consentimiento_anestesico_id")
    private Consentimiento consentimientoAnestesico;

    @Column(name = "indicaciones_preoperatorias", length = 500)
    private String indicacionesPreoperatorias;

    @Column(length = 300)
    private String motivo;

    @Column(name = "inicio_real")
    private LocalDateTime inicioReal;

    @Column(name = "fin_real")
    private LocalDateTime finReal;

    @Column(columnDefinition = "text")
    private String hallazgos;

    @Column(columnDefinition = "text")
    private String tecnica;

    @Column(columnDefinition = "text")
    private String complicaciones;

    @Column(name = "sangrado_ml")
    private Integer sangradoMl;

    @Column(name = "diagnostico_postoperatorio", length = 300)
    private String diagnosticoPostoperatorio;

    @Column(name = "indicaciones_postoperatorias", length = 500)
    private String indicacionesPostoperatorias;

    @Column(name = "nota_registrada_en")
    private LocalDateTime notaRegistradaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nota_por_id")
    private Usuario notaPor;

    @Column()
    private Short asa;

    @Column(name = "anestesia_inicio")
    private LocalDateTime anestesiaInicio;

    @Column(name = "anestesia_fin")
    private LocalDateTime anestesiaFin;

    @Column(name = "anestesia_observaciones", columnDefinition = "text")
    private String anestesiaObservaciones;

    @Column(name = "anestesia_registrada_en")
    private LocalDateTime anestesiaRegistradaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anestesia_por_id")
    private Usuario anestesiaPor;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    @Version
    private long version;
}
