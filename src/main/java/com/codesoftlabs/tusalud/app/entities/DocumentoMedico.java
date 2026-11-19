package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoDocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documentos_medicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoMedico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDocumentoMedico tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private EstadoDocumentoMedico estado = EstadoDocumentoMedico.BORRADOR;

    @Column(length = 10)
    private String serie;

    private Integer anio;

    private Integer numero;

    @Column(name = "codigo_verificacion", length = 19)
    private String codigoVerificacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "atencion_id")
    private Atencion atencion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingreso_id")
    private IngresoHospitalario ingreso;

    @Column(name = "documento_origen_id")
    private UUID documentoOrigenId;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "autoriza_diagnostico", nullable = false)
    private boolean autorizaDiagnostico;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "contenido_hash", length = 64)
    private String contenidoHash;

    @Column(name = "pdf_hash", length = 64)
    private String pdfHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_archivo_id")
    private Archivo pdfArchivo;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "emitido_en")
    private LocalDateTime emitidoEn;

    @Column(name = "anulado_en")
    private LocalDateTime anuladoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anulado_por_id")
    private Usuario anuladoPor;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    public String numeroCompleto() {
        if (serie == null || anio == null || numero == null) {
            return null;
        }
        return String.format("%s-%d-%06d", serie, anio, numero);
    }
}
