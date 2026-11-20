package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.DestinoExtra;
import com.codesoftlabs.tusalud.app.entities.enums.PlanillaEnums.EstadoAsistencia;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "asistencias_dia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AsistenciaDia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empleado_id", nullable = false)
    private Empleado empleado;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "turno_personal_id")
    private TurnoPersonal turnoPersonal;

    @Column(name = "programada_inicio")
    private LocalTime programadaInicio;

    @Column(name = "programada_fin")
    private LocalTime programadaFin;

    @Column(name = "entrada")
    private LocalDateTime entrada;

    @Column(name = "salida")
    private LocalDateTime salida;

    @Column(name = "minutos_tardanza", nullable = false)
    private int minutosTardanza;

    @Column(name = "minutos_trabajados", nullable = false)
    private int minutosTrabajados;

    @Column(name = "minutos_extra", nullable = false)
    private int minutosExtra;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAsistencia estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "extra_destino", nullable = false, length = 15)
    private DestinoExtra extraDestino;

    @Column(name = "falta_justificada", nullable = false)
    @Builder.Default
    private boolean faltaJustificada = false;

    @Column(name = "justificacion", length = 300)
    private String justificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ajustado_por_id")
    private Usuario ajustadoPor;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
