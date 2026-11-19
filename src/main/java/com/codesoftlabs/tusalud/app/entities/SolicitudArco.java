package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.EstadoArco;
import com.codesoftlabs.tusalud.app.entities.enums.PrivacidadEnums.TipoArco;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "solicitudes_arco")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SolicitudArco {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoArco tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoArco estado;

    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Column(length = 30)
    private String campo;

    @Column(name = "valor_propuesto", length = 200)
    private String valorPropuesto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalidad_id")
    private FinalidadTratamiento finalidad;

    @Column(name = "recibida_en", nullable = false)
    private LocalDateTime recibidaEn;

    @Column(name = "vence_en", nullable = false)
    private LocalDate venceEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsable_id")
    private Usuario responsable;

    @Column(name = "resuelta_en")
    private LocalDateTime resueltaEn;

    @Column(length = 1000)
    private String resolucion;

    @Column(name = "exportacion_clave", length = 200)
    private String exportacionClave;

    @Column(name = "exportacion_sha256", length = 64)
    private String exportacionSha256;

    @Column(name = "resultado_json", columnDefinition = "text")
    private String resultadoJson;
}
