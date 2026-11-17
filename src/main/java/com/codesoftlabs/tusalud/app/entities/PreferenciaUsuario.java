package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "preferencias_usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_preferencias_usuario", columnNames = "usuario_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenciaUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(name = "recordatorios_activos", nullable = false)
    @Builder.Default
    private boolean recordatoriosActivos = true;

    @Column(name = "horas_antelacion_recordatorio", nullable = false)
    @Builder.Default
    private int horasAntelacionRecordatorio = 24;

    @Column(name = "alertas_inicio_sesion", nullable = false)
    @Builder.Default
    private boolean alertasInicioSesion = true;

    @Column(name = "tour_completado", nullable = false)
    @Builder.Default
    private boolean tourCompletado = false;
}
