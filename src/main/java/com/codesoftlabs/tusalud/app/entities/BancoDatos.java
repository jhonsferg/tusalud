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
@Table(name = "bancos_datos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BancoDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, length = 500)
    private String descripcion;

    @Column(name = "finalidad_principal", nullable = false, length = 300)
    private String finalidadPrincipal;

    @Column(name = "categorias_datos", nullable = false, length = 400)
    private String categoriasDatos;

    @Column(name = "contiene_sensibles", nullable = false)
    private boolean contieneSensibles;

    @Column(nullable = false, length = 150)
    private String responsable;

    @Column(nullable = false, length = 150)
    private String ubicacion;

    @Column(name = "medidas_seguridad", nullable = false, length = 600)
    private String medidasSeguridad;

    @Column(name = "transferencia_transfronteriza", nullable = false)
    private boolean transferenciaTransfronteriza;

    @Column(name = "numero_inscripcion", length = 40)
    private String numeroInscripcion;

    @Column(name = "fecha_inscripcion")
    private LocalDate fechaInscripcion;

    @Column(nullable = false)
    @Builder.Default
    private boolean activo = true;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
