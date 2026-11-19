package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoPlantillaResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ConsentimientoResumen;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMedicoResumen;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.DocumentoMinimoResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.EstablecimientoResponse;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.ReferenciaResponse;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.ConsentimientoPlantilla;
import com.codesoftlabs.tusalud.app.entities.DocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.EstablecimientoSalud;
import com.codesoftlabs.tusalud.app.entities.ReferenciaClinica;

public final class DocumentosMedicosMapper {

    private DocumentosMedicosMapper() {}

    public static ReferenciaResponse referencia(ReferenciaClinica r) {
        if (r == null) {
            return null;
        }
        return new ReferenciaResponse(
                r.getId(),
                r.getDestino().getId(),
                r.getDestino().getNombre(),
                r.getEspecialidad(),
                r.getPrioridad(),
                r.getEstado(),
                r.getMotivoRechazo());
    }

    public static DocumentoMedicoResumen resumen(DocumentoMedico d, ReferenciaClinica referencia) {
        return new DocumentoMedicoResumen(
                d.getId(),
                d.getTipo(),
                d.getTipo().etiqueta(),
                d.getEstado(),
                d.numeroCompleto(),
                d.getPaciente().getId(),
                d.getPaciente().getUsuario().getNombreCompleto(),
                d.getMedico().getUsuario().getNombreCompleto(),
                d.getMedico().getColegiatura(),
                d.getCreadoEn(),
                d.getEmitidoEn(),
                d.getAnuladoEn(),
                d.getMotivoAnulacion(),
                d.getIngreso() == null ? null : d.getIngreso().getId(),
                d.getAtencion() == null ? null : d.getAtencion().getId(),
                referencia(referencia));
    }

    public static DocumentoMinimoResponse minimo(DocumentoMedico d) {
        return new DocumentoMinimoResponse(
                d.getTipo(),
                d.getTipo().etiqueta(),
                d.getEstado(),
                d.numeroCompleto(),
                d.getEmitidoEn() == null ? d.getCreadoEn() : d.getEmitidoEn());
    }

    public static EstablecimientoResponse establecimiento(EstablecimientoSalud e) {
        return new EstablecimientoResponse(
                e.getId(),
                e.getCodigoIpress(),
                e.getNombre(),
                e.getNivel(),
                e.getDistrito(),
                e.getContacto(),
                e.isActivo());
    }

    public static ConsentimientoPlantillaResponse plantilla(ConsentimientoPlantilla p) {
        return new ConsentimientoPlantillaResponse(
                p.getId(),
                p.getCodigo(),
                p.getNombre(),
                p.getTexto(),
                p.getRiesgos(),
                p.getBeneficios(),
                p.getAlternativas(),
                p.isRequiereTestigo(),
                p.getVigenciaDias(),
                p.getVersion(),
                p.isActiva());
    }

    public static ConsentimientoResumen consentimiento(Consentimiento c) {
        return new ConsentimientoResumen(
                c.getId(),
                c.getPlantilla().getNombre(),
                c.getProcedimiento(),
                c.estadoEfectivo(),
                c.getPaciente().getId(),
                c.getPaciente().getUsuario().getNombreCompleto(),
                c.getMedico().getUsuario().getNombreCompleto(),
                c.isRequiereTestigo(),
                c.getCreadoEn(),
                c.getFirmadoEn(),
                c.getVenceEn(),
                c.getFirmadoVia(),
                c.getIngreso() == null ? null : c.getIngreso().getId(),
                c.getAtencion() == null ? null : c.getAtencion().getId());
    }
}
