package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "series_comprobante",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_series_comprobante",
                        columnNames = {"sede", "serie"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SerieComprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String sede;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoCpe tipo;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(name = "correlativo_actual", nullable = false)
    private int correlativoActual;

    @Column(nullable = false)
    @Builder.Default
    private boolean activa = true;
}
