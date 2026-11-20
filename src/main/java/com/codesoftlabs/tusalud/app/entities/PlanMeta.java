package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.CronicosEnums.ParametroSeguimiento;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plan_metas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanTratamiento plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private ParametroSeguimiento parametro;

    @Column(name = "valor_min", precision = 8, scale = 2)
    private BigDecimal valorMin;

    @Column(name = "valor_max", precision = 8, scale = 2)
    private BigDecimal valorMax;

    @Column(nullable = false, length = 15)
    private String unidad;
}
