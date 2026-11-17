package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoIngresoHosp;
import com.codesoftlabs.tusalud.app.entities.enums.TipoIngresoHosp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "ingresos_hospitalarios",
        uniqueConstraints = @UniqueConstraint(name = "uk_ingresos_hosp_codigo", columnNames = "codigo"),
        indexes = {
            @Index(name = "idx_ingresos_hosp_estado_fecha", columnList = "estado, fecha_ingreso"),
            @Index(name = "idx_ingresos_hosp_paciente", columnList = "paciente_id"),
            @Index(name = "idx_ingresos_hosp_cama", columnList = "cama_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IngresoHospitalario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoIngresoHosp tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoIngresoHosp estado = EstadoIngresoHosp.ACTIVO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cama_id")
    private Cama cama;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(name = "diagnostico_ingreso", length = 300)
    private String diagnosticoIngreso;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso;

    @Column(name = "fecha_alta")
    private LocalDateTime fechaAlta;

    @Column(name = "diagnostico_alta", length = 300)
    private String diagnosticoAlta;

    @Column(name = "indicaciones_alta", length = 500)
    private String indicacionesAlta;

    @Column(name = "cargo_acumulado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal cargoAcumulado = BigDecimal.ZERO;

    @Column(name = "desde_en_cama")
    private LocalDateTime desdeEnCama;

    @Column(name = "triaje_nivel")
    private Integer triajeNivel;

    @Column(name = "triaje_presion_arterial", length = 20)
    private String triajePresionArterial;

    @Column(name = "triaje_frecuencia_cardiaca")
    private Integer triajeFrecuenciaCardiaca;

    @Column(name = "triaje_frecuencia_respiratoria")
    private Integer triajeFrecuenciaRespiratoria;

    @Column(name = "triaje_temperatura", precision = 4, scale = 1)
    private BigDecimal triajeTemperatura;

    @Column(name = "triaje_saturacion_o2")
    private Integer triajeSaturacionO2;

    @Column(name = "triaje_glasgow")
    private Integer triajeGlasgow;

    @Column(name = "triaje_motivo_consulta", length = 300)
    private String triajeMotivoConsulta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "triaje_registrado_por_id")
    private Usuario triajeRegistradoPor;

    @Column(name = "triaje_fecha")
    private LocalDateTime triajeFecha;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean tieneTriaje() {
        return triajeNivel != null;
    }
}
