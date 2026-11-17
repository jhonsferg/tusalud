package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaItem {

    @Column(nullable = false, length = 150)
    private String medicamento;

    @Column(length = 100)
    private String dosis;

    @Column(length = 100)
    private String frecuencia;

    @Column(length = 100)
    private String duracion;

    @Column(nullable = false)
    private boolean dispensado;
}
