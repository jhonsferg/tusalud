package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.SeveridadSeguridad;
import com.codesoftlabs.tusalud.app.entities.enums.TipoEventoSeguridad;
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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "eventos_seguridad",
        indexes = {
            @Index(name = "idx_eventos_seguridad_tipo_creado", columnList = "tipo, creado_en"),
            @Index(name = "idx_eventos_seguridad_usuario_creado", columnList = "usuario_id, creado_en")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoEventoSeguridad tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeveridadSeguridad severidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Usuario actor;

    @Column(name = "ip_enmascarada", length = 45)
    private String ipEnmascarada;

    @Column(length = 120)
    private String dispositivo;

    @Column(length = 300)
    private String detalle;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
