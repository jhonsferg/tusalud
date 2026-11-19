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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ejecuciones_retencion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EjecucionRetencion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "politica_id", nullable = false)
    private PoliticaRetencion politica;

    @Column(name = "ejecutada_en", nullable = false)
    private LocalDateTime ejecutadaEn;

    @Column(nullable = false)
    private int afectados;

    @Column(length = 400)
    private String detalle;
}
