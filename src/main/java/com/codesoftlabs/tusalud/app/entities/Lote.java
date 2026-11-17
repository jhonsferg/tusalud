package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoLote;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "lotes",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_lotes_producto_numero",
                        columnNames = {"producto_id", "numero_lote"}),
        indexes = @Index(name = "idx_lotes_producto_vencimiento", columnList = "producto_id, fecha_vencimiento"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lote {

    public static final int DIAS_POR_VENCER = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingreso_id")
    private Ingreso ingreso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Proveedor proveedor;

    @Column(name = "numero_lote", nullable = false, length = 40)
    private String numeroLote;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "cantidad_inicial", nullable = false)
    private int cantidadInicial;

    @Column(name = "cantidad_actual", nullable = false)
    private int cantidadActual;

    @Column(name = "costo_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal costoUnitario;

    @Column(length = 60)
    private String ubicacion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean estaVencido(LocalDate hoy) {
        return fechaVencimiento.isBefore(hoy);
    }

    public EstadoLote estado(LocalDate hoy) {
        if (cantidadActual <= 0) {
            return EstadoLote.AGOTADO;
        }
        if (estaVencido(hoy)) {
            return EstadoLote.VENCIDO;
        }
        return ChronoUnit.DAYS.between(hoy, fechaVencimiento) <= DIAS_POR_VENCER
                ? EstadoLote.POR_VENCER
                : EstadoLote.VIGENTE;
    }
}
