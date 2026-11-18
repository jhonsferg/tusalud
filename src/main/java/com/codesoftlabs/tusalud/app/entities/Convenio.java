package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "convenios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Convenio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aseguradora_id", nullable = false)
    private Aseguradora aseguradora;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(name = "vigencia_desde", nullable = false)
    private LocalDate vigenciaDesde;

    @Column(name = "vigencia_hasta", nullable = false)
    private LocalDate vigenciaHasta;

    @Column(name = "tope_anual", precision = 12, scale = 2)
    private BigDecimal topeAnual;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @OneToMany(mappedBy = "convenio", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReglaCobertura> reglas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "convenio_exclusiones", joinColumns = @JoinColumn(name = "convenio_id"))
    @Column(name = "codigo", length = 40)
    @Builder.Default
    private Set<String> exclusiones = new LinkedHashSet<>();

    public boolean vigenteEn(LocalDate fecha) {
        return activo && aseguradora.isActiva() && !fecha.isBefore(vigenciaDesde) && !fecha.isAfter(vigenciaHasta);
    }
}
