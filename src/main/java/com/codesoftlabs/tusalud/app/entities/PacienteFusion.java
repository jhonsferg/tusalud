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
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "paciente_fusiones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PacienteFusion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destino_paciente_id", nullable = false)
    private Paciente destino;

    @Column(name = "origen_historial", nullable = false, length = 20)
    private String origenHistorial;

    @Column(name = "origen_dni", nullable = false, length = 8)
    private String origenDni;

    @Column(name = "origen_nombre", nullable = false, length = 200)
    private String origenNombre;

    @Column(nullable = false, length = 300)
    private String motivo;

    @Column(nullable = false, columnDefinition = "text")
    private String resumen;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "realizado_por_id", nullable = false)
    private Usuario realizadoPor;

    @Column(name = "realizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime realizadoEn = LocalDateTime.now();
}
