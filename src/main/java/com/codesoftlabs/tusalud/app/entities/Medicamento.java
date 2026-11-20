package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.CategoriaControl;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoVenta;
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
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "medicamentos",
        indexes = {
            @Index(name = "idx_medicamentos_principio", columnList = "principio_activo_id"),
            @Index(name = "idx_medicamentos_producto", columnList = "producto_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "principio_activo_id", nullable = false)
    private PrincipioActivo principioActivo;

    @Column(name = "nombre_comercial", length = 120)
    private String nombreComercial;

    @Column(name = "forma_farmaceutica", nullable = false, length = 60)
    private String formaFarmaceutica;

    @Column(nullable = false, length = 60)
    private String concentracion;

    @Column(name = "via_administracion", nullable = false, length = 30)
    private String viaAdministracion;

    @Column(length = 120)
    private String presentacion;

    @Enumerated(EnumType.STRING)
    @Column(name = "categoria_control", nullable = false, length = 20)
    @Builder.Default
    private CategoriaControl categoriaControl = CategoriaControl.NINGUNA;

    @Column(name = "requiere_receta", nullable = false)
    @Builder.Default
    private boolean requiereReceta = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_venta", nullable = false, length = 15)
    @Builder.Default
    private EstadoVenta estadoVenta = EstadoVenta.VIGENTE;

    @Column(name = "producto_id")
    private UUID productoId;

    @Column(name = "en_petitorio", nullable = false)
    @Builder.Default
    private boolean enPetitorio = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    public String nombreCompleto() {
        return principioActivo.getDci() + " " + concentracion + " " + formaFarmaceutica.toLowerCase();
    }
}
