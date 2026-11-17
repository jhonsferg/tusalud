package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoOrdenImagen;
import com.codesoftlabs.tusalud.app.entities.enums.PrioridadOrden;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
        name = "ordenes_imagen",
        uniqueConstraints = @UniqueConstraint(name = "uk_ordenes_imagen_codigo", columnNames = "codigo"),
        indexes = {
            @Index(name = "idx_ordenes_imagen_estado_fecha", columnList = "estado, fecha"),
            @Index(name = "idx_ordenes_imagen_paciente", columnList = "paciente_id"),
            @Index(name = "idx_ordenes_imagen_medico", columnList = "medico_id"),
            @Index(name = "idx_ordenes_imagen_programada", columnList = "programada_para")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenImagen {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoOrdenImagen estado = EstadoOrdenImagen.SOLICITADA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private PrioridadOrden prioridad = PrioridadOrden.NORMAL;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_id")
    private Atencion atencion;

    @Column(name = "indicacion_clinica", nullable = false, length = 500)
    private String indicacionClinica;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "programada_para")
    private LocalDateTime programadaPara;

    @Column(length = 40)
    private String sala;

    @Column(name = "realizada_en")
    private LocalDateTime realizadaEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "realizada_por")
    private Usuario realizadaPor;

    @Column(name = "observaciones_toma", length = 300)
    private String observacionesToma;

    @Column(name = "informada_en")
    private LocalDateTime informadaEn;

    @Column(name = "hallazgo_critico", nullable = false)
    @Builder.Default
    private boolean hallazgoCritico = false;

    @Column(name = "motivo_cancelacion", length = 300)
    private String motivoCancelacion;

    @Column(nullable = false)
    @Builder.Default
    private boolean cobrado = false;

    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Builder.Default
    private List<ItemOrdenImagen> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void agregarItem(ItemOrdenImagen item) {
        item.setPosicion(items.size());
        items.add(item);
        item.setOrden(this);
    }
}
