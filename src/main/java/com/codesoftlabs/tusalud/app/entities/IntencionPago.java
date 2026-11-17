package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.Billetera;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoIntencionPago;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
        name = "intenciones_pago",
        uniqueConstraints = @UniqueConstraint(name = "uk_intenciones_pago_codigo", columnNames = "codigo"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntencionPago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Billetera billetera;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal descuento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "codigo_cupon", length = 30)
    private String codigoCupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoIntencionPago estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pago_id")
    private Pago pago;

    @Column(name = "expira_en", nullable = false)
    private LocalDateTime expiraEn;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public boolean vencida(LocalDateTime ahora) {
        return estado == EstadoIntencionPago.PENDIENTE && !expiraEn.isAfter(ahora);
    }
}
