package com.codesoftlabs.tusalud.app.documentos;

import com.codesoftlabs.tusalud.app.exceptions.BusinessLogicException;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MotorPlantillas implements DisposableBean {

    public static final int FILAS_PDF_MAXIMAS = 5_000;
    private static final long LIMITE_SEGUNDOS = 90;
    private static final Locale LOCALE = Locale.of("es", "PE");

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final ProveedorPlantillas proveedor;
    private final Map<String, JasperReport> cache = new ConcurrentHashMap<>();
    private final ExecutorService ejecutor = Executors.newVirtualThreadPerTaskExecutor();

    public MotorPlantillas(ProveedorPlantillas proveedor) {
        this.proveedor = proveedor;
    }

    public byte[] renderizarPdf(FichaDocumento ficha) {
        if (ficha.cantidadFilas() > FILAS_PDF_MAXIMAS) {
            throw new DatosInvalidosException(
                    "El PDF supera las 5 000 filas, use la exportación a Excel o acote el rango");
        }
        JasperReport reporte = compilada(ficha.plantilla());
        Map<String, Object> parametros = new HashMap<>(ficha.parametros());
        parametros.put(JRParameter.REPORT_LOCALE, LOCALE);
        JasperPrint impresion = ejecutarConLimite(() -> JasperFillManager.fillReport(
                reporte, parametros, new JRMapCollectionDataSource(List.copyOf(ficha.filas()))));
        return ejecutarConLimite(() -> JasperExportManager.exportReportToPdf(impresion));
    }

    public void invalidar(String codigo) {
        cache.keySet().removeIf(clave -> clave.startsWith(codigo + "#"));
    }

    public void validarCompilando(byte[] jrxml) {
        ValidadorPlantilla.validar(jrxml);
        try {
            ejecutarConLimite(() -> JasperCompileManager.compileReport(new ByteArrayInputStream(jrxml)));
        } catch (DatosInvalidosException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.info("Plantilla rechazada al compilar: {}", ex.getMessage());
            throw new DatosInvalidosException("La plantilla no compila: " + causa(ex));
        }
    }

    private JasperReport compilada(String codigo) {
        String clave = codigo + "#" + proveedor.claveActiva(codigo);
        JasperReport existente = cache.get(clave);
        if (existente != null) {
            return existente;
        }
        byte[] contenido = proveedor.contenido(codigo);
        JasperReport nueva = ejecutarConLimite(() -> {
            ValidadorPlantilla.validar(contenido);
            return JasperCompileManager.compileReport(new ByteArrayInputStream(contenido));
        });
        invalidar(codigo);
        cache.put(clave, nueva);
        return nueva;
    }

    private <T> T ejecutarConLimite(Callable<T> tarea) {
        Future<T> futuro = ejecutor.submit(tarea);
        try {
            return futuro.get(LIMITE_SEGUNDOS, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            futuro.cancel(true);
            throw new BusinessLogicException("La generación del documento tardó demasiado, acote el rango");
        } catch (InterruptedException ex) {
            futuro.cancel(true);
            Thread.currentThread().interrupt();
            throw new BusinessLogicException("La generación del documento fue interrumpida");
        } catch (ExecutionException ex) {
            Throwable causa = ex.getCause();
            if (causa instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (causa instanceof JRException) {
                log.warn("Falló JasperReports: {}", causa.getMessage());
                throw new IllegalStateException("No se pudo generar el documento: " + causa.getMessage(), causa);
            }
            throw new IllegalStateException("No se pudo generar el documento", causa);
        }
    }

    private static String causa(Throwable error) {
        Throwable actual = error;
        while (actual.getCause() != null && actual.getCause() != actual) {
            actual = actual.getCause();
        }
        String mensaje = actual.getMessage();
        if (mensaje == null || mensaje.isBlank()) {
            return "revise la sintaxis de la plantilla";
        }
        return mensaje.length() > 200 ? mensaje.substring(0, 200) : mensaje;
    }

    @Override
    public void destroy() {
        ejecutor.shutdownNow();
    }
}
