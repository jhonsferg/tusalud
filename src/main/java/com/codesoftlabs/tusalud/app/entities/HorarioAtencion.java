package com.codesoftlabs.tusalud.app.entities;

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
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "horarios_atencion",
        indexes = @Index(name = "idx_horarios_medico_dia", columnList = "medico_id, dia_semana"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", nullable = false, length = 10)
    private DayOfWeek diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "duracion_cita_minutos", nullable = false)
    @Builder.Default
    private Integer duracionCitaMinutos = 30;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    public boolean admiteCitaEn(LocalTime hora) {
        LocalTime fin = hora.plusMinutes(duracionCitaMinutos);
        boolean dentroDelBloque = !hora.isBefore(horaInicio) && !fin.isAfter(horaFin) && fin.isAfter(hora);
        long minutosDesdeInicio = Duration.between(horaInicio, hora).toMinutes();
        return dentroDelBloque && minutosDesdeInicio % duracionCitaMinutos == 0;
    }
}
