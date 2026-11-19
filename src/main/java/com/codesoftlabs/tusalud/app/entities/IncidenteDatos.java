package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.SeveridadIncidente;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoIncidente;
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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "incidentes_datos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidenteDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoIncidente tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SeveridadIncidente severidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoIncidente estado;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(name = "datos_afectados", nullable = false, length = 400)
    private String datosAfectados;

    @Column(name = "titulares_afectados", nullable = false)
    private int titularesAfectados;

    @Column(name = "detectado_en", nullable = false)
    private LocalDateTime detectadoEn;

    @Column(name = "notificar_autoridad_antes_de", nullable = false)
    private LocalDateTime notificarAutoridadAntesDe;

    @Column(name = "autoridad_notificada_en")
    private LocalDateTime autoridadNotificadaEn;

    @Column(name = "titulares_notificados_en")
    private LocalDateTime titularesNotificadosEn;

    @Column(length = 1000)
    private String contencion;

    @Column(name = "causa_raiz", length = 1000)
    private String causaRaiz;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;
}
