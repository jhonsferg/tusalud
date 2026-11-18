package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPreautorizacion;
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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "preautorizaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_preautorizaciones_codigo", columnNames = "codigo"),
        indexes = {
            @Index(name = "idx_preautorizaciones_paciente", columnList = "paciente_id"),
            @Index(name = "idx_preautorizaciones_estado", columnList = "estado")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Preautorizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    private Poliza poliza;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaServicio categoria;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(name = "monto_estimado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEstimado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoPreautorizacion estado = EstadoPreautorizacion.SOLICITADA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitada_por", nullable = false)
    private Usuario solicitadaPor;

    @Column(name = "solicitada_en", nullable = false)
    private LocalDateTime solicitadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gestionada_por")
    private Usuario gestionadaPor;

    @Column(name = "gestionada_en")
    private LocalDateTime gestionadaEn;

    @Column(name = "codigo_autorizacion", length = 30)
    private String codigoAutorizacion;

    @Column(name = "monto_aprobado", precision = 12, scale = 2)
    private BigDecimal montoAprobado;

    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    public EstadoPreautorizacion estadoEn(LocalDate fecha) {
        if (estado == EstadoPreautorizacion.APROBADA && vigenteHasta != null && fecha.isAfter(vigenteHasta)) {
            return EstadoPreautorizacion.VENCIDA;
        }
        return estado;
    }
}
