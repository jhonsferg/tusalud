package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.EstadoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.TipoCpe;
import com.codesoftlabs.tusalud.app.entities.enums.TipoDocumentoCliente;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
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
        name = "documentos_electronicos",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_documentos_electronicos_numero",
                        columnNames = {"serie", "numero"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoElectronico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private TipoCpe tipo;

    @Column(nullable = false, length = 4)
    private String serie;

    @Column(nullable = false)
    private Integer numero;

    @Column(nullable = false, length = 20)
    private String sede;

    @Column(name = "emitido_en", nullable = false)
    private LocalDateTime emitidoEn;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String moneda = "PEN";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comprobante_id")
    private Comprobante comprobante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "documento_referencia_id")
    private DocumentoElectronico documentoReferencia;

    @Column(name = "motivo_codigo", length = 2)
    private String motivoCodigo;

    @Column(name = "motivo_descripcion", length = 250)
    private String motivoDescripcion;

    @Enumerated(EnumType.STRING)
    @Column(name = "cliente_tipo_doc", nullable = false, length = 10)
    private TipoDocumentoCliente clienteTipoDoc;

    @Column(name = "cliente_doc", length = 11)
    private String clienteDoc;

    @Column(name = "cliente_nombre", nullable = false, length = 150)
    private String clienteNombre;

    @Column(name = "cliente_direccion", length = 200)
    private String clienteDireccion;

    @Column(name = "op_gravada", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal opGravada = BigDecimal.ZERO;

    @Column(name = "op_exonerada", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal opExonerada = BigDecimal.ZERO;

    @Column(name = "op_inafecta", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal opInafecta = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoCpe estado;

    @Column(name = "digest_value", length = 100)
    private String digestValue;

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

    @Column(name = "cdr_recibido_en")
    private LocalDateTime cdrRecibidoEn;

    @Column(nullable = false)
    private int intentos;

    @Column(name = "proximo_intento")
    private LocalDateTime proximoIntento;

    @Column(name = "ultimo_error", length = 400)
    private String ultimoError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resumen_id")
    private ResumenSunat resumen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "baja_resumen_id")
    private ResumenSunat bajaResumen;

    @Column(name = "motivo_baja", length = 250)
    private String motivoBaja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id")
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por_id")
    private Usuario creadoPor;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("posicion ASC")
    @Builder.Default
    private List<DocumentoElectronicoItem> items = new ArrayList<>();

    public String numeroCompleto() {
        return String.format("%s-%08d", serie, numero);
    }

    public void agregarItem(DocumentoElectronicoItem item) {
        item.setPosicion(items.size());
        item.setDocumento(this);
        items.add(item);
    }
}
