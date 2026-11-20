package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "receta_electronica_items",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_receta_items_posicion",
                        columnNames = {"receta_id", "posicion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaElectronicaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_id", nullable = false)
    private RecetaElectronica receta;

    @Column(nullable = false)
    private int posicion;

    @Column(name = "atencion_posicion")
    private Integer atencionPosicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medicamento_id")
    private Medicamento medicamento;

    @Column(name = "medicamento_texto", nullable = false, length = 200)
    private String medicamentoTexto;

    @Column(length = 100)
    private String dosis;

    @Column(length = 100)
    private String frecuencia;

    @Column(length = 100)
    private String duracion;

    @Column(length = 30)
    private String via;

    @Column(name = "dosis_unitaria_mg", precision = 10, scale = 2)
    private BigDecimal dosisUnitariaMg;

    @Column(name = "veces_por_dia")
    private Integer vecesPorDia;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "cantidad_dispensada", nullable = false)
    @Builder.Default
    private int cantidadDispensada = 0;

    @Column(name = "control_especial", nullable = false)
    private boolean controlEspecial;

    public int pendiente() {
        return Math.max(0, cantidad - cantidadDispensada);
    }
}
