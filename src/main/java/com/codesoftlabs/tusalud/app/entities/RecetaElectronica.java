package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "recetas_electronicas",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_recetas_numero", columnNames = "numero"),
            @UniqueConstraint(name = "uk_recetas_codigo", columnNames = "codigo_verificacion")
        },
        indexes = {
            @Index(name = "idx_recetas_paciente", columnList = "paciente_id"),
            @Index(name = "idx_recetas_atencion", columnList = "atencion_id"),
            @Index(name = "idx_recetas_medico", columnList = "medico_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecetaElectronica {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String numero;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "atencion_id", nullable = false)
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoRecetaElectronica estado = EstadoRecetaElectronica.EMITIDA;

    @Column(name = "emitida_en", nullable = false)
    private LocalDateTime emitidaEn;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDateTime vigenteHasta;

    @Column(length = 1000)
    private String indicaciones;

    @Column(name = "diagnostico_codigo", length = 12)
    private String diagnosticoCodigo;

    @Column(name = "diagnostico_descripcion", length = 300)
    private String diagnosticoDescripcion;

    @Column(name = "contenido_canonico", nullable = false, columnDefinition = "text")
    private String contenidoCanonico;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(nullable = false, columnDefinition = "text")
    private String firma;

    @Column(name = "firma_algoritmo", nullable = false, length = 30)
    private String firmaAlgoritmo;

    @Column(name = "certificado_huella", nullable = false, length = 64)
    private String certificadoHuella;

    @Column(name = "certificado_sujeto", nullable = false, length = 200)
    private String certificadoSujeto;

    @Column(name = "certificado_der", nullable = false, columnDefinition = "text")
    private String certificadoDer;

    @Column(name = "certificado_de_prueba", nullable = false)
    private boolean certificadoDePrueba;

    @Column(name = "codigo_verificacion", nullable = false, length = 19)
    private String codigoVerificacion;

    @Column(name = "control_especial", nullable = false)
    private boolean controlEspecial;

    @Column(name = "anulada_en")
    private LocalDateTime anuladaEn;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reemplaza_id")
    private RecetaElectronica reemplaza;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Builder.Default
    private List<RecetaElectronicaItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RecetaAlerta> alertas = new ArrayList<>();

    public void agregarItem(RecetaElectronicaItem item) {
        item.setReceta(this);
        items.add(item);
    }

    public void agregarAlerta(RecetaAlerta alerta) {
        alerta.setReceta(this);
        alertas.add(alerta);
    }

    public boolean vigente(LocalDateTime ahora) {
        return estado != EstadoRecetaElectronica.ANULADA && !ahora.isAfter(vigenteHasta);
    }
}
