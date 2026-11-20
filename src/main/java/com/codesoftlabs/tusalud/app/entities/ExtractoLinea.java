package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoLineaExtracto;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "extracto_lineas",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_extracto_lineas_huella",
                        columnNames = {"cuenta_id", "huella"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractoLinea {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "extracto_id", nullable = false)
    private ExtractoBancario extracto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaTesoreria cuenta;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(length = 60)
    private String referencia;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 64)
    private String huella;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoLineaExtracto estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movimiento_id")
    private MovimientoTesoreria movimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conciliada_por_id")
    private Usuario conciliadaPor;

    @Column(name = "conciliada_en")
    private LocalDateTime conciliadaEn;
}
