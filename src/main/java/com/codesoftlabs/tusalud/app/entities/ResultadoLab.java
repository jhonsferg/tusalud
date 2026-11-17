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
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resultados_lab")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultadoLab {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private ItemOrdenLab item;

    @Column(name = "parametro_id")
    private Long parametroId;

    @Column(name = "parametro_nombre", nullable = false, length = 100)
    private String parametroNombre;

    @Column(length = 20)
    private String unidad;

    @Column(nullable = false, length = 100)
    private String valor;

    @Column(name = "valor_min", precision = 12, scale = 3)
    private BigDecimal valorMin;

    @Column(name = "valor_max", precision = 12, scale = 3)
    private BigDecimal valorMax;

    @Column(name = "fuera_de_rango", nullable = false)
    private boolean fueraDeRango;
}
