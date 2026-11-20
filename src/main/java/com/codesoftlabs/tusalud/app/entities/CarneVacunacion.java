package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "carnes_vacunacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarneVacunacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "codigo_verificacion", nullable = false, length = 19)
    private String codigoVerificacion;

    @Column(name = "contenido_hash", nullable = false, length = 64)
    private String contenidoHash;

    @Column(name = "emitido_en", nullable = false)
    @Builder.Default
    private LocalDateTime emitidoEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emitido_por_id", nullable = false)
    private Usuario emitidoPor;
}
