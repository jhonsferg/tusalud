package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoAsiento;
import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.OrigenAsiento;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
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
@Table(
        name = "asientos",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_asientos_numero", columnNames = "numero"),
            @UniqueConstraint(
                    name = "uk_asientos_origen",
                    columnNames = {"origen", "referencia_id"})
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(length = 20)
    private String numero;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private PeriodoContable periodo;

    @Column(nullable = false, length = 200)
    private String glosa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigenAsiento origen;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 14)
    private EstadoAsiento estado;

    @Column(name = "total_debe", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalDebe = BigDecimal.ZERO;

    @Column(name = "total_haber", nullable = false, precision = 14, scale = 2)
    @Builder.Default
    private BigDecimal totalHaber = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contabilizado_por_id")
    private Usuario contabilizadoPor;

    @Column(name = "contabilizado_en")
    private LocalDateTime contabilizadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revierte_a_id")
    private Asiento revierteA;

    @Column(name = "motivo_reversa", length = 300)
    private String motivoReversa;

    @OneToMany(mappedBy = "asiento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Builder.Default
    private List<AsientoLinea> lineas = new ArrayList<>();

    public void agregarLinea(AsientoLinea linea) {
        linea.setPosicion(lineas.size());
        linea.setAsiento(this);
        lineas.add(linea);
    }

    public void recalcularTotales() {
        totalDebe = lineas.stream().map(AsientoLinea::getDebe).reduce(BigDecimal.ZERO, BigDecimal::add);
        totalHaber = lineas.stream().map(AsientoLinea::getHaber).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean cuadra() {
        return totalDebe.compareTo(totalHaber) == 0;
    }
}
