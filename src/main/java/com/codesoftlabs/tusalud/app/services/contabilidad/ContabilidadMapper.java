package com.codesoftlabs.tusalud.app.services.contabilidad;

import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.AsientoResumen;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.CuentaContableResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.CuentaTesoreriaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.ExtractoLineaResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.LineaAsientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.MovimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.PlantillaAsientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.ContabilidadResponses.PlantillaLineaResponse;
import com.codesoftlabs.tusalud.app.entities.Asiento;
import com.codesoftlabs.tusalud.app.entities.AsientoLinea;
import com.codesoftlabs.tusalud.app.entities.CuentaContable;
import com.codesoftlabs.tusalud.app.entities.CuentaTesoreria;
import com.codesoftlabs.tusalud.app.entities.ExtractoLinea;
import com.codesoftlabs.tusalud.app.entities.MovimientoTesoreria;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsiento;
import com.codesoftlabs.tusalud.app.entities.PlantillaAsientoLinea;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

final class ContabilidadMapper {

    private ContabilidadMapper() {}

    static CuentaContableResponse cuenta(CuentaContable c) {
        return new CuentaContableResponse(
                c.getId(),
                c.getCodigo(),
                c.getNombre(),
                c.getTipo(),
                c.getNaturaleza(),
                c.isAceptaMovimientos(),
                c.isActiva(),
                c.isSistema());
    }

    static AsientoResumen resumen(Asiento a) {
        return new AsientoResumen(
                a.getId(),
                a.getNumero(),
                a.getFecha(),
                a.getPeriodo().etiqueta(),
                a.getGlosa(),
                a.getOrigen(),
                a.getEstado(),
                a.getTotalDebe(),
                a.getTotalHaber());
    }

    static AsientoResponse asiento(Asiento a, UUID reversaId) {
        List<LineaAsientoResponse> lineas =
                a.getLineas().stream().map(ContabilidadMapper::linea).toList();
        return new AsientoResponse(
                a.getId(),
                a.getNumero(),
                a.getFecha(),
                a.getPeriodo().getId(),
                a.getPeriodo().etiqueta(),
                a.getGlosa(),
                a.getOrigen(),
                a.getReferenciaId(),
                a.getEstado(),
                a.getTotalDebe(),
                a.getTotalHaber(),
                nombre(a.getCreadoPor()),
                a.getCreadoEn(),
                nombre(a.getContabilizadoPor()),
                a.getContabilizadoEn(),
                a.getRevierteA() == null ? null : a.getRevierteA().getId(),
                a.getRevierteA() == null ? null : a.getRevierteA().getNumero(),
                reversaId,
                a.getMotivoReversa(),
                lineas);
    }

    static LineaAsientoResponse linea(AsientoLinea l) {
        return new LineaAsientoResponse(
                l.getId(),
                l.getPosicion(),
                l.getCuenta().getId(),
                l.getCuenta().getCodigo(),
                l.getCuenta().getNombre(),
                l.getDebe(),
                l.getHaber(),
                l.getGlosa());
    }

    static PlantillaAsientoResponse plantilla(PlantillaAsiento p) {
        return new PlantillaAsientoResponse(
                p.getId(),
                p.getEvento(),
                p.getNombre(),
                p.isActiva(),
                p.getLineas().stream().map(ContabilidadMapper::lineaPlantilla).toList());
    }

    static PlantillaLineaResponse lineaPlantilla(PlantillaAsientoLinea l) {
        return new PlantillaLineaResponse(
                l.getId(),
                l.getPosicion(),
                l.getLado(),
                l.getConcepto(),
                l.getCuenta().getId(),
                l.getCuenta().getCodigo(),
                l.getCuenta().getNombre());
    }

    static CuentaTesoreriaResponse tesoreria(CuentaTesoreria c, BigDecimal saldo) {
        return new CuentaTesoreriaResponse(
                c.getId(),
                c.getNombre(),
                c.getTipo(),
                c.getBanco(),
                c.getUltimos4(),
                c.getMoneda(),
                c.getCuentaContable().getId(),
                c.getCuentaContable().getCodigo(),
                c.getSaldoInicial(),
                saldo,
                c.isPredeterminada(),
                c.isActiva());
    }

    static MovimientoResponse movimiento(MovimientoTesoreria m) {
        return new MovimientoResponse(
                m.getId(),
                m.getCuenta().getId(),
                m.getCuenta().getNombre(),
                m.getFecha(),
                m.getTipo(),
                m.getMonto(),
                m.getDescripcion(),
                m.getReferencia(),
                m.getOrigen(),
                m.getAsiento() == null ? null : m.getAsiento().getId(),
                m.isConciliado());
    }

    static ExtractoLineaResponse lineaExtracto(ExtractoLinea l) {
        return new ExtractoLineaResponse(
                l.getId(),
                l.getFecha(),
                l.getDescripcion(),
                l.getReferencia(),
                l.getMonto(),
                l.getEstado(),
                l.getMovimiento() == null ? null : l.getMovimiento().getId());
    }

    private static String nombre(Usuario usuario) {
        return usuario == null ? "Sistema" : usuario.getNombreCompleto();
    }
}
