package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.SeveridadInteraccion;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "interacciones_medicamentosas",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_interacciones_par",
                        columnNames = {"principio_a_id", "principio_b_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteraccionMedicamentosa {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "principio_a_id", nullable = false)
    private PrincipioActivo principioA;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "principio_b_id", nullable = false)
    private PrincipioActivo principioB;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private SeveridadInteraccion severidad;

    @Column(nullable = false, length = 400)
    private String descripcion;

    @Column(length = 400)
    private String recomendacion;
}
