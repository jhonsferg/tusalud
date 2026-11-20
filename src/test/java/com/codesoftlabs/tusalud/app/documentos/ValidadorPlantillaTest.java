package com.codesoftlabs.tusalud.app.documentos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ValidadorPlantillaTest {

    private static byte[] base() throws Exception {
        try (InputStream entrada = ValidadorPlantillaTest.class.getResourceAsStream("/reportes/constancia.jrxml")) {
            return entrada.readAllBytes();
        }
    }

    private static byte[] con(String reemplazo, String por) throws Exception {
        return new String(base(), StandardCharsets.UTF_8)
                .replace(reemplazo, por)
                .getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] conExpresion(String expresion) {
        String xml = "<jasperReport name=\"x\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"515\">"
                + "<parameter name=\"p\" class=\"java.lang.String\"/>"
                + "<title height=\"20\"><element kind=\"textField\" x=\"0\" y=\"0\" width=\"100\" height=\"14\">"
                + "<expression><![CDATA[" + expresion + "]]></expression></element></title></jasperReport>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @ParameterizedTest
    @EnumSource(
            value = TipoDocumento.class,
            names = {
                "COMPROBANTE",
                "RECETA",
                "RESULTADOS_LABORATORIO",
                "INFORME_IMAGEN",
                "CONSTANCIA",
                "RESUMEN_HISTORIA",
                "REPORTE_EJECUTIVO",
                "REPORTE_FINANCIERO",
                "REPORTE_OPERATIVO",
                "REPORTE_CALIDAD",
                "REPORTE_CONTABLE"
            })
    void aceptaLasPlantillasPorDefecto(TipoDocumento tipo) throws Exception {
        try (InputStream entrada = getClass().getResourceAsStream("/reportes/" + tipo.plantilla() + ".jrxml")) {
            assertThat(entrada).isNotNull();
            byte[] contenido = entrada.readAllBytes();
            assertThatCode(() -> ValidadorPlantilla.validar(contenido)).doesNotThrowAnyException();
        }
    }

    @Test
    void aceptaExpresionesSimples() {
        assertThatCode(() -> ValidadorPlantilla.validar(conExpresion("\"Hola \" + $P{p} + \" · \" + $P{p}")))
                .doesNotThrowAnyException();
        assertThatCode(() -> ValidadorPlantilla.validar(conExpresion("$P{p} != null && !$P{p}.isEmpty()")))
                .doesNotThrowAnyException();
    }

    @Test
    void rechazaEjecucionDeCodigo() {
        assertThatThrownBy(() -> ValidadorPlantilla.validar(conExpresion("System.exit(1)")))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(
                        () -> ValidadorPlantilla.validar(conExpresion("java.lang.Runtime.getRuntime().exec(\"calc\")")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("Runtime");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(conExpresion("$P{p}.getClass().getName()")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("getClass");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(conExpresion("new java.io.File(\"/etc/passwd\").exists()")))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rechazaEscapesUnicodeQueRompenLosLiterales() {
        assertThatThrownBy(() -> ValidadorPlantilla.validar(conExpresion(
                        "\"x\\u0022 + java.lang.Runtime.getRuntime().exec(\\u0022calc\\u0022) + \\u0022\"")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("escape");
    }

    @Test
    void rechazaScriptletsImportacionesYSubinformes() throws Exception {
        assertThatThrownBy(() -> ValidadorPlantilla.validar(
                        con("name=\"constancia\"", "name=\"constancia\" scriptletClass=\"x.Y\"")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("scriptletClass");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(con("<title ", "<import value=\"java.io.File\"/><title ")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("import");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(con("kind=\"line\"", "kind=\"subreport\"")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("subreport");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(con("<title ", "<scriptlet name=\"s\"/><title ")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("scriptlet");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(con("<title ", "<queryString language=\"sql\"/><title ")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("queryString");
    }

    @Test
    void rechazaImagenesQueNoVienenDeParametros() throws Exception {
        String externa = "<jasperReport name=\"x\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"515\">"
                + "<title height=\"20\"><element kind=\"image\" x=\"0\" y=\"0\" width=\"10\" height=\"10\">"
                + "<expression><![CDATA[\"http://malo.example/x.png\"]]></expression></element></title></jasperReport>";
        assertThatThrownBy(() -> ValidadorPlantilla.validar(externa.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("imágenes");
        String archivo = externa.replace("\"http://malo.example/x.png\"", "\"/etc/passwd\"");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(archivo.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rechazaEntidadesExternasYTiposNoPermitidos() throws Exception {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE jasperReport [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<jasperReport name=\"x\" pageWidth=\"595\" pageHeight=\"842\" columnWidth=\"515\"/>";
        assertThatThrownBy(() -> ValidadorPlantilla.validar(xxe.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(
                        () -> ValidadorPlantilla.validar(con("class=\"java.lang.String\"", "class=\"java.io.File\"")))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("tipo de dato");
        assertThatThrownBy(() -> ValidadorPlantilla.validar(con(
                        "<jasperReport ",
                        "<jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\" ")))
                .isInstanceOf(DatosInvalidosException.class);
    }

    @Test
    void rechazaPlantillasVaciasEnormesOQueNoSonJrxml() {
        assertThatThrownBy(() -> ValidadorPlantilla.validar(new byte[0])).isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> ValidadorPlantilla.validar(new byte[ValidadorPlantilla.TAMANO_MAXIMO + 1]))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("512 KB");
        assertThatThrownBy(() -> ValidadorPlantilla.validar("<html/>".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatosInvalidosException.class);
        assertThatThrownBy(() -> ValidadorPlantilla.validar("no es xml".getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatosInvalidosException.class);
    }
}
