package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.TipoParametro;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parametros_examen")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametroExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "examen_id", nullable = false)
    private ExamenLab examen;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 20)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoParametro tipo;

    @Column(name = "valor_min", precision = 12, scale = 3)
    private BigDecimal valorMin;

    @Column(name = "valor_max", precision = 12, scale = 3)
    private BigDecimal valorMax;

    @Column(nullable = false)
    private int orden;
}
