package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoTurno;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadTurno;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "turnos",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_turnos_numero_fecha",
                    columnNames = {"fecha", "numero"}),
            @UniqueConstraint(name = "uk_turnos_cita", columnNames = "cita_id")
        },
        indexes = {
            @Index(name = "idx_turnos_fecha_estado", columnList = "fecha, estado"),
            @Index(name = "idx_turnos_medico_fecha", columnList = "medico_id, fecha")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Turno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String numero;

    @Column(nullable = false)
    private Integer secuencia;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private PrioridadTurno prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private EstadoTurno estado = EstadoTurno.EN_ESPERA;

    @Column(length = 300)
    private String motivo;

    @Column(length = 50)
    private String consultorio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cita_id")
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id")
    private Medico medico;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "llamado_en")
    private LocalDateTime llamadoEn;

    @Column(name = "inicio_atencion_en")
    private LocalDateTime inicioAtencionEn;

    @Column(name = "fin_atencion_en")
    private LocalDateTime finAtencionEn;
}
