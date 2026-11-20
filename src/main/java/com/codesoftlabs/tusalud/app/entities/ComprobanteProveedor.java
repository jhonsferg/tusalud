package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoComprobanteProveedor;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.TipoComprobanteProveedor;
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
        name = "comprobantes_proveedor",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_comprobantes_proveedor_documento",
                        columnNames = {"proveedor_id", "tipo", "serie", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComprobanteProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "orden_id")
    private OrdenCompra orden;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoComprobanteProveedor tipo;

    @Column(nullable = false, length = 10)
    private String serie;

    @Column(nullable = false, length = 20)
    private String numero;

    @Column(name = "ruc_emisor", nullable = false, length = 11)
    private String rucEmisor;

    @Column(name = "fecha_emision", nullable = false)
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igv;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoPagado = BigDecimal.ZERO;

    @Column(name = "monto_credito", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal montoCredito = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoComprobanteProveedor estado;

    @Column(length = 300)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "registrado_en", nullable = false)
    private LocalDateTime registradoEn;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    public BigDecimal saldo() {
        return total.subtract(montoPagado).subtract(montoCredito);
    }

    public String numeroCompleto() {
        return serie + "-" + numero;
    }

    public boolean vencido(LocalDate hoy) {
        return estado != EstadoComprobanteProveedor.ANULADO
                && estado != EstadoComprobanteProveedor.PAGADO
                && fechaVencimiento.isBefore(hoy);
    }
}
