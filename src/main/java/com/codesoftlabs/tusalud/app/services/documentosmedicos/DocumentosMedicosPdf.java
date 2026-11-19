package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.documentos.FichaDocumento;
import com.codesoftlabs.tusalud.app.documentos.Formato;
import com.codesoftlabs.tusalud.app.documentos.MotorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.QrGenerador;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.DocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.TipoDocumentoMedico;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentosMedicosPdf {

    private final MotorPlantillas motor;

    @Value("${app.documentos-medicos.url-verificacion:http://localhost:4600/verificar/}")
    private String urlVerificacion;

    public byte[] renderizarDocumento(
            DocumentoMedico documento, Map<String, Object> contenido, String banner, String generadoPor) {
        TipoDocumentoMedico tipo = documento.getTipo();
        FichaDocumento ficha = FichaDocumento.de(tipoPlantilla(tipo));
        String numero = documento.numeroCompleto();
        ficha.titulo(tipo.etiqueta(), numero == null ? "Borrador" : numero);
        if (banner != null) {
            ficha.banner(banner);
        }
        Paciente paciente = documento.getPaciente();
        ficha.par("Paciente", paciente.getUsuario().getNombreCompleto())
                .par("DNI", paciente.getDni())
                .par("Historia clínica", paciente.getHistorialClinicoNumero())
                .par("Edad", edad(paciente.getFechaNacimiento()))
                .par(
                        "Emisión",
                        documento.getEmitidoEn() == null ? "Sin emitir" : Formato.fechaHora(documento.getEmitidoEn()))
                .par("Médico", documento.getMedico().getUsuario().getNombreCompleto())
                .par("CMP", documento.getMedico().getColegiatura());
        cuerpo(ficha, tipo, contenido);
        diagnosticos(ficha, contenido.get("diagnosticos"));
        ficha.firma(
                documento.getMedico().getUsuario().getNombreCompleto(),
                "CMP " + documento.getMedico().getColegiatura());
        ficha.parametros().put("firma3", numero == null ? "" : "Documento " + numero);
        return sellar(ficha, documento.getCodigoVerificacion(), documento.getContenidoHash(), generadoPor);
    }

    public byte[] renderizarConsentimiento(Consentimiento c, byte[] firma, String banner, String generadoPor) {
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.CONSENTIMIENTO);
        ficha.titulo("Consentimiento informado", c.getProcedimiento());
        if (banner != null) {
            ficha.banner(banner);
        }
        Paciente paciente = c.getPaciente();
        ficha.par("Paciente", paciente.getUsuario().getNombreCompleto())
                .par("DNI", paciente.getDni())
                .par("Historia clínica", paciente.getHistorialClinicoNumero())
                .par("Médico", c.getMedico().getUsuario().getNombreCompleto())
                .par("CMP", c.getMedico().getColegiatura())
                .par("Procedimiento", c.getProcedimiento())
                .par("Versión del texto", String.valueOf(c.getPlantillaVersion()))
                .par("Estado", c.estadoEfectivo().name());
        for (String bloque : c.getTextoAceptado().split("\n\n")) {
            int corte = bloque.indexOf('\n');
            if (corte > 0) {
                ficha.seccion(bloque.substring(0, corte).strip());
                ficha.fila("", bloque.substring(corte + 1).strip());
            } else {
                ficha.fila("", bloque.strip());
            }
        }
        if (c.getFirmadoEn() != null) {
            ficha.seccion("Firma");
            String firmante =
                    c.getFirmanteTipo() == null ? "" : c.getFirmanteTipo().name();
            ficha.fila("Firmante", c.getFirmanteNombre() + " · DNI " + c.getFirmanteDni() + " · " + firmante);
            if (c.getFirmanteParentesco() != null) {
                ficha.fila("Parentesco", c.getFirmanteParentesco());
            }
            if (c.getTestigoNombre() != null) {
                ficha.fila("Testigo", c.getTestigoNombre() + " · DNI " + c.getTestigoDni());
            }
            ficha.fila("Firmado el", Formato.fechaHora(c.getFirmadoEn()));
            if (c.getVenceEn() != null) {
                ficha.fila("Vigente hasta", Formato.fechaHora(c.getVenceEn()));
            }
            ficha.firma(c.getFirmanteNombre(), "DNI " + c.getFirmanteDni());
            if (firma != null) {
                ficha.grafico(1, leer(firma));
            }
        }
        if (c.getMotivoRevocacion() != null) {
            ficha.seccion("Revocación");
            ficha.fila("Motivo", c.getMotivoRevocacion());
        }
        return sellar(ficha, c.getCodigoVerificacion(), c.getFirmaHash(), generadoPor);
    }

    private byte[] sellar(FichaDocumento ficha, String codigo, String hash, String generadoPor) {
        ficha.parametros().put("generadoPor", generadoPor);
        ficha.parametros().put("generadoEn", Formato.fechaHora(LocalDateTime.now()));
        ficha.parametros().put("codigo", codigo == null ? "Sin emitir" : codigo);
        ficha.parametros().put("urlVerificacion", codigo == null ? "" : urlVerificacion + codigo);
        ficha.parametros().put("hashCorto", hash == null ? "" : hash.substring(0, Math.min(16, hash.length())));
        if (codigo != null) {
            ficha.grafico(2, QrGenerador.generar(urlVerificacion + codigo, 240));
        }
        return motor.renderizarPdf(ficha);
    }

    private void cuerpo(FichaDocumento ficha, TipoDocumentoMedico tipo, Map<String, Object> c) {
        switch (tipo) {
            case CERTIFICADO -> {
                ficha.seccion("Certificación");
                fila(ficha, "Motivo", c.get("motivo"));
                fila(ficha, "Hallazgos", c.get("hallazgos"));
                fila(ficha, "Recomendaciones", c.get("recomendaciones"));
                fila(ficha, "Vigencia", c.get("vigenciaDias") + " días desde la emisión");
            }
            case DESCANSO -> {
                LocalDate desde = LocalDate.parse(String.valueOf(c.get("desde")));
                LocalDate hasta = LocalDate.parse(String.valueOf(c.get("hasta")));
                long dias = java.time.temporal.ChronoUnit.DAYS.between(desde, hasta) + 1;
                ficha.seccion("Descanso médico");
                fila(ficha, "Tipo", c.get("tipoDescanso"));
                fila(ficha, "Contingencia", c.get("contingencia"));
                fila(ficha, "Desde", Formato.fecha(desde));
                fila(ficha, "Hasta", Formato.fecha(hasta));
                fila(ficha, "Total", dias + (dias == 1 ? " día" : " días"));
                fila(ficha, "Indicación", c.get("indicacion"));
            }
            case INFORME -> {
                ficha.seccion("Informe clínico");
                fila(ficha, "Dirigido a", c.get("destinatario"));
                fila(ficha, "Resumen", c.get("resumen"));
                fila(ficha, "Tratamiento", c.get("tratamiento"));
                fila(ficha, "Evolución", c.get("evolucion"));
            }
            case REFERENCIA -> {
                ficha.seccion("Referencia");
                fila(ficha, "Destino", c.get("destinoNombre"));
                fila(ficha, "Especialidad", c.get("especialidad"));
                fila(ficha, "Prioridad", c.get("prioridad"));
                fila(ficha, "Motivo", c.get("motivo"));
                fila(ficha, "Resumen clínico", c.get("resumenClinico"));
            }
            case CONTRARREFERENCIA -> {
                ficha.seccion("Contrarreferencia");
                fila(ficha, "Referencia", c.get("referenciaNumero"));
                fila(ficha, "Resumen", c.get("resumen"));
                fila(ficha, "Recomendaciones", c.get("recomendaciones"));
            }
            case EPICRISIS -> {
                ficha.seccion("Epicrisis");
                fila(ficha, "Motivo de ingreso", c.get("motivo"));
                fila(ficha, "Dx de ingreso", c.get("diagnosticoIngreso"));
                fila(ficha, "Dx de alta", c.get("diagnosticoAlta"));
                fila(ficha, "Evolución", c.get("resumenEvolucion"));
                fila(ficha, "Procedimientos", c.get("procedimientos"));
                fila(ficha, "Condición de alta", c.get("condicionAlta"));
                fila(ficha, "Indicaciones", c.get("indicaciones"));
            }
            default -> {}
        }
    }

    private static void fila(FichaDocumento ficha, String etiqueta, Object valor) {
        if (valor != null && !String.valueOf(valor).isBlank()) {
            ficha.fila(etiqueta, String.valueOf(valor));
        }
    }

    @SuppressWarnings("unchecked")
    private static void diagnosticos(FichaDocumento ficha, Object crudo) {
        if (!(crudo instanceof List<?> lista) || lista.isEmpty()) {
            return;
        }
        ficha.seccion("Diagnósticos CIE-10");
        for (Object item : lista) {
            Map<String, Object> d = (Map<String, Object>) item;
            String marca = Boolean.TRUE.equals(d.get("principal")) ? " · principal" : "";
            ficha.fila(String.valueOf(d.get("codigo")), d.get("descripcion") + marca);
        }
    }

    private static TipoDocumento tipoPlantilla(TipoDocumentoMedico tipo) {
        return switch (tipo) {
            case CERTIFICADO -> TipoDocumento.CERTIFICADO_MEDICO;
            case DESCANSO -> TipoDocumento.DESCANSO_MEDICO;
            case INFORME -> TipoDocumento.INFORME_MEDICO;
            case REFERENCIA -> TipoDocumento.REFERENCIA_MEDICA;
            case CONTRARREFERENCIA -> TipoDocumento.CONTRARREFERENCIA_MEDICA;
            case EPICRISIS -> TipoDocumento.EPICRISIS;
        };
    }

    private static String edad(LocalDate nacimiento) {
        return nacimiento == null
                ? ""
                : Period.between(nacimiento, LocalDate.now()).getYears() + " años";
    }

    private static Image leer(byte[] png) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(png));
            return imagen;
        } catch (IOException ex) {
            return null;
        }
    }
}
