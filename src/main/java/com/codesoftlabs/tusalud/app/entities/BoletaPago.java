package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.RegimenLaboral;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.SistemaPensionario;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "boletas_pago")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoletaPago {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planilla_id", nullable = false)
    private Planilla planilla;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrato_id", nullable = false)
    private ContratoLaboral contrato;

    @Column(name = "numero", nullable = false, length = 24)
    private String numero;

    @Column(name = "empleado_nombre", nullable = false, length = 200)
    private String empleadoNombre;

    @Column(name = "cargo", nullable = false, length = 100)
    private String cargo;

    @Enumerated(EnumType.STRING)
    @Column(name = "regimen", nullable = false, length = 20)
    private RegimenLaboral regimen;

    @Enumerated(EnumType.STRING)
    @Column(name = "sistema_pensionario", nullable = false, length = 5)
    private SistemaPensionario sistemaPensionario;

    @Column(name = "banco", length = 60)
    private String banco;

    @Column(name = "cuenta", length = 30)
    private String cuenta;

    @Column(name = "dias_laborados", nullable = false)
    private int diasLaborados;

    @Column(name = "dias_falta", nullable = false)
    private int diasFalta;

    @Column(name = "dias_no_remunerados", nullable = false)
    private int diasNoRemunerados;

    @Column(name = "minutos_tardanza", nullable = false)
    private int minutosTardanza;

    @Column(name = "minutos_extra_pagados", nullable = false)
    private int minutosExtraPagados;

    @Column(name = "remuneracion_basica", nullable = false, precision = 12, scale = 2)
    private BigDecimal remuneracionBasica;

    @Column(name = "total_ingresos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalIngresos;

    @Column(name = "total_descuentos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalDescuentos;

    @Column(name = "renta_quinta", nullable = false, precision = 12, scale = 2)
    private BigDecimal rentaQuinta;

    @Column(name = "pension", nullable = false, precision = 12, scale = 2)
    private BigDecimal pension;

    @Column(name = "aporte_empleador", nullable = false, precision = 12, scale = 2)
    private BigDecimal aporteEmpleador;

    @Column(name = "neto", nullable = false, precision = 12, scale = 2)
    private BigDecimal neto;

    @Column(name = "provision_gratificacion", nullable = false, precision = 12, scale = 2)
    private BigDecimal provisionGratificacion;

    @Column(name = "provision_cts", nullable = false, precision = 12, scale = 2)
    private BigDecimal provisionCts;

    @Column(name = "provision_vacaciones", nullable = false, precision = 12, scale = 2)
    private BigDecimal provisionVacaciones;

    @Column(name = "hash", length = 64)
    private String hash;

    @OneToMany(mappedBy = "boleta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Builder.Default
    private List<BoletaLinea> lineas = new ArrayList<>();
}
