package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.IndicadorCrecimiento;
import com.codesoftlabs.tusalud.app.entities.enums.SexoPaciente;
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
@Table(name = "referencia_crecimiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenciaCrecimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private IndicadorCrecimiento indicador;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private SexoPaciente sexo;

    @Column(name = "edad_meses", nullable = false)
    private int edadMeses;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal l;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal m;

    @Column(nullable = false, precision = 8, scale = 5)
    private BigDecimal s;

    @Column(nullable = false, length = 80)
    private String fuente;

    @Column(name = "por_validar", nullable = false)
    @Builder.Default
    private boolean porValidar = true;
}
