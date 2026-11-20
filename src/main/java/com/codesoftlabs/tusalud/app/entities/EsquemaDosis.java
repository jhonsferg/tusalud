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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "esquema_dosis")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EsquemaDosis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "esquema_id", nullable = false)
    private EsquemaVacunacion esquema;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vacuna_id", nullable = false)
    private Vacuna vacuna;

    @Column(name = "numero_dosis", nullable = false)
    private int numeroDosis;

    @Column(nullable = false, length = 60)
    private String etiqueta;

    @Column(name = "edad_minima_dias", nullable = false)
    private int edadMinimaDias;

    @Column(name = "edad_recomendada_dias", nullable = false)
    private int edadRecomendadaDias;

    @Column(name = "edad_maxima_dias")
    private Integer edadMaximaDias;

    @Column(name = "intervalo_minimo_dias")
    private Integer intervaloMinimoDias;
}
