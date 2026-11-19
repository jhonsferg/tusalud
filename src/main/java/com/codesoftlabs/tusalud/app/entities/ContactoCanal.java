package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoContacto;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "contactos_canal",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_contactos_canal_usuario",
                        columnNames = {"usuario_id", "canal"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactoCanal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Canal canal;

    @Column(nullable = false, length = 150)
    private String destino;

    @Column(name = "consentimiento_en")
    private LocalDateTime consentimientoEn;

    @Column(name = "verificado_en")
    private LocalDateTime verificadoEn;

    @Column(name = "baja_en")
    private LocalDateTime bajaEn;

    @Column(name = "rebotado_en")
    private LocalDateTime rebotadoEn;

    @Column(name = "codigo_hash", length = 64)
    private String codigoHash;

    @Column(name = "codigo_expira")
    private LocalDateTime codigoExpira;

    @Column(name = "intentos_codigo", nullable = false)
    @Builder.Default
    private int intentosCodigo = 0;

    @Column(name = "ultimo_envio_codigo")
    private LocalDateTime ultimoEnvioCodigo;

    @Column(name = "envios_codigo_hora", nullable = false)
    @Builder.Default
    private int enviosCodigoHora = 0;

    @Column(name = "ventana_envios_desde")
    private LocalDateTime ventanaEnviosDesde;

    @Column(name = "actualizado_en", nullable = false)
    @Builder.Default
    private LocalDateTime actualizadoEn = LocalDateTime.now();

    public EstadoContacto estado() {
        if (bajaEn != null || rebotadoEn != null) {
            return EstadoContacto.BAJA;
        }
        if (verificadoEn != null) {
            return EstadoContacto.ACTIVO;
        }
        return consentimientoEn != null ? EstadoContacto.PENDIENTE_VERIFICACION : EstadoContacto.SIN_CONFIGURAR;
    }

    public boolean puedeRecibir(boolean obligatoria) {
        return bajaEn == null
                && rebotadoEn == null
                && verificadoEn != null
                && (obligatoria || consentimientoEn != null);
    }
}
