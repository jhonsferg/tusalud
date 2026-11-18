package com.codesoftlabs.tusalud.app.services.seguros;

import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.AseguradoraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.ConvenioResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.CuentaAseguradoraResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.LiquidacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.MiPolizaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PolizaResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.PreautorizacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.SegurosResponses.ReglaResponse;
import com.codesoftlabs.tusalud.app.entities.Aseguradora;
import com.codesoftlabs.tusalud.app.entities.Convenio;
import com.codesoftlabs.tusalud.app.entities.CuentaAseguradora;
import com.codesoftlabs.tusalud.app.entities.Liquidacion;
import com.codesoftlabs.tusalud.app.entities.Poliza;
import com.codesoftlabs.tusalud.app.entities.Preautorizacion;
import com.codesoftlabs.tusalud.app.entities.ReglaCobertura;
import com.codesoftlabs.tusalud.app.entities.enums.EstadoPoliza;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public final class SegurosMapper {

    private SegurosMapper() {}

    public static AseguradoraResponse aseguradora(Aseguradora a) {
        return new AseguradoraResponse(
                a.getId(), a.getCodigo(), a.getNombre(), a.getTipo(), a.getRuc(), a.getContacto(), a.isActiva());
    }

    public static ReglaResponse regla(ReglaCobertura r) {
        return new ReglaResponse(
                r.getCategoria(), r.getTipo(), r.getValor(), r.getTopePorServicio(), r.isRequierePreautorizacion());
    }

    public static List<ReglaResponse> reglas(Convenio c) {
        return c.getReglas().stream()
                .sorted(Comparator.comparing(r -> r.getCategoria().ordinal()))
                .map(SegurosMapper::regla)
                .toList();
    }

    public static ConvenioResponse convenio(Convenio c, LocalDate hoy) {
        return new ConvenioResponse(
                c.getId(),
                c.getAseguradora().getId(),
                c.getAseguradora().getCodigo(),
                c.getAseguradora().getNombre(),
                c.getNombre(),
                c.getVigenciaDesde(),
                c.getVigenciaHasta(),
                c.getTopeAnual(),
                c.isActivo(),
                c.vigenteEn(hoy),
                reglas(c),
                c.getExclusiones().stream().sorted().toList());
    }

    private static Long dias(Poliza p, LocalDate hoy) {
        return p.estadoEn(hoy) == EstadoPoliza.ACTIVA ? ChronoUnit.DAYS.between(hoy, p.getVigenciaHasta()) : null;
    }

    public static PolizaResponse poliza(Poliza p, LocalDate hoy) {
        return new PolizaResponse(
                p.getId(),
                p.getPaciente().getId(),
                p.getPaciente().getUsuario().getNombreCompleto(),
                p.getPaciente().getDni(),
                p.getConvenio().getAseguradora().getNombre(),
                p.getConvenio().getId(),
                p.getConvenio().getNombre(),
                p.getNumero(),
                p.getTitularNombre(),
                p.getParentesco(),
                p.getPlan(),
                p.getVigenciaDesde(),
                p.getVigenciaHasta(),
                p.estadoEn(hoy),
                dias(p, hoy),
                p.getMotivoSuspension());
    }

    public static MiPolizaResponse miPoliza(Poliza p, LocalDate hoy) {
        return new MiPolizaResponse(
                p.getId(),
                p.getConvenio().getAseguradora().getNombre(),
                p.getConvenio().getNombre(),
                p.getNumero(),
                p.getTitularNombre(),
                p.getParentesco(),
                p.getPlan(),
                p.getVigenciaDesde(),
                p.getVigenciaHasta(),
                p.estadoEn(hoy),
                dias(p, hoy),
                reglas(p.getConvenio()));
    }

    public static PreautorizacionResponse preautorizacion(Preautorizacion p, LocalDate hoy) {
        return new PreautorizacionResponse(
                p.getId(),
                p.getCodigo(),
                p.getPaciente().getId(),
                p.getPaciente().getUsuario().getNombreCompleto(),
                p.getPoliza().getConvenio().getAseguradora().getNombre(),
                p.getPoliza().getNumero(),
                p.getCategoria(),
                p.getReferenciaId(),
                p.getDescripcion(),
                p.getMontoEstimado(),
                p.estadoEn(hoy),
                p.getSolicitadaPor().getNombreCompleto(),
                p.getSolicitadaEn(),
                p.getGestionadaPor() == null ? null : p.getGestionadaPor().getNombreCompleto(),
                p.getGestionadaEn(),
                p.getCodigoAutorizacion(),
                p.getMontoAprobado(),
                p.getVigenteHasta(),
                p.getMotivoRechazo());
    }

    public static CuentaAseguradoraResponse cuenta(CuentaAseguradora c, LocalDate hoy) {
        return new CuentaAseguradoraResponse(
                c.getId(),
                c.getAseguradora().getId(),
                c.getAseguradora().getNombre(),
                c.getComprobante() == null ? null : c.getComprobante().getCodigo(),
                c.getCategoria(),
                c.getDescripcion(),
                c.getMontoTotal(),
                c.getMontoCubierto(),
                c.getCopago(),
                c.getFecha(),
                c.getEstado(),
                Math.max(0, ChronoUnit.DAYS.between(c.getFecha().toLocalDate(), hoy)),
                c.getLiquidacion() == null ? null : c.getLiquidacion().getCodigo());
    }

    public static LiquidacionResponse liquidacion(Liquidacion l) {
        return new LiquidacionResponse(
                l.getId(),
                l.getCodigo(),
                l.getAseguradora().getId(),
                l.getAseguradora().getNombre(),
                l.getDesde(),
                l.getHasta(),
                l.getTotal(),
                l.getEstado(),
                l.getCantidadCuentas(),
                l.getCreadaPor().getNombreCompleto(),
                l.getCreadaEn(),
                l.getEnviadaEn(),
                l.getPagadaEn(),
                l.getMontoPagado(),
                l.getMontoGlosa(),
                l.getMotivoGlosa());
    }
}
