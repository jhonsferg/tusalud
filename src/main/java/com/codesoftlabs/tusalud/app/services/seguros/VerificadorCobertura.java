package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.entities.Poliza;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface VerificadorCobertura {

    ResultadoVerificacion verificar(Poliza poliza, BigDecimal consumidoAnual, LocalDate fecha);

    record ResultadoVerificacion(boolean vigente, String estado, BigDecimal saldoTope, String codigo, String mensaje) {}
}
