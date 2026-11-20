package com.codesoftlabs.tusalud.app.services.planilla;

import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.AsistenciaDiaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.BoletaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.BoletaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ConceptoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ContratoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.EmpleadoConceptoResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.LineaBoletaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MarcacionResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.MiBoletaResumenResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.ParametroResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.PlanillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.PlanillaResponses.TerminalResponse;
import com.codesoftlabs.tusalud.app.entities.AsistenciaDia;
import com.codesoftlabs.tusalud.app.entities.BoletaPago;
import com.codesoftlabs.tusalud.app.entities.ConceptoPlanilla;
import com.codesoftlabs.tusalud.app.entities.ContratoLaboral;
import com.codesoftlabs.tusalud.app.entities.EmpleadoConcepto;
import com.codesoftlabs.tusalud.app.entities.Marcacion;
import com.codesoftlabs.tusalud.app.entities.ParametroPlanilla;
import com.codesoftlabs.tusalud.app.entities.Planilla;
import com.codesoftlabs.tusalud.app.entities.TerminalAsistencia;
import com.codesoftlabs.tusalud.app.entities.Usuario;
import java.time.LocalDate;

public final class PlanillaMapper {

    private PlanillaMapper() {}

    public static String enmascarar(String cuenta) {
        if (cuenta == null || cuenta.isBlank()) {
            return null;
        }
        String limpia = cuenta.replace("-", "");
        if (limpia.length() <= 4) {
            return "****";
        }
        return "****" + limpia.substring(limpia.length() - 4);
    }

    private static String nombre(Usuario usuario) {
        return usuario == null ? null : usuario.getNombreCompleto();
    }

    public static ContratoResponse contrato(ContratoLaboral c) {
        return new ContratoResponse(
                c.getId(),
                c.getEmpleado().getId(),
                c.getEmpleado().getUsuario().getNombreCompleto(),
                c.getEmpleado().getArea(),
                c.getCargo(),
                c.getRegimen(),
                c.getRemuneracion(),
                c.isAsignacionFamiliar(),
                c.getSistemaPensionario(),
                c.getAfpNombre(),
                c.getHorasSemanales(),
                c.getBanco(),
                enmascarar(c.getCuenta()),
                c.getFechaInicio(),
                c.getFechaFin(),
                c.getEstado(),
                c.getEmpleado().getCodigoMarcacion());
    }

    public static ParametroResponse parametro(ParametroPlanilla p, LocalDate hoy) {
        boolean vigente = !p.getVigenteDesde().isAfter(hoy)
                && (p.getVigenteHasta() == null || !p.getVigenteHasta().isBefore(hoy));
        return new ParametroResponse(
                p.getId(),
                p.getCodigo(),
                p.getValor(),
                p.getVigenteDesde(),
                p.getVigenteHasta(),
                p.getDescripcion(),
                p.isPorValidar(),
                vigente);
    }

    public static ConceptoResponse concepto(ConceptoPlanilla c) {
        return new ConceptoResponse(
                c.getId(),
                c.getCodigo(),
                c.getNombre(),
                c.getTipo(),
                c.getModo(),
                c.getValorDefecto(),
                c.isAfectoRenta(),
                c.isAfectoPension(),
                c.isAfectoEssalud(),
                c.isActivo(),
                c.isSistema());
    }

    public static EmpleadoConceptoResponse asignacion(EmpleadoConcepto a) {
        ConceptoPlanilla c = a.getConcepto();
        return new EmpleadoConceptoResponse(
                a.getId(),
                c.getId(),
                c.getCodigo(),
                c.getNombre(),
                c.getTipo(),
                c.getModo(),
                a.getValor(),
                a.getVigenteDesde(),
                a.getVigenteHasta(),
                a.isActivo());
    }

    public static MarcacionResponse marcacion(Marcacion m) {
        return new MarcacionResponse(
                m.getId(),
                m.getEmpleado().getId(),
                m.getEmpleado().getUsuario().getNombreCompleto(),
                m.getTipo(),
                m.getFechaHora(),
                m.getOrigen(),
                m.getObservacion());
    }

    public static TerminalResponse terminal(TerminalAsistencia t) {
        return new TerminalResponse(t.getId(), t.getNombre(), t.isActivo(), t.getUltimoUso());
    }

    public static AsistenciaDiaResponse asistencia(AsistenciaDia a) {
        return new AsistenciaDiaResponse(
                a.getId(),
                a.getEmpleado().getId(),
                a.getEmpleado().getUsuario().getNombreCompleto(),
                a.getFecha(),
                a.getProgramadaInicio(),
                a.getProgramadaFin(),
                a.getEntrada(),
                a.getSalida(),
                a.getMinutosTardanza(),
                a.getMinutosTrabajados(),
                a.getMinutosExtra(),
                a.getEstado(),
                a.getExtraDestino(),
                a.isFaltaJustificada(),
                a.getJustificacion(),
                a.getAjustadoPor() != null);
    }

    public static PlanillaResponse planilla(Planilla p) {
        return new PlanillaResponse(
                p.getId(),
                p.getCodigo(),
                p.getAnio(),
                p.getMes(),
                p.getEstado(),
                p.getCantidadBoletas(),
                p.getTotalIngresos(),
                p.getTotalDescuentos(),
                p.getTotalNeto(),
                p.getTotalAporteEmpleador(),
                p.getTotalRenta(),
                p.getTotalPension(),
                p.getHash(),
                nombre(p.getCalculadaPor()),
                p.getCalculadaEn(),
                nombre(p.getAprobadaPor()),
                p.getAprobadaEn(),
                nombre(p.getPagadaPor()),
                p.getPagadaEn(),
                p.getFechaPago(),
                p.getReferenciaPago(),
                nombre(p.getCerradaPor()),
                p.getCerradaEn());
    }

    public static BoletaResumenResponse boletaResumen(BoletaPago b) {
        return new BoletaResumenResponse(
                b.getId(),
                b.getNumero(),
                b.getEmpleado().getId(),
                b.getEmpleadoNombre(),
                b.getCargo(),
                b.getTotalIngresos(),
                b.getTotalDescuentos(),
                b.getNeto(),
                b.getBanco());
    }

    public static MiBoletaResumenResponse miBoleta(BoletaPago b) {
        Planilla p = b.getPlanilla();
        return new MiBoletaResumenResponse(
                b.getId(),
                b.getNumero(),
                p.getCodigo(),
                p.getAnio(),
                p.getMes(),
                b.getTotalIngresos(),
                b.getTotalDescuentos(),
                b.getNeto(),
                p.getEstado());
    }

    public static BoletaResponse boleta(BoletaPago b, boolean conCuenta) {
        Planilla p = b.getPlanilla();
        return new BoletaResponse(
                b.getId(),
                b.getNumero(),
                p.getId(),
                p.getCodigo(),
                p.getAnio(),
                p.getMes(),
                p.getEstado(),
                b.getEmpleado().getId(),
                b.getEmpleadoNombre(),
                b.getCargo(),
                b.getRegimen(),
                b.getSistemaPensionario(),
                b.getBanco(),
                conCuenta ? enmascarar(b.getCuenta()) : null,
                b.getDiasLaborados(),
                b.getDiasFalta(),
                b.getDiasNoRemunerados(),
                b.getMinutosTardanza(),
                b.getMinutosExtraPagados(),
                b.getRemuneracionBasica(),
                b.getTotalIngresos(),
                b.getTotalDescuentos(),
                b.getRentaQuinta(),
                b.getPension(),
                b.getAporteEmpleador(),
                b.getNeto(),
                b.getProvisionGratificacion(),
                b.getProvisionCts(),
                b.getProvisionVacaciones(),
                b.getHash(),
                b.getLineas().stream()
                        .map(l -> new LineaBoletaResponse(
                                l.getCodigo(), l.getNombre(), l.getTipo(), l.getBase(), l.getMonto()))
                        .toList());
    }
}
