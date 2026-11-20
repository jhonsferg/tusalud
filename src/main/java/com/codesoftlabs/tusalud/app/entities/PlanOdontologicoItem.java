package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoItemPlan;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.Superficie;
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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plan_odontologico_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanOdontologicoItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanOdontologico plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "procedimiento_id", nullable = false)
    private ProcedimientoOdontologico procedimiento;

    @Column
    private Short pieza;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Superficie superficie;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoItemPlan estado =
            com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoItemPlan.PENDIENTE;

    @Column(nullable = false)
    private int posicion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "consentimiento_id")
    private Consentimiento consentimiento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id")
    private SesionOdontologica sesion;

    @Column(name = "realizado_en")
    private LocalDateTime realizadoEn;

    @Column(length = 300)
    private String observacion;
}
