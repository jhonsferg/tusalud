package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.documentos.FormatoDocumento;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoExportacion;
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
        name = "exportaciones",
        indexes = {
            @Index(name = "idx_exportaciones_usuario_fecha", columnList = "solicitado_por, created_at"),
            @Index(name = "idx_exportaciones_estado_expira", columnList = "estado, expira_en")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Exportacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDocumento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FormatoDocumento formato;

    @Column(columnDefinition = "text")
    private String parametros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoExportacion estado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archivo_id")
    private Archivo archivo;

    @Column(length = 150)
    private String nombre;

    private Long tamano;

    @Column(length = 300)
    private String error;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitado_por", nullable = false)
    private Usuario solicitadoPor;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "iniciada_en")
    private LocalDateTime iniciadaEn;

    @Column(name = "terminada_en")
    private LocalDateTime terminadaEn;

    @Column(name = "expira_en")
    private LocalDateTime expiraEn;
}
