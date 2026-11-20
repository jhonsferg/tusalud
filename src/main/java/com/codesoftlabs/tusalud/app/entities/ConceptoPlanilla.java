package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.ModoConcepto;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.TipoConcepto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conceptos_planilla")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConceptoPlanilla {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "codigo", nullable = false, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoConcepto tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo", nullable = false, length = 25)
    private ModoConcepto modo;

    @Column(name = "valor_defecto", nullable = false, precision = 12, scale = 4)
    private BigDecimal valorDefecto;

    @Column(name = "afecto_renta", nullable = false)
    @Builder.Default
    private boolean afectoRenta = true;

    @Column(name = "afecto_pension", nullable = false)
    @Builder.Default
    private boolean afectoPension = true;

    @Column(name = "afecto_essalud", nullable = false)
    @Builder.Default
    private boolean afectoEssalud = true;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "sistema", nullable = false)
    @Builder.Default
    private boolean sistema = false;

    @Column(name = "orden", nullable = false)
    private int orden;
}
