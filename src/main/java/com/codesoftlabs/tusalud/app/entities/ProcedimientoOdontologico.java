package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPieza;
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
@Table(name = "procedimientos_odontologicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcedimientoOdontologico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 40)
    private String categoria;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal precio;

    @Column(name = "requiere_pieza", nullable = false)
    @Builder.Default
    private boolean requierePieza = true;

    @Column(name = "requiere_superficie", nullable = false)
    @Builder.Default
    private boolean requiereSuperficie = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resultante", length = 25)
    private EstadoPieza estadoResultante;

    @Column(name = "requiere_consentimiento", nullable = false)
    @Builder.Default
    private boolean requiereConsentimiento = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;
}
