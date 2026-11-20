package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.EstadoEvento;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.MetodoAnalisis;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.SeveridadEvento;
import com.codesoftlabs.tusalud.app.entities.enums.CalidadEnums.TipoEvento;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "eventos_adversos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoAdverso {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private int anio;

    @Column(nullable = false)
    private int numero;

    @Column(nullable = false, length = 20, unique = true)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private TipoEvento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private SeveridadEvento severidad;

    @Column(nullable = false, length = 60)
    private String area;

    @Column(nullable = false, length = 2000)
    private String descripcion;

    @Column(nullable = false)
    private boolean anonimo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notificante_id", nullable = false)
    private Usuario notificante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cirugia_id")
    private Cirugia cirugia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispensacion_id")
    private Dispensacion dispensacion;

    @Column(length = 40)
    private String clasificacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    @Builder.Default
    private EstadoEvento estado = EstadoEvento.NOTIFICADO;

    @Column(name = "ocurrido_en", nullable = false)
    private LocalDateTime ocurridoEn;

    @Column(name = "notificado_en", nullable = false)
    @Builder.Default
    private LocalDateTime notificadoEn = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "analisis_metodo", length = 15)
    private MetodoAnalisis analisisMetodo;

    @Column(name = "analisis_conclusion", length = 1000)
    private String analisisConclusion;

    @Column(name = "cerrado_en")
    private LocalDateTime cerradoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cerrado_por_id")
    private Usuario cerradoPor;

    @Column(length = 1000)
    private String verificacion;

    @Column(name = "accion_eficaz")
    private Boolean accionEficaz;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Builder.Default
    private List<EventoCausa> causas = new ArrayList<>();

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("creadoEn ASC, id ASC")
    @Builder.Default
    private List<EventoAccion> acciones = new ArrayList<>();
}
