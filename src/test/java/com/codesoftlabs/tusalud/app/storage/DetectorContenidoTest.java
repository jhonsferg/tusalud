package com.codesoftlabs.tusalud.app.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class DetectorContenidoTest {

    private final DetectorContenido detector = new DetectorContenido();

    static final byte[] PNG = Base64.getDecoder()
            .decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    static byte[] zip(String... entradas) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida)) {
            for (String nombre : entradas) {
                zip.putNextEntry(new ZipEntry(nombre));
                zip.write("<x/>".getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return salida.toByteArray();
    }

    @Test
    void reconocePorFirmaLasImagenesYElPdf() {
        assertThat(detector.detectar(PNG)).contains(TipoContenido.PNG);
        assertThat(detector.detectar(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10}))
                .contains(TipoContenido.JPEG);
        assertThat(detector.detectar("RIFF....WEBPVP8 ".getBytes(StandardCharsets.ISO_8859_1)))
                .contains(TipoContenido.WEBP);
        assertThat(detector.detectar("%PDF-1.7\n1 0 obj".getBytes(StandardCharsets.UTF_8)))
                .contains(TipoContenido.PDF);
    }

    @Test
    void distingueExcelDeWordYRechazaZipsGenericos() throws IOException {
        assertThat(detector.detectar(zip("[Content_Types].xml", "xl/workbook.xml")))
                .contains(TipoContenido.XLSX);
        assertThat(detector.detectar(zip("[Content_Types].xml", "word/document.xml")))
                .contains(TipoContenido.DOCX);
        assertThat(detector.detectar(zip("a.txt", "b.txt"))).isEmpty();
        assertThat(detector.detectar(zip("xl/workbook.xml", "word/document.xml")))
                .isEmpty();
        assertThat(detector.detectar(zip("xl/workbook.xml", "../evil.sh"))).isEmpty();
    }

    @Test
    void reconoceCsvYPlantillasJrxml() {
        assertThat(detector.detectar("codigo,nombre\n1,Ana\n".getBytes(StandardCharsets.UTF_8)))
                .contains(TipoContenido.CSV);
        assertThat(detector.detectar("a;b;c\n1;2;3".getBytes(StandardCharsets.UTF_8)))
                .contains(TipoContenido.CSV);
        String jrxml =
                "<?xml version=\"1.0\"?><jasperReport xmlns=\"http://jasperreports.sourceforge.net/jasperreports\""
                        + " name=\"r\"><title/></jasperReport>";
        assertThat(detector.detectar(jrxml.getBytes(StandardCharsets.UTF_8))).contains(TipoContenido.JRXML);
    }

    @Test
    void rechazaEjecutablesScriptsYMarcado() {
        assertThat(detector.detectar(new byte[] {'M', 'Z', (byte) 0x90, 0, 3, 0, 0, 0}))
                .isEmpty();
        assertThat(detector.detectar(new byte[] {0x7F, 'E', 'L', 'F', 2, 1, 1, 0}))
                .isEmpty();
        assertThat(detector.detectar("#!/bin/sh\nrm -rf /\n".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(detector.detectar("<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(detector.detectar(
                        "<svg xmlns='http://www.w3.org/2000/svg'><script/></svg>".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(detector.detectar("{\"a\":1}".getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
    }

    @Test
    void rechazaXmlConDoctypeYBinariosDisfrazadosDeTexto() {
        String xxe = "<?xml version=\"1.0\"?><!DOCTYPE jasperReport [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>"
                + "<jasperReport name=\"r\">&x;</jasperReport>";
        assertThat(detector.detectar(xxe.getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(detector.detectar(new byte[] {'a', ',', 'b', 0, 1, 2, 3})).isEmpty();
        assertThat(detector.detectar(new byte[] {1, 2})).isEmpty();
        assertThat(detector.detectar(null)).isEmpty();
    }
}
