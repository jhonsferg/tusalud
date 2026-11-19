package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.AlcanceRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.CategoriaRetencion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEjecucionRespaldo;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.TipoEjecucionRespaldo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
        name = "respaldo_ejecuciones",
        indexes = @Index(name = "idx_respaldo_ejecuciones_inicio", columnList = "iniciado_en"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RespaldoEjecucion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoEjecucionRespaldo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoEjecucionRespaldo estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AlcanceRespaldo alcance;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private CategoriaRetencion categoria;

    @Column(name = "iniciado_en", nullable = false)
    private LocalDateTime iniciadoEn;

    @Column(name = "finalizado_en", nullable = false)
    private LocalDateTime finalizadoEn;

    @Column(name = "bytes_total")
    private Long bytesTotal;

    @Column(length = 200)
    private String archivo;

    @Column(length = 64)
    private String checksum;

    @Column(name = "migraciones_validadas")
    private Integer migracionesValidadas;

    @Column(name = "tablas_validadas")
    private Integer tablasValidadas;

    @Column(name = "objetos_almacen")
    private Integer objetosAlmacen;

    @Column(name = "copia_remota", nullable = false)
    private boolean copiaRemota;

    @Column(length = 500)
    private String detalle;

    @Column(name = "registrado_en", nullable = false)
    @Builder.Default
    private LocalDateTime registradoEn = LocalDateTime.now();
}
