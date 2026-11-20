package com.codesoftlabs.tusalud.app.entities;

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
@Table(name = "lotes_esteriles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoteEsteril {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "set_id", nullable = false)
    private SetInstrumental setInstrumental;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ciclo_id", nullable = false)
    private CicloEsterilizacion ciclo;

    @Column(name = "esterilizado_en", nullable = false)
    private LocalDateTime esterilizadoEn;

    @Column(name = "vence_en", nullable = false)
    private LocalDateTime venceEn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoLoteEsteril estado =
            com.codesoftlabs.tusalud.app.entities.enums.QuirofanoEnums.EstadoLoteEsteril.ESTERIL;
}
