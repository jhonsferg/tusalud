package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza;
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
        name = "polizas",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_polizas_numero_convenio",
                        columnNames = {"numero", "convenio_id"}),
        indexes = @Index(name = "idx_polizas_paciente", columnList = "paciente_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Poliza {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "convenio_id", nullable = false)
    private Convenio convenio;

    @Column(nullable = false, length = 30)
    private String numero;

    @Column(name = "titular_nombre", nullable = false, length = 120)
    private String titularNombre;

    @Column(nullable = false, length = 30)
    private String parentesco;

    @Column(length = 60)
    private String plan;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta", nullable = false)
    private LocalDate vigenciaHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoPoliza estado = EstadoPoliza.ACTIVA;

    @Column(name = "motivo_suspension", length = 300)
    private String motivoSuspension;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrada_por")
    private Usuario registradaPor;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    public EstadoPoliza estadoEn(LocalDate fecha) {
        if (estado == EstadoPoliza.SUSPENDIDA) {
            return EstadoPoliza.SUSPENDIDA;
        }
        return fecha.isAfter(vigenciaHasta) ? EstadoPoliza.VENCIDA : EstadoPoliza.ACTIVA;
    }

    public boolean usableEn(LocalDate fecha) {
        return estadoEn(fecha) == EstadoPoliza.ACTIVA && !fecha.isBefore(vigenciaDesde) && convenio.vigenteEn(fecha);
    }
}
