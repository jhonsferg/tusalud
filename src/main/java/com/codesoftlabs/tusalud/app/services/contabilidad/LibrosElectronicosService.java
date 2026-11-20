package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.LibroElectronico;
import com.codesoftlabs.tusalud.app.entities.Asiento;
import com.codesoftlabs.tusalud.app.entities.AsientoLinea;
import com.codesoftlabs.tusalud.app.exceptions.ConflictException;
import com.codesoftlabs.tusalud.app.repositories.AsientoRepository;
import com.codesoftlabs.tusalud.app.services.contabilidad.OrigenesContables.CompraContable;
import com.codesoftlabs.tusalud.app.services.contabilidad.OrigenesContables.Emisor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LibrosElectronicosService {

    public static final String ESTADO_FORMATO = "POR_VALIDAR";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String DIARIO = "050100";
    private static final String MAYOR = "060100";
    private static final String COMPRAS = "080100";

    public enum Libro {
        DIARIO(LibrosElectronicosService.DIARIO),
        MAYOR(LibrosElectronicosService.MAYOR),
        COMPRAS(LibrosElectronicosService.COMPRAS);

        private final String codigo;

        Libro(String codigo) {
            this.codigo = codigo;
        }

        public String codigo() {
            return codigo;
        }
    }

    private final AsientoRepository asientos;
    private final OrigenesContables origenes;

    public LibroElectronico generar(Libro libro, int anio, int mes) {
        Emisor emisor = origenes.emisor()
                .orElseThrow(() -> new ConflictException("Defina la configuración fiscal con el RUC del emisor"));
        LocalDate inicio = LocalDate.of(anio, mes, 1);
        LocalDate fin = inicio.plusMonths(1).minusDays(1);
        String periodo = String.format("%04d%02d00", anio, mes);
        StringBuilder texto = new StringBuilder();
        int registros = switch (libro) {
            case DIARIO -> diario(texto, periodo, asientos.contabilizadosEntre(inicio, fin));
            case MAYOR -> mayor(texto, periodo, asientos.contabilizadosEntre(inicio, fin));
            case COMPRAS -> compras(texto, periodo, origenes.comprasEntre(inicio, fin));
        };
        String nombre = String.format(
                "LE%s%04d%02d00%s00%d%d%d1.txt", emisor.ruc(), anio, mes, libro.codigo(), 1, registros > 0 ? 1 : 0, 1);
        return new LibroElectronico(nombre, texto.toString(), registros, ESTADO_FORMATO);
    }

    private int diario(StringBuilder texto, String periodo, List<Asiento> lista) {
        int registros = 0;
        for (Asiento asiento : lista) {
            for (AsientoLinea linea : asiento.getLineas()) {
                registros++;
                campos(
                        texto,
                        periodo,
                        asiento.getNumero(),
                        "M" + (linea.getPosicion() + 1),
                        linea.getCuenta().getCodigo(),
                        "",
                        "",
                        "PEN",
                        "",
                        "",
                        "",
                        "",
                        "",
                        asiento.getFecha().format(FECHA),
                        "",
                        asiento.getFecha().format(FECHA),
                        limpio(linea.getGlosa() == null ? asiento.getGlosa() : linea.getGlosa()),
                        "",
                        importe(linea.getDebe()),
                        importe(linea.getHaber()),
                        "",
                        "1");
            }
        }
        return registros;
    }

    private int mayor(StringBuilder texto, String periodo, List<Asiento> lista) {
        Map<String, List<AsientoLinea>> porCuenta = new TreeMap<>();
        for (Asiento asiento : lista) {
            for (AsientoLinea linea : asiento.getLineas()) {
                porCuenta
                        .computeIfAbsent(linea.getCuenta().getCodigo(), k -> new java.util.ArrayList<>())
                        .add(linea);
            }
        }
        int registros = 0;
        for (Map.Entry<String, List<AsientoLinea>> entrada : porCuenta.entrySet()) {
            int correlativo = 0;
            for (AsientoLinea linea : entrada.getValue()) {
                registros++;
                correlativo++;
                campos(
                        texto,
                        periodo,
                        linea.getAsiento().getNumero(),
                        "M" + correlativo,
                        entrada.getKey(),
                        "",
                        "",
                        "PEN",
                        importe(linea.getDebe()),
                        importe(linea.getHaber()),
                        "",
                        "1");
            }
        }
        return registros;
    }

    private int compras(StringBuilder texto, String periodo, List<CompraContable> lista) {
        int registros = 0;
        for (CompraContable compra : lista) {
            registros++;
            campos(
                    texto,
                    periodo,
                    compra.serie() + "-" + compra.numero(),
                    "M" + registros,
                    compra.fechaEmision().format(FECHA),
                    compra.fechaVencimiento().format(FECHA),
                    tipoComprobante(compra.tipo()),
                    compra.serie(),
                    "",
                    compra.numero(),
                    "6",
                    compra.rucProveedor(),
                    limpio(compra.proveedor()),
                    importe(compra.subtotal()),
                    importe(compra.igv()),
                    importe(compra.total()),
                    "PEN",
                    "1");
        }
        return registros;
    }

    private static void campos(StringBuilder texto, String... valores) {
        for (String valor : valores) {
            texto.append(valor).append('|');
        }
        texto.append('\n');
    }

    private static String tipoComprobante(String tipo) {
        return switch (tipo) {
            case "FACTURA" -> "01";
            case "BOLETA" -> "03";
            default -> "00";
        };
    }

    private static String importe(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static String limpio(String texto) {
        return texto == null ? "" : texto.replace('|', '/').replace('\n', ' ').strip();
    }
}
