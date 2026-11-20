package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.ContabilidadEnums.EstadoPeriodo;
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
        name = "periodos_contables",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_periodos_contables",
                        columnNames = {"anio", "mes"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodoContable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int mes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoPeriodo estado;

    @Column(name = "abierto_en", nullable = false)
    private LocalDateTime abiertoEn;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrado_por_id")
    private Usuario cerradoPor;

    @Column(name = "reabierto_en")
    private LocalDateTime reabiertoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reabierto_por_id")
    private Usuario reabiertoPor;

    @Column(name = "motivo_reapertura", length = 300)
    private String motivoReapertura;

    public LocalDate inicio() {
        return LocalDate.of(anio, mes, 1);
    }

    public LocalDate fin() {
        return inicio().plusMonths(1).minusDays(1);
    }

    public String etiqueta() {
        return String.format("%04d-%02d", anio, mes);
    }

    public boolean abierto() {
        return estado == EstadoPeriodo.ABIERTO;
    }
}
