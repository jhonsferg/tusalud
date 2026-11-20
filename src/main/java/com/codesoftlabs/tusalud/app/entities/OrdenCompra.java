package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.EstadoOrdenCompra;
import com.codesoftlabs.tusalud.app.entities.enums.ComprasEnums.NivelAprobacion;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ordenes_compra")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenCompra {

    public static final BigDecimal TASA_IGV = new BigDecimal("0.18");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private Integer anio;

    private Integer numero;

    @Column(length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 22)
    private EstadoOrdenCompra estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisicion_id")
    private Requisicion requisicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cotizacion_id")
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creada_por_id", nullable = false)
    private Usuario creadaPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aprobada_por_id")
    private Usuario aprobadaPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel_aprobacion", length = 10)
    private NivelAprobacion nivelAprobacion;

    @Column(name = "afecta_igv", nullable = false)
    @Builder.Default
    private boolean afectaIgv = true;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "condicion_pago_dias", nullable = false)
    private int condicionPagoDias;

    @Column(name = "fecha_entrega_estimada")
    private LocalDate fechaEntregaEstimada;

    @Column(length = 300)
    private String observaciones;

    @Column(length = 64)
    private String hash;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "enviada_en")
    private LocalDateTime enviadaEn;

    @Column(name = "aprobada_en")
    private LocalDateTime aprobadaEn;

    @Column(name = "motivo_resolucion", length = 300)
    private String motivoResolucion;

    @Column(name = "cerrada_en")
    private LocalDateTime cerradaEn;

    @Column(name = "anulada_en")
    private LocalDateTime anuladaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulada_por_id")
    private Usuario anuladaPor;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Builder.Default
    private List<OrdenCompraItem> items = new ArrayList<>();

    public void agregarItem(OrdenCompraItem item) {
        item.setPosicion(items.size());
        item.setOrden(this);
        items.add(item);
    }

    public void limpiarItems() {
        items.clear();
    }

    public void recalcular() {
        BigDecimal suma = items.stream().map(OrdenCompraItem::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        subtotal = suma.setScale(2, RoundingMode.HALF_UP);
        igv = afectaIgv ? subtotal.multiply(TASA_IGV).setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        total = subtotal.add(igv);
    }

    public String codigoVisible() {
        return codigo == null ? "Borrador" : codigo;
    }
}
