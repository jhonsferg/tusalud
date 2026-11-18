package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoCuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.enums.OrigenComprobante;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(
        name = "cuentas_aseguradora",
        indexes = {
            @Index(name = "idx_cuentas_aseguradora_estado", columnList = "aseguradora_id, estado"),
            @Index(name = "idx_cuentas_aseguradora_consumo", columnList = "paciente_id, convenio_id, fecha")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CuentaAseguradora {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aseguradora_id", nullable = false)
    private Aseguradora aseguradora;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convenio_id", nullable = false)
    private Convenio convenio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id")
    private Comprobante comprobante;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaServicio categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenComprobante origen;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotal;

    @Column(name = "monto_cubierto", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoCubierto;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal copago;

    @Column(name = "poliza_numero", nullable = false, length = 30)
    private String polizaNumero;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoCuentaAseguradora estado = EstadoCuentaAseguradora.PENDIENTE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "liquidacion_id")
    private Liquidacion liquidacion;
}
