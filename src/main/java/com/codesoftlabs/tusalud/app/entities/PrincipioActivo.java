package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaEmbarazo;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CompatibilidadLactancia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "principios_activos",
        uniqueConstraints = @UniqueConstraint(name = "uk_principios_activos_dci", columnNames = "dci"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrincipioActivo {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 120)
    private String dci;

    @Column(name = "clase_terapeutica", length = 80)
    private String claseTerapeutica;

    @Column(name = "grupo_alergia", length = 40)
    private String grupoAlergia;

    @Column(name = "alias_alergia", length = 250)
    private String aliasAlergia;

    @Column(name = "dosis_max_adulto_mg_dia", precision = 10, scale = 2)
    private BigDecimal dosisMaxAdultoMgDia;

    @Column(name = "dosis_max_pediatrica_mg_kg_dia", precision = 8, scale = 3)
    private BigDecimal dosisMaxPediatricaMgKgDia;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_embarazo", nullable = false, length = 15)
    @Builder.Default
    private CategoriaEmbarazo categoriaEmbarazo = CategoriaEmbarazo.SIN_CLASIFICAR;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private CompatibilidadLactancia lactancia = CompatibilidadLactancia.SIN_CLASIFICAR;

    @Column(name = "aviso_renal", length = 300)
    private String avisoRenal;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();
}
