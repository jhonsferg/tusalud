package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "consentimiento_plantillas",
        uniqueConstraints = @UniqueConstraint(name = "uk_consentimiento_plantillas_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentimientoPlantilla {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, columnDefinition = "text")
    private String texto;

    @Column(nullable = false, columnDefinition = "text")
    private String riesgos;

    @Column(nullable = false, columnDefinition = "text")
    private String beneficios;

    @Column(nullable = false, columnDefinition = "text")
    private String alternativas;

    @Column(name = "requiere_testigo", nullable = false)
    private boolean requiereTestigo;

    @Column(name = "vigencia_dias", nullable = false)
    @Builder.Default
    private int vigenciaDias = 30;

    @Column(nullable = false)
    @Builder.Default
    private int version = 1;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;
}
