package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.MarcaTarjeta;
import com.codesoftlabs.tusalud.app.entities.enums.TipoMetodoPago;
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
import java.time.YearMonth;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "metodos_pago", indexes = @Index(name = "idx_metodos_pago_usuario", columnList = "usuario_id"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMetodoPago tipo;

    @Column(nullable = false, length = 100)
    private String titular;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private MarcaTarjeta marca;

    @Column(nullable = false, length = 4)
    private String ultimos4;

    @Column(name = "vencimiento_mes", nullable = false)
    private Integer vencimientoMes;

    @Column(name = "vencimiento_anio", nullable = false)
    private Integer vencimientoAnio;

    @Column(nullable = false)
    @Builder.Default
    private boolean predeterminado = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean estaVencida(YearMonth hoy) {
        return YearMonth.of(vencimientoAnio, vencimientoMes).isBefore(hoy);
    }

    public String descripcion() {
        return marca.name() + " terminada en " + ultimos4;
    }
}
