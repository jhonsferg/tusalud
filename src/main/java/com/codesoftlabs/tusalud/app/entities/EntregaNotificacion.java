package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.entities.enums.TipoNotificacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "entregas_notificacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_entregas_idempotencia", columnNames = "clave_idempotencia"),
        indexes = {
            @Index(name = "idx_entregas_cola", columnList = "estado, proximo_intento"),
            @Index(name = "idx_entregas_usuario_canal", columnList = "usuario_id, canal, creado_en")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntregaNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "notificacion_id")
    private UUID notificacionId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Canal canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoNotificacion tipo;

    @Column(name = "destino_mascara", nullable = false, length = 60)
    private String destinoMascara;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoEntrega estado = EstadoEntrega.PENDIENTE;

    @Column(nullable = false)
    @Builder.Default
    private int intentos = 0;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(length = 200)
    private String motivo;

    @Column(name = "id_externo", length = 100)
    private String idExterno;

    @Column(name = "clave_idempotencia", nullable = false, length = 100)
    private String claveIdempotencia;

    @Column(name = "plantilla_version")
    private Integer plantillaVersion;

    @Column(nullable = false)
    private boolean urgente;

    @Version
    private long version;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "enviado_en")
    private LocalDateTime enviadoEn;

    @Column(name = "entregado_en")
    private LocalDateTime entregadoEn;
}
