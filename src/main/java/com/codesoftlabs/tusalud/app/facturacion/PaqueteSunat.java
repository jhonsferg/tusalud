package com.codesoftlabs.tusalud.app.facturacion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class PaqueteSunat {

    public static final String NS_RESPUESTA = "urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2";
    public static final String RUC_SUNAT = "20131312955";

    private static final int MAX_ENTRADAS = 5;
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    public record Cdr(String codigo, String descripcion, List<String> observaciones) {

        public boolean aceptado() {
            return "0".equals(codigo);
        }
    }

    private PaqueteSunat() {}

    public static byte[] comprimir(String nombreXml, byte[] xml) {
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream();
                ZipOutputStream zip = new ZipOutputStream(salida)) {
            zip.putNextEntry(new ZipEntry(nombreXml));
            zip.write(xml);
            zip.closeEntry();
            zip.finish();
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo empaquetar el comprobante", ex);
        }
    }

    public static Map<String, byte[]> descomprimir(byte[] contenido) {
        Map<String, byte[]> archivos = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(contenido))) {
            ZipEntry entrada;
            long total = 0;
            while ((entrada = zip.getNextEntry()) != null) {
                if (archivos.size() >= MAX_ENTRADAS) {
                    throw new IllegalArgumentException("El paquete tiene demasiados archivos");
                }
                String nombre = entrada.getName();
                if (nombre.contains("..") || nombre.startsWith("/") || nombre.contains("\\")) {
                    throw new IllegalArgumentException("El paquete tiene un nombre de archivo no permitido");
                }
                ByteArrayOutputStream salida = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int leidos;
                while ((leidos = zip.read(buffer)) > 0) {
                    total += leidos;
                    if (total > MAX_BYTES) {
                        throw new IllegalArgumentException("El paquete excede el tamaño permitido");
                    }
                    salida.write(buffer, 0, leidos);
                }
                archivos.put(nombre, salida.toByteArray());
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException("El paquete no es un ZIP válido", ex);
        }
        return archivos;
    }

    public static byte[] construirCdr(
            String referencia, String rucEmisor, String codigo, String descripcion, List<String> observaciones) {
        try {
            Document doc =
                    XmlFirmador.analizar(("<ar:ApplicationResponse xmlns:ar=\"" + NS_RESPUESTA + "\" xmlns:cac=\""
                                    + UblGenerador.NS_CAC + "\" xmlns:cbc=\"" + UblGenerador.NS_CBC + "\"/>")
                            .getBytes(StandardCharsets.UTF_8));
            Element raiz = doc.getDocumentElement();
            LocalDateTime ahora = LocalDateTime.now();
            hijo(doc, raiz, "cbc", "UBLVersionID", "2.0");
            hijo(doc, raiz, "cbc", "CustomizationID", "1.0");
            hijo(doc, raiz, "cbc", "ID", ahora.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));
            hijo(doc, raiz, "cbc", "ResponseDate", ahora.toLocalDate().toString());
            hijo(
                    doc,
                    raiz,
                    "cbc",
                    "ResponseTime",
                    ahora.toLocalTime().withNano(0).toString());
            for (String observacion : observaciones) {
                hijo(doc, raiz, "cbc", "Note", observacion);
            }
            Element emisor = hijo(doc, raiz, "cac", "SenderParty", null);
            hijo(doc, hijo(doc, emisor, "cac", "PartyIdentification", null), "cbc", "ID", RUC_SUNAT);
            Element receptor = hijo(doc, raiz, "cac", "ReceiverParty", null);
            hijo(doc, hijo(doc, receptor, "cac", "PartyIdentification", null), "cbc", "ID", rucEmisor);
            Element respuesta = hijo(doc, raiz, "cac", "DocumentResponse", null);
            Element detalle = hijo(doc, respuesta, "cac", "Response", null);
            hijo(doc, detalle, "cbc", "ReferenceID", referencia);
            hijo(doc, detalle, "cbc", "ResponseCode", codigo);
            hijo(doc, detalle, "cbc", "Description", descripcion);
            return XmlFirmador.serializar(doc);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo construir la constancia de recepción", ex);
        }
    }

    public static Cdr leerCdr(byte[] cdrZip) {
        Map<String, byte[]> archivos = descomprimir(cdrZip);
        byte[] xml = archivos.entrySet().stream()
                .filter(e -> e.getKey().toLowerCase().endsWith(".xml"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La constancia no contiene un XML"));
        try {
            Document doc = XmlFirmador.analizar(xml);
            String codigo = primero(doc, "ResponseCode");
            String descripcion = primero(doc, "Description");
            List<String> observaciones = new ArrayList<>();
            NodeList notas = doc.getElementsByTagNameNS(UblGenerador.NS_CBC, "Note");
            for (int i = 0; i < notas.getLength(); i++) {
                observaciones.add(notas.item(i).getTextContent().trim());
            }
            return new Cdr(codigo == null ? "" : codigo, descripcion == null ? "" : descripcion, observaciones);
        } catch (Exception ex) {
            throw new IllegalArgumentException("La constancia de recepción no se pudo leer", ex);
        }
    }

    private static String primero(Document doc, String nombre) {
        NodeList nodos = doc.getElementsByTagNameNS(UblGenerador.NS_CBC, nombre);
        return nodos.getLength() == 0 ? null : nodos.item(0).getTextContent().trim();
    }

    private static Element hijo(Document doc, Element padre, String prefijo, String nombre, String texto) {
        String espacio = prefijo.equals("cac") ? UblGenerador.NS_CAC : UblGenerador.NS_CBC;
        Element e = doc.createElementNS(espacio, prefijo + ":" + nombre);
        if (texto != null) {
            e.setTextContent(texto);
        }
        padre.appendChild(e);
        return e;
    }
}
