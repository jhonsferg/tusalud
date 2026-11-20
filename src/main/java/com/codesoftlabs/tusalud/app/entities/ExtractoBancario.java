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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "extractos_bancarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExtractoBancario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaTesoreria cuenta;

    @Column(nullable = false, length = 120)
    private String archivo;

    private LocalDate desde;

    private LocalDate hasta;

    @Column(nullable = false)
    private int lineas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "importado_por_id", nullable = false)
    private Usuario importadoPor;

    @Column(name = "importado_en", nullable = false)
    private LocalDateTime importadoEn;
}
