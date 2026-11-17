package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.EstadoDispensacion;
import com.codesoftlabs.tusalud.app.entities.enums.MetodoPagoFarmacia;
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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "dispensaciones",
        uniqueConstraints = @UniqueConstraint(name = "uk_dispensaciones_comprobante", columnNames = "comprobante"),
        indexes = {
            @Index(name = "idx_dispensaciones_fecha", columnList = "fecha"),
            @Index(name = "idx_dispensaciones_paciente", columnList = "paciente_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispensacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, length = 20)
    private String comprobante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoDispensacion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_id")
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 10)
    private MetodoPagoFarmacia metodoPago;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Column(length = 300)
    private String observaciones;

    @OneToMany(mappedBy = "dispensacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DispensacionItem> items = new ArrayList<>();

    public void agregarItem(DispensacionItem item) {
        items.add(item);
        item.setDispensacion(this);
    }
}
