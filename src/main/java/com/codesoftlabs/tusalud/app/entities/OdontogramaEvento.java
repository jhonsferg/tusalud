package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.EstadoPieza;
import com.codesoftlabs.tusalud.app.entities.enums.OdontologiaEnums.Superficie;
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
import org.hibernate.annotations.Immutable;

@Entity
@Immutable
@Table(name = "odontograma_eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OdontogramaEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @Column(nullable = false)
    private short pieza;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Superficie superficie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoPieza estado;

    @Column(length = 300)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por_id", nullable = false)
    private Usuario registradoPor;

    @Column(name = "item_id")
    private UUID itemId;

    @Column(name = "registrado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime registradoEn = LocalDateTime.now();
}
