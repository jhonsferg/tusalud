package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "hce_impresiones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HceImpresion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(name = "codigo_verificacion", nullable = false, length = 19, unique = true)
    private String codigoVerificacion;

    @Column(name = "contenido_hash", nullable = false, length = 64)
    private String contenidoHash;

    @Column(nullable = false)
    private int atenciones;

    @Column(name = "emitido_en", nullable = false)
    @Builder.Default
    private LocalDateTime emitidoEn = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "emitido_por_id", nullable = false)
    private Usuario emitidoPor;
}
