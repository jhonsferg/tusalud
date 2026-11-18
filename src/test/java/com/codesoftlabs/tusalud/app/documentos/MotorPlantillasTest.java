package com.codesoftlabs.tusalud.app.documentos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MotorPlantillasTest {

    private final AtomicReference<byte[]> personalizada = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>(ProveedorPlantillas.CLAVE_DEFECTO);
    private MotorPlantillas motor;

    @BeforeEach
    void preparar() {
        ProveedorPlantillas proveedor = new ProveedorPlantillas() {
            @Override
            public String claveActiva(String codigo) {
                return clave.get();
            }

            @Override
            public byte[] contenido(String codigo) {
                return personalizada.get() != null ? personalizada.get() : contenidoPorDefecto(codigo);
            }

            @Override
            public byte[] contenidoPorDefecto(String codigo) {
                try (InputStream entrada = getClass().getResourceAsStream("/reportes/" + codigo + ".jrxml")) {
                    return entrada.readAllBytes();
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        };
        motor = new MotorPlantillas(proveedor);
    }

    private static String texto(byte[] pdf) throws IOException {
        PdfReader lector = new PdfReader(pdf);
        try {
            PdfTextExtractor extractor = new PdfTextExtractor(lector);
            StringBuilder todo = new StringBuilder();
            for (int pagina = 1; pagina <= lector.getNumberOfPages(); pagina++) {
                todo.append(extractor.getTextFromPage(pagina)).append('\n');
            }
            return todo.toString();
        } finally {
            lector.close();
        }
    }

    private static FichaDocumento ficha(TipoDocumento tipo) {
        FichaDocumento ficha = FichaDocumento.de(tipo)
                .titulo("Documento de prueba ñandú", "Año 2026")
                .par("Paciente", "José Núñez Ángel")
                .par("Diagnóstico", "Hipertensión leve")
                .nota("Observaciones", "Acción recomendada: reposo y control en 30 días.")
                .firma("Dra. Muñoz", "CMP 12345");
        ficha.parametros().put("generadoPor", "Ñoño Pérez");
        ficha.parametros().put("generadoEn", "17/11/2026 10:00");
        ficha.parametros().put("codigo", "ABCD-1234-EF56");
        if (tipo.esReporte()) {
            ficha.seccion("Indicadores")
                    .cabecera("Indicador", "Actual", "Anterior", "Variación")
                    .fila("Ingresos del año", "S/ 1,200.00", "S/ 900.00", "+33.3 %")
                    .grafico(1, GraficosPdf.barras("Citas", List.of("01/11", "02/11"), List.of(3, 5)))
                    .grafico(2, GraficosPdf.torta("Métodos", List.of("Efectivo", "Yape"), List.of(60, 40)));
        } else {
            ficha.fila("Losartán 50 mg", "1 tableta", "Cada 24 horas", "30 días")
                    .filaFuera(true, "Glucosa", "Glucosa en ayunas", "180", "mg/dL", "70 - 100", "Fuera de rango");
        }
        ficha.total("Total", "S/ 118.00");
        return ficha;
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
                "REPORTE_CALIDAD"
            })
    void generaUnPdfValidoConTildesYEnies(TipoDocumento tipo) throws Exception {
        byte[] pdf = motor.renderizarPdf(ficha(tipo));

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(2_000);
        String extraido = texto(pdf);
        assertThat(extraido).contains("TuSalud").contains("ñandú").contains("José Núñez Ángel");
        assertThat(extraido).contains("Hipertensión").contains("Generado por Ñoño Pérez");
        assertThat(extraido).contains("Código de verificación ABCD-1234-EF56");
    }

    @Test
    void usaLaNuevaVersionDeUnaPlantillaPersonalizadaYVuelveALaPredeterminada() throws Exception {
        byte[] original = motor.renderizarPdf(ficha(TipoDocumento.CONSTANCIA));
        String modificada = new String(
                        motor.getClass()
                                .getResourceAsStream("/reportes/constancia.jrxml")
                                .readAllBytes(),
                        StandardCharsets.UTF_8)
                .replace("TuSalud", "Clínica Ñandú");
        personalizada.set(modificada.getBytes(StandardCharsets.UTF_8));
        clave.set("version-2");

        byte[] nueva = motor.renderizarPdf(ficha(TipoDocumento.CONSTANCIA));
        assertThat(texto(nueva)).contains("Clínica Ñandú");
        assertThat(texto(original)).doesNotContain("Clínica Ñandú");

        personalizada.set(null);
        clave.set(ProveedorPlantillas.CLAVE_DEFECTO);
        assertThat(texto(motor.renderizarPdf(ficha(TipoDocumento.CONSTANCIA)))).doesNotContain("Clínica Ñandú");
    }

    @Test
    void validaCompilandoYRechazaExpresionesQueNoCompilan() throws Exception {
        byte[] valida =
                motor.getClass().getResourceAsStream("/reportes/receta.jrxml").readAllBytes();
        motor.validarCompilando(valida);

        String rota = new String(valida, StandardCharsets.UTF_8).replace("$P{titulo}", "$P{inexistente}");
        assertThatThrownBy(() -> motor.validarCompilando(rota.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("no compila");
    }

    @Test
    void limitaLasFilasDelPdf() {
        FichaDocumento ficha = ficha(TipoDocumento.RECETA);
        for (int i = 0; i <= MotorPlantillas.FILAS_PDF_MAXIMAS; i++) {
            ficha.fila("Medicamento", "1", "2", "3");
        }
        assertThatThrownBy(() -> motor.renderizarPdf(ficha))
                .isInstanceOf(DatosInvalidosException.class)
                .hasMessageContaining("5 000 filas");
    }
}
