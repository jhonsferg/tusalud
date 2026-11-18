package com.codesoftlabs.tusalud.app.storage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

@Component
public class DetectorContenido {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int MAX_ENTRADAS_ZIP = 2000;
    private static final long MAX_BYTES_ZIP_DESCOMPRIMIDOS = 200L * 1024 * 1024;
    private static final int MAX_BYTES_TEXTO = 25 * 1024 * 1024;

    public Optional<TipoContenido> detectar(byte[] contenido) {
        if (contenido == null || contenido.length < 4) {
            return Optional.empty();
        }
        if (esJpeg(contenido)) {
            return Optional.of(TipoContenido.JPEG);
        }
        if (empieza(contenido, PNG)) {
            return Optional.of(TipoContenido.PNG);
        }
        if (esWebp(contenido)) {
            return Optional.of(TipoContenido.WEBP);
        }
        if (empieza(contenido, new byte[] {'%', 'P', 'D', 'F', '-'})) {
            return Optional.of(TipoContenido.PDF);
        }
        if (contenido[0] == 'P' && contenido[1] == 'K' && contenido[2] == 3 && contenido[3] == 4) {
            return detectarOffice(contenido);
        }
        return detectarTexto(contenido);
    }

    private static boolean esJpeg(byte[] contenido) {
        return (contenido[0] & 0xFF) == 0xFF && (contenido[1] & 0xFF) == 0xD8 && (contenido[2] & 0xFF) == 0xFF;
    }

    private static boolean esWebp(byte[] contenido) {
        return contenido.length >= 12
                && contenido[0] == 'R'
                && contenido[1] == 'I'
                && contenido[2] == 'F'
                && contenido[3] == 'F'
                && contenido[8] == 'W'
                && contenido[9] == 'E'
                && contenido[10] == 'B'
                && contenido[11] == 'P';
    }

    private static boolean empieza(byte[] contenido, byte[] prefijo) {
        if (contenido.length < prefijo.length) {
            return false;
        }
        for (int i = 0; i < prefijo.length; i++) {
            if (contenido[i] != prefijo[i]) {
                return false;
            }
        }
        return true;
    }

    private Optional<TipoContenido> detectarOffice(byte[] contenido) {
        boolean xlsx = false;
        boolean docx = false;
        long descomprimidos = 0;
        int entradas = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            ZipEntry entrada;
            while ((entrada = zip.getNextEntry()) != null) {
                if (++entradas > MAX_ENTRADAS_ZIP) {
                    return Optional.empty();
                }
                String nombre = entrada.getName();
                if (nombre.contains("..") || nombre.startsWith("/") || nombre.contains("\\")) {
                    return Optional.empty();
                }
                if ("xl/workbook.xml".equals(nombre)) {
                    xlsx = true;
                }
                if ("word/document.xml".equals(nombre)) {
                    docx = true;
                }
                int leidos;
                while ((leidos = zip.read(buffer)) > 0) {
                    descomprimidos += leidos;
                    if (descomprimidos > MAX_BYTES_ZIP_DESCOMPRIMIDOS) {
                        return Optional.empty();
                    }
                }
            }
        } catch (IOException ex) {
            return Optional.empty();
        }
        if (xlsx && !docx) {
            return Optional.of(TipoContenido.XLSX);
        }
        if (docx && !xlsx) {
            return Optional.of(TipoContenido.DOCX);
        }
        return Optional.empty();
    }

    private Optional<TipoContenido> detectarTexto(byte[] contenido) {
        if (contenido.length > MAX_BYTES_TEXTO) {
            return Optional.empty();
        }
        String texto;
        try {
            texto = StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(contenido))
                    .toString();
        } catch (CharacterCodingException ex) {
            return Optional.empty();
        }
        if (texto.startsWith("﻿")) {
            texto = texto.substring(1);
        }
        if (tieneControlesNoPermitidos(texto)) {
            return Optional.empty();
        }
        String recortado = texto.stripLeading();
        if (recortado.startsWith("<")) {
            return esJrxml(texto) ? Optional.of(TipoContenido.JRXML) : Optional.empty();
        }
        if (recortado.startsWith("#!") || recortado.startsWith("{") || recortado.startsWith("[")) {
            return Optional.empty();
        }
        return esCsv(texto) ? Optional.of(TipoContenido.CSV) : Optional.empty();
    }

    private static boolean tieneControlesNoPermitidos(String texto) {
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c < 0x20 && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        return false;
    }

    private static boolean esCsv(String texto) {
        String primera = texto.lines().findFirst().orElse("");
        return !primera.isBlank() && (primera.contains(",") || primera.contains(";") || primera.contains("\t"));
    }

    private boolean esJrxml(String texto) {
        try {
            DocumentBuilderFactory fabrica = DocumentBuilderFactory.newInstance();
            fabrica.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setFeature("http://xml.org/sax/features/external-general-entities", false);
            fabrica.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);
            fabrica.setNamespaceAware(true);
            DocumentBuilder constructor = fabrica.newDocumentBuilder();
            constructor.setErrorHandler(new DefaultHandler());
            Document documento = constructor.parse(new InputSource(new java.io.StringReader(texto)));
            return "jasperReport".equals(documento.getDocumentElement().getLocalName());
        } catch (Exception ex) {
            return false;
        }
    }
}
