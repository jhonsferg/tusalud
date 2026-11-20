package com.codesoftlabs.tusalud.app.services.pediatria;

import com.codesoftlabs.tusalud.app.documentos.FichaDocumento;
import com.codesoftlabs.tusalud.app.documentos.Formato;
import com.codesoftlabs.tusalud.app.documentos.MotorPlantillas;
import com.codesoftlabs.tusalud.app.documentos.QrGenerador;
import com.codesoftlabs.tusalud.app.documentos.TipoDocumento;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.CarneResponse;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.DosisCarne;
import com.codesoftlabs.tusalud.app.dto.response.PediatriaResponses.RegistroVacunaResponse;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoDosis;
import com.codesoftlabs.tusalud.app.entities.enums.PediatriaEnums.EstadoRegistroVacunacion;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CarneVacunacionPdf {

    private final MotorPlantillas motor;

    @Value("${app.documentos-medicos.url-verificacion:http://localhost:4600/verificar/}")
    private String urlVerificacion;

    public byte[] renderizar(
            CarneResponse carne, String codigo, String hash, String generadoPor, LocalDateTime emitidoEn) {
        FichaDocumento ficha = FichaDocumento.de(TipoDocumento.INFORME_MEDICO);
        ficha.titulo("Carné de vacunación", "Emitido " + Formato.fechaHora(emitidoEn));
        if (carne.esquemaPorValidar()) {
            ficha.banner("Esquema de vacunación por validar con la norma técnica vigente");
        }
        ficha.par("Paciente", carne.pacienteNombre())
                .par("DNI", carne.dni())
                .par(
                        "Nacimiento",
                        carne.fechaNacimiento() == null ? "Sin registrar" : Formato.fecha(carne.fechaNacimiento()))
                .par("Edad", carne.edadMeses() == null ? "" : edad(carne.edadMeses()))
                .par("Esquema", carne.esquemaNombre())
                .par("Aplicadas", String.valueOf(carne.aplicadas()))
                .par("Atrasadas", String.valueOf(carne.atrasadas()))
                .par("Próximas", String.valueOf(carne.proximas()));
        Map<UUID, RegistroVacunaResponse> registros = new HashMap<>();
        carne.registros().stream()
                .filter(r -> r.estado() == EstadoRegistroVacunacion.VIGENTE)
                .forEach(r -> registros.put(r.id(), r));
        ficha.seccion("Dosis aplicadas");
        boolean hayAplicadas = false;
        for (DosisCarne d : carne.dosis()) {
            RegistroVacunaResponse r = d.registroId() == null ? null : registros.get(d.registroId());
            if (d.estado() == EstadoDosis.APLICADA && r != null) {
                hayAplicadas = true;
                ficha.fila(
                        d.vacunaNombre() + " · dosis " + d.numeroDosis(),
                        Formato.fecha(r.fechaAplicacion()) + " · lote " + r.lote() + " · " + r.establecimiento() + " · "
                                + r.responsableNombre());
            }
        }
        if (!hayAplicadas) {
            ficha.fila("", "Sin dosis registradas");
        }
        ficha.seccion("Dosis pendientes");
        boolean hayPendientes = false;
        for (DosisCarne d : carne.dosis()) {
            if (d.estado() == EstadoDosis.APLICADA) {
                continue;
            }
            hayPendientes = true;
            String detalle = d.etiqueta() + " · recomendada el " + Formato.fecha(d.fechaRecomendada());
            if (d.estado() == EstadoDosis.ATRASADA) {
                detalle += " · atrasada " + d.atrasoDias() + " días";
            } else if (d.estado() == EstadoDosis.FUERA_DE_EDAD) {
                detalle += " · fuera del rango de edad";
            }
            ficha.fila(d.vacunaNombre() + " · dosis " + d.numeroDosis(), detalle);
        }
        if (!hayPendientes) {
            ficha.fila("", "Esquema completo para la edad");
        }
        ficha.firma(generadoPor, "Emisión del carné");
        ficha.parametros().put("generadoPor", generadoPor);
        ficha.parametros().put("generadoEn", Formato.fechaHora(LocalDateTime.now()));
        ficha.parametros().put("codigo", codigo);
        ficha.parametros().put("urlVerificacion", urlVerificacion + codigo);
        ficha.parametros().put("hashCorto", hash.substring(0, Math.min(16, hash.length())));
        ficha.grafico(2, QrGenerador.generar(urlVerificacion + codigo, 240));
        return motor.renderizarPdf(ficha);
    }

    private static String edad(int meses) {
        if (meses < 24) {
            return meses + (meses == 1 ? " mes" : " meses");
        }
        return meses / 12 + " años";
    }
}
