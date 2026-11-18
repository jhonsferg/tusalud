package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public final class LibroExcel {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LibroExcel.class);

    public static final int FILAS_MAXIMAS = 100_000;
    private static final int VENTANA = 200;
    private static final int ANCHO_MAXIMO = 60;
    private static final int LARGO_MAXIMO_CELDA = 32_000;

    private LibroExcel() {}

    public static byte[] construir(String generadoPor, String codigo, String titulo, List<HojaExcel> hojas) {
        long total = hojas.stream().mapToLong(hoja -> hoja.filas().size()).sum();
        if (total > FILAS_MAXIMAS) {
            throw new DatosInvalidosException(
                    "La exportación supera el máximo de 100 000 filas, acote el rango de fechas o los filtros");
        }
        SXSSFWorkbook libro = new SXSSFWorkbook(VENTANA);
        try (ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            CreationHelper ayuda = libro.getCreationHelper();
            CellStyle cabecera = libro.createCellStyle();
            Font negrita = libro.createFont();
            negrita.setBold(true);
            negrita.setColor(IndexedColors.WHITE.getIndex());
            cabecera.setFont(negrita);
            cabecera.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            CellStyle fecha = libro.createCellStyle();
            fecha.setDataFormat(ayuda.createDataFormat().getFormat("dd/mm/yyyy"));
            CellStyle fechaHora = libro.createCellStyle();
            fechaHora.setDataFormat(ayuda.createDataFormat().getFormat("dd/mm/yyyy hh:mm"));
            CellStyle decimal = libro.createCellStyle();
            decimal.setDataFormat(ayuda.createDataFormat().getFormat("#,##0.00"));

            for (HojaExcel datos : hojas) {
                escribirHoja(libro, datos, cabecera, fecha, fechaHora, decimal);
            }
            escribirInformacion(libro, generadoPor, codigo, titulo, cabecera);
            libro.write(salida);
            return normalizar(salida.toByteArray());
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo generar el libro de Excel", ex);
        } finally {
            libro.dispose();
            cerrar(libro);
        }
    }

    private static byte[] normalizar(byte[] original) throws IOException {
        try (org.apache.commons.compress.archivers.zip.ZipFile zip =
                        org.apache.commons.compress.archivers.zip.ZipFile.builder()
                                .setByteArray(original)
                                .get();
                ByteArrayOutputStream salida = new ByteArrayOutputStream();
                java.util.zip.ZipOutputStream nuevo = new java.util.zip.ZipOutputStream(salida)) {
            java.util.Enumeration<org.apache.commons.compress.archivers.zip.ZipArchiveEntry> entradas =
                    zip.getEntries();
            while (entradas.hasMoreElements()) {
                org.apache.commons.compress.archivers.zip.ZipArchiveEntry entrada = entradas.nextElement();
                nuevo.putNextEntry(new java.util.zip.ZipEntry(entrada.getName()));
                try (java.io.InputStream contenido = zip.getInputStream(entrada)) {
                    contenido.transferTo(nuevo);
                }
                nuevo.closeEntry();
            }
            nuevo.finish();
            return salida.toByteArray();
        }
    }

    private static void cerrar(SXSSFWorkbook libro) {
        try {
            libro.close();
        } catch (IOException ex) {
            log.warn("No se pudo cerrar el libro de Excel: {}", ex.getMessage());
        }
    }

    private static void escribirHoja(
            SXSSFWorkbook libro,
            HojaExcel datos,
            CellStyle cabecera,
            CellStyle fecha,
            CellStyle fechaHora,
            CellStyle decimal) {
        SXSSFSheet hoja = libro.createSheet(nombreHoja(datos.nombre()));
        hoja.trackAllColumnsForAutoSizing();
        Row fila = hoja.createRow(0);
        for (int i = 0; i < datos.columnas().size(); i++) {
            Cell celda = fila.createCell(i);
            celda.setCellValue(sanear(datos.columnas().get(i)));
            celda.setCellStyle(cabecera);
        }
        hoja.createFreezePane(0, 1);
        int indice = 1;
        for (List<Object> valores : datos.filas()) {
            Row actual = hoja.createRow(indice++);
            for (int i = 0; i < valores.size(); i++) {
                escribirCelda(actual.createCell(i), valores.get(i), fecha, fechaHora, decimal);
            }
        }
        for (int i = 0; i < datos.columnas().size(); i++) {
            hoja.autoSizeColumn(i);
            int ancho = hoja.getColumnWidth(i);
            hoja.setColumnWidth(i, Math.min(ancho + 512, ANCHO_MAXIMO * 256));
        }
        hoja.untrackAllColumnsForAutoSizing();
    }

    private static void escribirInformacion(
            SXSSFWorkbook libro, String generadoPor, String codigo, String titulo, CellStyle cabecera) {
        SXSSFSheet hoja = libro.createSheet("Información");
        String[][] datos = {
            {"Documento", titulo},
            {"Generado por", generadoPor},
            {"Generado el", Formato.fechaHora(LocalDateTime.now())},
            {"Código de verificación", codigo}
        };
        for (int i = 0; i < datos.length; i++) {
            Row fila = hoja.createRow(i);
            Cell etiqueta = fila.createCell(0);
            etiqueta.setCellValue(datos[i][0]);
            etiqueta.setCellStyle(cabecera);
            fila.createCell(1).setCellValue(sanear(datos[i][1]));
        }
        hoja.setColumnWidth(0, 26 * 256);
        hoja.setColumnWidth(1, 52 * 256);
    }

    private static void escribirCelda(
            Cell celda, Object valor, CellStyle fecha, CellStyle fechaHora, CellStyle decimal) {
        if (valor == null) {
            celda.setBlank();
        } else if (valor instanceof Number numero && !(valor instanceof BigDecimal)) {
            celda.setCellValue(numero.doubleValue());
        } else if (valor instanceof BigDecimal numero) {
            celda.setCellValue(numero.doubleValue());
            celda.setCellStyle(decimal);
        } else if (valor instanceof Boolean booleano) {
            celda.setCellValue(booleano ? "Sí" : "No");
        } else if (valor instanceof LocalDate dia) {
            celda.setCellValue(dia);
            celda.setCellStyle(fecha);
        } else if (valor instanceof LocalDateTime momento) {
            celda.setCellValue(momento);
            celda.setCellStyle(fechaHora);
        } else if (valor instanceof LocalTime hora) {
            celda.setCellValue(Formato.hora(hora));
        } else {
            celda.setCellValue(sanear(valor.toString()));
        }
    }

    public static String sanear(String texto) {
        if (texto == null) {
            return "";
        }
        String limpio = texto.replace('\u0000', ' ');
        if (limpio.length() > LARGO_MAXIMO_CELDA) {
            limpio = limpio.substring(0, LARGO_MAXIMO_CELDA);
        }
        if (!limpio.isEmpty()) {
            char primero = limpio.charAt(0);
            if (primero == '='
                    || primero == '+'
                    || primero == '-'
                    || primero == '@'
                    || primero == '\t'
                    || primero == '\r') {
                return "'" + limpio;
            }
        }
        return limpio;
    }

    private static String nombreHoja(String nombre) {
        String seguro = WorkbookUtil.createSafeSheetName(nombre == null || nombre.isBlank() ? "Datos" : nombre);
        return seguro.length() > 31 ? seguro.substring(0, 31) : seguro;
    }
}
