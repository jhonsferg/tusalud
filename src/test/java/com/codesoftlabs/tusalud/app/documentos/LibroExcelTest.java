package com.codesoftlabs.tusalud.app.documentos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class LibroExcelTest {

    private static XSSFWorkbook abrir(byte[] contenido) throws Exception {
        return new XSSFWorkbook(new ByteArrayInputStream(contenido));
    }

    @Test
    void generaUnLibroValidoConTiposYTildes() throws Exception {
        HojaExcel hoja = HojaExcel.de("Citas del año", "Paciente", "Monto", "Fecha", "Hora", "Cantidad")
                .fila(
                        "José Núñez",
                        new BigDecimal("118.50"),
                        LocalDate.of(2026, 11, 17),
                        LocalDateTime.of(2026, 11, 17, 9, 30),
                        4);

        byte[] libro = LibroExcel.construir("Ñoño Pérez", "ABCD-1234-EF56", "Prueba", List.of(hoja));

        assertThat(libro[0]).isEqualTo((byte) 'P');
        assertThat(libro[1]).isEqualTo((byte) 'K');
        try (XSSFWorkbook leido = abrir(libro)) {
            assertThat(leido.getSheetName(0)).isEqualTo("Citas del año");
            assertThat(leido.getSheetName(1)).isEqualTo("Información");
            Sheet datos = leido.getSheetAt(0);
            assertThat(datos.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Paciente");
            assertThat(datos.getRow(1).getCell(0).getStringCellValue()).isEqualTo("José Núñez");
            assertThat(datos.getRow(1).getCell(1).getNumericCellValue()).isEqualTo(118.5);
            assertThat(datos.getRow(1).getCell(2).getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(datos.getRow(1).getCell(4).getNumericCellValue()).isEqualTo(4);
            Sheet informacion = leido.getSheet("Información");
            assertThat(informacion.getRow(1).getCell(1).getStringCellValue()).isEqualTo("Ñoño Pérez");
            assertThat(informacion.getRow(3).getCell(1).getStringCellValue()).isEqualTo("ABCD-1234-EF56");
        }
    }

    @Test
    void neutralizaLaInyeccionDeFormulas() throws Exception {
        HojaExcel hoja = HojaExcel.de("Datos", "Texto")
                .fila("=HYPERLINK(\"http://malo\",\"x\")")
                .fila("+cmd|' /C calc'!A0")
                .fila("-2+3")
                .fila("@SUM(1+1)")
                .fila("normal");

        try (XSSFWorkbook leido = abrir(LibroExcel.construir("Admin", "CODIGO", "Prueba", List.of(hoja)))) {
            Sheet datos = leido.getSheetAt(0);
            for (int fila = 1; fila <= 4; fila++) {
                Cell celda = datos.getRow(fila).getCell(0);
                assertThat(celda.getCellType()).isEqualTo(CellType.STRING);
                assertThat(celda.getStringCellValue()).startsWith("'");
            }
            assertThat(datos.getRow(5).getCell(0).getStringCellValue()).isEqualTo("normal");
        }
    }

    @Test
    void sanearPrefijaSoloLosPeligrososYRecortaLoExcesivo() {
        assertThat(LibroExcel.sanear("=1+1")).isEqualTo("'=1+1");
        assertThat(LibroExcel.sanear("\tabc")).isEqualTo("'\tabc");
        assertThat(LibroExcel.sanear("Hola = mundo")).isEqualTo("Hola = mundo");
        assertThat(LibroExcel.sanear(null)).isEmpty();
        assertThat(LibroExcel.sanear("a".repeat(40_000))).hasSize(32_000);
    }

    @Test
    void limitaLasFilasYSaneaElNombreDeLaHoja() throws Exception {
        HojaExcel enorme = HojaExcel.de("Datos", "N");
        for (int i = 0; i <= LibroExcel.FILAS_MAXIMAS; i++) {
            enorme.fila(i);
        }
        assertThatThrownBy(() -> LibroExcel.construir("Admin", "C", "T", List.of(enorme)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("100 000");

        HojaExcel rara = HojaExcel.de("Nombre/con:caracteres*[raros]?demasiado largo para excel", "N")
                .fila(1);
        try (XSSFWorkbook leido = abrir(LibroExcel.construir("Admin", "C", "T", List.of(rara)))) {
            assertThat(leido.getSheetName(0)).hasSizeLessThanOrEqualTo(31).doesNotContain("/", ":", "*", "[", "]", "?");
        }
    }
}
