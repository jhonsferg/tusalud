package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.Afectacion;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ModoFiscal;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "configuracion_fiscal",
        uniqueConstraints = @UniqueConstraint(name = "uk_configuracion_fiscal_sede", columnNames = "sede"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionFiscal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String sede;

    @Column(nullable = false)
    private boolean habilitada;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 150)
    private String nombreComercial;

    @Column(nullable = false, length = 200)
    private String direccion;

    @Column(nullable = false, length = 6)
    private String ubigeo;

    @Column(nullable = false, length = 60)
    private String departamento;

    @Column(nullable = false, length = 60)
    private String provincia;

    @Column(nullable = false, length = 60)
    private String distrito;

    @Column(name = "codigo_local", nullable = false, length = 4)
    @Builder.Default
    private String codigoLocal = "0000";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ProveedorFiscal proveedor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ModoFiscal modo;

    @Column(name = "url_servicio", length = 300)
    private String urlServicio;

    @Enumerated(EnumType.STRING)
    @Column(name = "afectacion_servicios", nullable = false, length = 10)
    @Builder.Default
    private Afectacion afectacionServicios = Afectacion.GRAVADO;

    @Column(name = "igv_porcentaje", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal igvPorcentaje = new BigDecimal("18.00");

    @Column(name = "boletas_por_resumen", nullable = false)
    private boolean boletasPorResumen;

    @Column(name = "dias_limite_baja", nullable = false)
    @Builder.Default
    private int diasLimiteBaja = 7;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actualizado_por_id")
    private Usuario actualizadoPor;
}
