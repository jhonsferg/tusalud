package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.entities.Convenio;
import com.codesoftlabs.tusalud.app.entities.ReglaCobertura;
import com.codesoftlabs.tusalud.app.entities.enums.CategoriaServicio;
import com.codesoftlabs.tusalud.app.entities.enums.TipoCobertura;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class CoberturaCalculadora {

    public static final BigDecimal COPAGO_MINIMO = new BigDecimal("0.01");
    private static final BigDecimal CIEN = new BigDecimal("100");

    public record Linea(String codigo, String descripcion, BigDecimal monto) {}

    public record LineaCalculada(
            String descripcion, BigDecimal monto, BigDecimal cubierto, BigDecimal copago, String motivo) {}

    public record Resultado(
            BigDecimal total,
            BigDecimal cubierto,
            BigDecimal copago,
            BigDecimal saldoTopeAnual,
            List<LineaCalculada> lineas) {}

    public Resultado calcular(
            Convenio convenio, CategoriaServicio categoria, List<Linea> lineas, BigDecimal consumidoAnual) {
        ReglaCobertura regla = convenio.getReglas().stream()
                .filter(r -> r.getCategoria() == categoria)
                .findFirst()
                .orElse(null);
        BigDecimal saldo = convenio.getTopeAnual() == null
                ? null
                : convenio.getTopeAnual().subtract(escala(consumidoAnual)).max(BigDecimal.ZERO);
        List<BigDecimal> montos = new ArrayList<>();
        List<BigDecimal> cubiertos = new ArrayList<>();
        List<String> motivos = new ArrayList<>();
        for (Linea linea : lineas) {
            BigDecimal monto = escala(linea.monto());
            BigDecimal cubierto = BigDecimal.ZERO;
            String motivo = null;
            if (regla == null) {
                motivo = "El convenio no cubre esta categoría";
            } else if (excluido(convenio, linea.codigo())) {
                motivo = "Servicio excluido del convenio";
            } else {
                cubierto = base(regla, monto);
                if (regla.getTopePorServicio() != null && cubierto.compareTo(regla.getTopePorServicio()) > 0) {
                    cubierto = escala(regla.getTopePorServicio());
                    motivo = "Tope por servicio aplicado";
                }
                if (saldo != null) {
                    if (cubierto.compareTo(saldo) > 0) {
                        cubierto = saldo;
                        motivo = "Tope anual alcanzado";
                    }
                    saldo = saldo.subtract(cubierto);
                }
            }
            montos.add(monto);
            cubiertos.add(cubierto);
            motivos.add(motivo);
        }
        BigDecimal total = montos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal cubiertoTotal = cubiertos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal copago = total.subtract(cubiertoTotal);
        if (total.compareTo(COPAGO_MINIMO) >= 0 && copago.compareTo(COPAGO_MINIMO) < 0) {
            BigDecimal falta = COPAGO_MINIMO.subtract(copago);
            for (int i = cubiertos.size() - 1; i >= 0 && falta.signum() > 0; i--) {
                BigDecimal ajuste = cubiertos.get(i).min(falta);
                if (ajuste.signum() > 0) {
                    cubiertos.set(i, cubiertos.get(i).subtract(ajuste));
                    motivos.set(i, "Copago mínimo aplicado");
                    falta = falta.subtract(ajuste);
                    if (saldo != null) {
                        saldo = saldo.add(ajuste);
                    }
                }
            }
            cubiertoTotal = cubiertos.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            copago = total.subtract(cubiertoTotal);
        }
        List<LineaCalculada> calculadas = new ArrayList<>();
        for (int i = 0; i < lineas.size(); i++) {
            calculadas.add(new LineaCalculada(
                    lineas.get(i).descripcion(),
                    montos.get(i),
                    cubiertos.get(i),
                    montos.get(i).subtract(cubiertos.get(i)),
                    motivos.get(i)));
        }
        return new Resultado(total, cubiertoTotal, copago, saldo == null ? null : escala(saldo), calculadas);
    }

    private static BigDecimal base(ReglaCobertura regla, BigDecimal monto) {
        if (regla.getTipo() == TipoCobertura.PORCENTAJE) {
            return monto.multiply(regla.getValor())
                    .divide(CIEN, 2, RoundingMode.HALF_UP)
                    .min(monto);
        }
        BigDecimal copagoLinea = regla.getValor().min(monto);
        return escala(monto.subtract(copagoLinea));
    }

    private static boolean excluido(Convenio convenio, String codigo) {
        if (codigo == null) {
            return false;
        }
        String buscado = codigo.trim().toUpperCase(Locale.ROOT);
        return convenio.getExclusiones().stream().anyMatch(e -> e.equalsIgnoreCase(buscado));
    }

    private static BigDecimal escala(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
