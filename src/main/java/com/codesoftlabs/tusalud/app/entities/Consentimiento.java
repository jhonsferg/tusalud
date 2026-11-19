package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.FirmanteTipo;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.ViaFirma;
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
@Table(name = "consentimientos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consentimiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plantilla_id", nullable = false)
    private ConsentimientoPlantilla plantilla;

    @Column(name = "plantilla_version", nullable = false)
    private int plantillaVersion;

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

    @Column(name = "orden_imagen_id")
    private UUID ordenImagenId;

    @Column(nullable = false, length = 200)
    private String procedimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private EstadoConsentimiento estado = EstadoConsentimiento.PENDIENTE_FIRMA;

    @Column(name = "texto_aceptado", nullable = false, columnDefinition = "text")
    private String textoAceptado;

    @Column(name = "requiere_testigo", nullable = false)
    private boolean requiereTestigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "firmante_tipo", length = 15)
    private FirmanteTipo firmanteTipo;

    @Column(name = "firmante_nombre", length = 150)
    private String firmanteNombre;

    @Column(name = "firmante_dni", length = 12)
    private String firmanteDni;

    @Column(name = "firmante_parentesco", length = 50)
    private String firmanteParentesco;

    @Column(name = "testigo_nombre", length = 150)
    private String testigoNombre;

    @Column(name = "testigo_dni", length = 12)
    private String testigoDni;

    @Enumerated(EnumType.STRING)
    @Column(name = "firmado_via", length = 15)
    private ViaFirma firmadoVia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "firma_archivo_id")
    private Archivo firmaArchivo;

    @Column(name = "firma_hash", length = 64)
    private String firmaHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_archivo_id")
    private Archivo pdfArchivo;

    @Column(name = "pdf_hash", length = 64)
    private String pdfHash;

    @Column(name = "codigo_verificacion", length = 19)
    private String codigoVerificacion;

    @Column(name = "creado_en", nullable = false)
    @Builder.Default
    private LocalDateTime creadoEn = LocalDateTime.now();

    @Column(name = "firmado_en")
    private LocalDateTime firmadoEn;

    @Column(name = "vence_en")
    private LocalDateTime venceEn;

    @Column(name = "revocado_en")
    private LocalDateTime revocadoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revocado_por_id")
    private Usuario revocadoPor;

    @Column(name = "motivo_revocacion", length = 300)
    private String motivoRevocacion;

    public EstadoConsentimiento estadoEfectivo() {
        if (estado == EstadoConsentimiento.FIRMADO && venceEn != null && venceEn.isBefore(LocalDateTime.now())) {
            return EstadoConsentimiento.VENCIDO;
        }
        return estado;
    }
}
