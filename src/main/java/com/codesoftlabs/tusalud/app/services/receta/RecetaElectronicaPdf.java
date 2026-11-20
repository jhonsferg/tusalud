package com.codesoftlabs.tusalud.app.services.receta;

import com.codesoftlabs.tusalud.app.documentos.FichaDocumento;
import com.codesoftlabs.tusalud.app.documentos.Formato;
import com.codesoftlabs.tusalud.app.documentos.MotorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.QrGenerador;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.entities.Paciente;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronica;
import com.codesoftlabs.tusalud.app.entities.RecetaElectronicaItem;
import com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecetaElectronicaPdf {

    private final MotorPlantillas motor;

    @Value("${app.documentos-medicos.url-verificacion:http://localhost:4600/verificar/}")
    private String urlVerificacion;

    public byte[] renderizar(RecetaElectronica receta, boolean firmaValida, String generadoPor) {
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.INFORME_MEDICO);
        ficha.titulo("Receta electrónica", receta.getNumero());
        if (receta.getEstado() == EstadoRecetaElectronica.ANULADA) {
            ficha.banner("ANULADA: " + receta.getMotivoAnulacion());
        } else if (receta.isCertificadoDePrueba()) {
            ficha.banner("Firma electrónica de prueba, sin validez legal como firma digital acreditada");
        } else if (receta.isControlEspecial()) {
            ficha.banner("Medicamento de control especial");
        }
        Paciente paciente = receta.getPaciente();
        ficha.par("Paciente", paciente.getUsuario().getNombreCompleto())
                .par("DNI", paciente.getDni())
                .par("Historia clínica", paciente.getHistorialClinicoNumero())
                .par("Edad", edad(paciente.getFechaNacimiento()))
                .par("Emisión", Formato.fechaHora(receta.getEmitidaEn()))
                .par("Vigente hasta", Formato.fechaHora(receta.getVigenteHasta()))
                .par("Médico", receta.getMedico().getUsuario().getNombreCompleto())
                .par("CMP", receta.getMedico().getColegiatura());
        if (receta.getDiagnosticoCodigo() != null) {
            ficha.seccion("Diagnóstico");
            ficha.fila(receta.getDiagnosticoCodigo(), receta.getDiagnosticoDescripcion());
        }
        ficha.seccion("Prescripción");
        for (RecetaElectronicaItem item : receta.getItems()) {
            StringBuilder detalle = new StringBuilder();
            anexar(detalle, item.getDosis());
            anexar(detalle, item.getFrecuencia());
            anexar(detalle, item.getDuracion());
            anexar(detalle, item.getVia() == null ? null : "vía " + item.getVia());
            anexar(detalle, "cantidad " + item.getCantidad());
            if (item.isControlEspecial()) {
                anexar(detalle, "control especial");
            }
            ficha.fila((item.getPosicion() + 1) + ". " + item.getMedicamentoTexto(), detalle.toString());
        }
        if (receta.getIndicaciones() != null && !receta.getIndicaciones().isBlank()) {
            ficha.seccion("Indicaciones al paciente");
            ficha.fila("", receta.getIndicaciones());
        }
        ficha.seccion("Firma electrónica");
        ficha.fila("Algoritmo", receta.getFirmaAlgoritmo());
        ficha.fila("Certificado", receta.getCertificadoSujeto());
        ficha.fila("Huella", receta.getCertificadoHuella().substring(0, 32));
        ficha.fila("Estado de la firma", firmaValida ? "Válida" : "No verificable");
        ficha.firma(
                receta.getMedico().getUsuario().getNombreCompleto(),
                "CMP " + receta.getMedico().getColegiatura());
        ficha.parametros().put("generadoPor", generadoPor);
        ficha.parametros().put("generadoEn", Formato.fechaHora(LocalDateTime.now()));
        ficha.parametros().put("codigo", receta.getCodigoVerificacion());
        ficha.parametros().put("urlVerificacion", urlVerificacion + receta.getCodigoVerificacion());
        ficha.parametros().put("hashCorto", receta.getHash().substring(0, 16));
        ficha.grafico(2, QrGenerador.generar(urlVerificacion + receta.getCodigoVerificacion(), 240));
        return motor.renderizarPdf(ficha);
    }

    private static void anexar(StringBuilder texto, String parte) {
        if (parte == null || parte.isBlank()) {
            return;
        }
        if (texto.length() > 0) {
            texto.append(" · ");
        }
        texto.append(parte.strip());
    }

    private static String edad(LocalDate nacimiento) {
        if (nacimiento == null) {
            return "Sin registrar";
        }
        Period p = Period.between(nacimiento, LocalDate.now());
        return p.getYears() >= 2 ? p.getYears() + " años" : (p.getYears() * 12 + p.getMonths()) + " meses";
    }
}
