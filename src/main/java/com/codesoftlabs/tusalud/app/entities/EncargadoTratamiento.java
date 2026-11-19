package com.codesoftlabs.tusalud.app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "encargados_tratamiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EncargadoTratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "razon_social", nullable = false, length = 150)
    private String razonSocial;

    @Column(length = 11)
    private String ruc;

    @Column(nullable = false, length = 200)
    private String servicio;

    @Column(name = "categorias_datos", nullable = false, length = 300)
    private String categoriasDatos;

    @Column(nullable = false, length = 60)
    private String pais;

    @Column(nullable = false)
    private boolean transfronterizo;

    @Column(name = "contrato_vigente_hasta")
    private LocalDate contratoVigenteHasta;

    @Column(length = 150)
    private String contacto;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
