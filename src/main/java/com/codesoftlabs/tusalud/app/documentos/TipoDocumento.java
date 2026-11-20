package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.entities.enums.RolNombre;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

public enum TipoDocumento {
    COMPROBANTE(
            "comprobante",
            "comprobante",
            true,
            Set.of(FormatoDocumento.PDF),
            false,
            EnumSet.of(RolNombre.ROLE_CAJA, RolNombre.ROLE_ADMIN, RolNombre.ROLE_PACIENTE)),
    COMPROBANTE_ELECTRONICO(
            "comprobante-electronico",
            "comprobante-electronico",
            false,
            Set.of(FormatoDocumento.PDF),
            false,
            EnumSet.of(
                    RolNombre.ROLE_CAJA,
                    RolNombre.ROLE_FINANZAS,
                    RolNombre.ROLE_GERENCIA,
                    RolNombre.ROLE_ADMIN,
                    RolNombre.ROLE_PACIENTE)),
    RECETA(
            "receta",
            "receta",
            true,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE, RolNombre.ROLE_FARMACIA)),
    RESULTADOS_LABORATORIO(
            "resultados-laboratorio",
            "resultados-laboratorio",
            true,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_PACIENTE, RolNombre.ROLE_MEDICO, RolNombre.ROLE_LABORATORIO)),
    INFORME_IMAGEN(
            "informe-imagen",
            "informe-imagen",
            true,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_PACIENTE, RolNombre.ROLE_MEDICO, RolNombre.ROLE_IMAGENES)),
    CONSTANCIA(
            "constancia",
            "constancia",
            true,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_PACIENTE, RolNombre.ROLE_MEDICO)),
    RESUMEN_HISTORIA(
            "resumen-historia",
            "resumen-historia",
            true,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_PACIENTE, RolNombre.ROLE_MEDICO)),
    LIQUIDACION(
            "liquidacion",
            "liquidacion",
            true,
            Set.of(FormatoDocumento.PDF),
            false,
            EnumSet.of(RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    REPORTE_EJECUTIVO(
            "reporte-ejecutivo",
            "reporte-ejecutivo",
            false,
            Set.of(FormatoDocumento.PDF, FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    REPORTE_FINANCIERO(
            "reporte-financiero",
            "reporte-financiero",
            false,
            Set.of(FormatoDocumento.PDF, FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    REPORTE_OPERATIVO(
            "reporte-operativo",
            "reporte-operativo",
            false,
            Set.of(FormatoDocumento.PDF, FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_OPERACIONES, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    REPORTE_CALIDAD(
            "reporte-calidad",
            "reporte-calidad",
            false,
            Set.of(FormatoDocumento.PDF, FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_CALIDAD, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    REPORTE_MORBILIDAD(
            "reporte-morbilidad",
            "reporte-morbilidad",
            false,
            Set.of(FormatoDocumento.PDF, FormatoDocumento.XLSX),
            false,
            EnumSet.of(
                    RolNombre.ROLE_CALIDAD, RolNombre.ROLE_OPERACIONES, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    LISTADO_COMPROBANTES(
            "listado-comprobantes",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_CAJA, RolNombre.ROLE_ADMIN)),
    ORDEN_COMPRA(
            "orden-compra",
            "orden-compra",
            true,
            Set.of(FormatoDocumento.PDF),
            false,
            EnumSet.of(RolNombre.ROLE_ALMACEN, RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    REPORTE_COMPRAS(
            "reporte-compras",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA, RolNombre.ROLE_ADMIN)),
    REPORTE_CONTABLE(
            "reporte-contable",
            "reporte-contable",
            false,
            Set.of(FormatoDocumento.PDF, FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA)),
    LIBRO_DIARIO(
            "libro-diario",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA)),
    LIBRO_MAYOR(
            "libro-mayor",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA)),
    BALANCE_COMPROBACION(
            "balance-comprobacion",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_FINANZAS, RolNombre.ROLE_GERENCIA)),
    KARDEX(
            "kardex",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_ALMACEN, RolNombre.ROLE_ADMIN)),
    STOCK(
            "stock",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_ALMACEN, RolNombre.ROLE_ADMIN, RolNombre.ROLE_FARMACIA)),
    AUSENCIAS(
            "ausencias",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_RRHH, RolNombre.ROLE_ADMIN)),
    TURNOS(
            "turnos",
            null,
            false,
            Set.of(FormatoDocumento.XLSX),
            false,
            EnumSet.of(RolNombre.ROLE_RRHH, RolNombre.ROLE_ADMIN)),
    CERTIFICADO_MEDICO(
            "certificado-medico",
            "certificado-medico",
            false,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE)),
    DESCANSO_MEDICO(
            "descanso-medico",
            "descanso-medico",
            false,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE)),
    INFORME_MEDICO(
            "informe-medico",
            "informe-medico",
            false,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE)),
    REFERENCIA_MEDICA(
            "referencia-medica",
            "referencia-medica",
            false,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE)),
    CONTRARREFERENCIA_MEDICA(
            "contrarreferencia-medica",
            "contrarreferencia-medica",
            false,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE)),
    EPICRISIS(
            "epicrisis",
            "epicrisis",
            false,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE)),
    CONSENTIMIENTO(
            "consentimiento",
            "consentimiento",
            false,
            Set.of(FormatoDocumento.PDF),
            true,
            EnumSet.of(RolNombre.ROLE_MEDICO, RolNombre.ROLE_PACIENTE)),
    MIS_CITAS("mis-citas", null, false, Set.of(FormatoDocumento.XLSX), false, EnumSet.of(RolNombre.ROLE_MEDICO));

    private final String codigo;
    private final String plantilla;
    private final boolean directo;
    private final Set<FormatoDocumento> formatos;
    private final boolean clinico;
    private final Set<RolNombre> roles;

    TipoDocumento(
            String codigo,
            String plantilla,
            boolean directo,
            Set<FormatoDocumento> formatos,
            boolean clinico,
            Set<RolNombre> roles) {
        this.codigo = codigo;
        this.plantilla = plantilla;
        this.directo = directo;
        this.formatos = formatos;
        this.clinico = clinico;
        this.roles = roles;
    }

    public String codigo() {
        return codigo;
    }

    public String plantilla() {
        return plantilla;
    }

    public boolean directo() {
        return directo;
    }

    public Set<FormatoDocumento> formatos() {
        return formatos;
    }

    public boolean clinico() {
        return clinico;
    }

    public Set<RolNombre> roles() {
        return roles;
    }

    public boolean autonomo() {
        return switch (this) {
            case CERTIFICADO_MEDICO,
                    DESCANSO_MEDICO,
                    INFORME_MEDICO,
                    REFERENCIA_MEDICA,
                    CONTRARREFERENCIA_MEDICA,
                    EPICRISIS,
                    CONSENTIMIENTO -> true;
            default -> false;
        };
    }

    public boolean esReporte() {
        return name().startsWith("REPORTE_");
    }

    public static TipoDocumento desdeCodigo(String codigo) {
        if (codigo == null) {
            return null;
        }
        String normalizado = codigo.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        for (TipoDocumento tipo : values()) {
            if (tipo.codigo.equals(normalizado)) {
                return tipo;
            }
        }
        return null;
    }
}
