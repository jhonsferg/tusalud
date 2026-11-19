package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoTeleconsulta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "teleconsultas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Teleconsulta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false, unique = true)
    private Cita cita;

    @Column(nullable = false, unique = true)
    private UUID sala;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoTeleconsulta estado = EstadoTeleconsulta.PROGRAMADA;

    @Column(name = "abierta_en")
    private LocalDateTime abiertaEn;

    @Column(name = "inicio_real")
    private LocalDateTime inicioReal;

    @Column(name = "fin_real")
    private LocalDateTime finReal;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Column(nullable = false)
    @Builder.Default
    private int reconexiones = 0;

    @Column(name = "tipo_candidato", length = 10)
    private String tipoCandidato;

    @Column(name = "recordatorio_enviado", nullable = false)
    @Builder.Default
    private boolean recordatorioEnviado = false;

    @Column(name = "sala_abierta_notificada", nullable = false)
    @Builder.Default
    private boolean salaAbiertaNotificada = false;

    @Column(name = "paciente_en_sala_notificado", nullable = false)
    @Builder.Default
    private boolean pacienteEnSalaNotificado = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
