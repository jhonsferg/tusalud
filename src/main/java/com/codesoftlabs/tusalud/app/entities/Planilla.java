package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoPlanilla;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "planillas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Planilla {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "anio", nullable = false)
    private int anio;

    @Column(name = "mes", nullable = false)
    private int mes;

    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoPlanilla estado;

    @Column(name = "cantidad_boletas", nullable = false)
    private int cantidadBoletas;

    @Column(name = "total_ingresos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalIngresos;

    @Column(name = "total_descuentos", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(name = "total_neto", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalNeto;

    @Column(name = "total_aporte_empleador", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAporteEmpleador;

    @Column(name = "total_renta", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalRenta;

    @Column(name = "total_pension", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalPension;

    @Column(name = "hash", length = 64)
    private String hash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculada_por_id")
    private Usuario calculadaPor;

    @Column(name = "calculada_en")
    private LocalDateTime calculadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobada_por_id")
    private Usuario aprobadaPor;

    @Column(name = "aprobada_en")
    private LocalDateTime aprobadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pagada_por_id")
    private Usuario pagadaPor;

    @Column(name = "pagada_en")
    private LocalDateTime pagadaEn;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "referencia_pago", length = 60)
    private String referenciaPago;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrada_por_id")
    private Usuario cerradaPor;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
