package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.TipoCupon;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "cupones", uniqueConstraints = @UniqueConstraint(name = "uk_cupones_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cupon {

    private static final BigDecimal CIEN = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(length = 255)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoCupon tipo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "usos_maximos")
    private Integer usosMaximos;

    @Column(name = "usos_actuales", nullable = false)
    @Builder.Default
    private int usosActuales = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public String motivoNoVigente(LocalDate hoy) {
        if (!activo) {
            return "El cupón no está activo";
        }
        if (hoy.isBefore(fechaInicio)) {
            return "El cupón aún no está vigente";
        }
        if (hoy.isAfter(fechaFin)) {
            return "El cupón venció el " + fechaFin;
        }
        if (usosMaximos != null && usosActuales >= usosMaximos) {
            return "El cupón ya alcanzó su límite de usos";
        }
        return null;
    }

    public boolean esVigente(LocalDate hoy) {
        return motivoNoVigente(hoy) == null;
    }

    public BigDecimal calcularDescuento(BigDecimal monto) {
        BigDecimal descuento =
                tipo == TipoCupon.PORCENTAJE ? monto.multiply(valor).divide(CIEN, 2, RoundingMode.HALF_UP) : valor;
        return descuento.min(monto).setScale(2, RoundingMode.HALF_UP);
    }
}
