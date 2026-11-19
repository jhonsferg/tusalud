package com.codesoftlabs.tusalud.app.entities;

import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.OperacionSunat;
import com.codesoftlabs.tusalud.app.entities.enums.FacturacionEnums.ProveedorFiscal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "intercambios_sunat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntercambioSunat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "documento_id")
    private UUID documentoId;

    @Column(name = "resumen_id")
    private UUID resumenId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OperacionSunat operacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private ProveedorFiscal proveedor;

    @Column(nullable = false)
    private boolean exitoso;

    @Column(name = "codigo_respuesta", length = 10)
    private String codigoRespuesta;

    @Column(length = 500)
    private String mensaje;

    @Column(name = "duracion_ms", nullable = false)
    private int duracionMs;

    @Column(name = "hash_envio", length = 64)
    private String hashEnvio;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;
}
