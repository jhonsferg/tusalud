package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.entities.Poliza;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class VerificadorCoberturaSimulado implements VerificadorCobertura {

    @Override
    public ResultadoVerificacion verificar(Poliza poliza, BigDecimal consumidoAnual, LocalDate fecha) {
        EstadoPoliza estado = poliza.estadoEn(fecha);
        boolean convenioVigente = poliza.getConvenio().vigenteEn(fecha);
        boolean vigente =
                estado == EstadoPoliza.ACTIVA && !fecha.isBefore(poliza.getVigenciaDesde()) && convenioVigente;
        String texto;
        if (estado == EstadoPoliza.SUSPENDIDA) {
            texto = "SUSPENDIDA";
        } else if (estado == EstadoPoliza.VENCIDA) {
            texto = "VENCIDA";
        } else if (!convenioVigente) {
            texto = "CONVENIO_NO_VIGENTE";
        } else if (fecha.isBefore(poliza.getVigenciaDesde())) {
            texto = "AUN_NO_VIGENTE";
        } else {
            texto = "VIGENTE";
        }
        BigDecimal tope = poliza.getConvenio().getTopeAnual();
        BigDecimal saldo = tope == null
                ? null
                : tope.subtract(consumidoAnual == null ? BigDecimal.ZERO : consumidoAnual)
                        .max(BigDecimal.ZERO)
                        .setScale(2, RoundingMode.HALF_UP);
        return new ResultadoVerificacion(
                vigente,
                texto,
                saldo,
                codigo(poliza.getNumero(), fecha),
                "Verificación simulada, sin conexión con la aseguradora");
    }

    private static String codigo(String numero, LocalDate fecha) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((numero.trim().toUpperCase() + "|" + fecha).getBytes(StandardCharsets.UTF_8));
            return "VC-" + HexFormat.of().withUpperCase().formatHex(hash, 0, 5);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
