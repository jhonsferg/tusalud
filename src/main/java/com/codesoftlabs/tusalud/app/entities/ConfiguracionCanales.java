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
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "configuracion_canales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfiguracionCanales {

    public static final LocalTime SILENCIO_DESDE = LocalTime.of(22, 0);
    public static final LocalTime SILENCIO_HASTA = LocalTime.of(7, 0);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "silencio_activo", nullable = false)
    @Builder.Default
    private boolean silencioActivo = true;

    @Column(name = "silencio_desde", nullable = false)
    @Builder.Default
    private LocalTime silencioDesde = SILENCIO_DESDE;

    @Column(name = "silencio_hasta", nullable = false)
    @Builder.Default
    private LocalTime silencioHasta = SILENCIO_HASTA;
}
