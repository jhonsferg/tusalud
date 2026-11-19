package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoResumen;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoResumen;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "resumenes_sunat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumenSunat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoResumen tipo;

    @Column(nullable = false, length = 20)
    private String identificador;

    @Column(name = "fecha_referencia", nullable = false)
    private LocalDate fechaReferencia;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoResumen estado;

    @Column(length = 40)
    private String ticket;

    @Column(name = "xml_clave", length = 200)
    private String xmlClave;

    @Column(name = "xml_sha256", length = 64)
    private String xmlSha256;

    @Column(name = "cdr_clave", length = 200)
    private String cdrClave;

    @Column(name = "cdr_codigo", length = 10)
    private String cdrCodigo;

    @Column(name = "cdr_descripcion", length = 400)
    private String cdrDescripcion;

    @Column(length = 250)
    private String motivo;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(name = "ultimo_error", length = 400)
    private String ultimoError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
