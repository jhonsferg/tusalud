package com.codesoftlabs.tusalud.app.services.documentosmedicos;

import com.codesoftlabs.tusalud.app.documentos.CodigoDocumentoClinico;
import com.codesoftlabs.tusalud.app.dto.response.DocumentosMedicosResponses.VerificacionPublicaResponse;
import com.codesoftlabs.tusalud.app.entities.CarneVacunacion;
import com.codesoftlabs.tusalud.app.entities.Consentimiento;
import com.codesoftlabs.tusalud.app.entities.DocumentoMedico;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoConsentimiento;
import com.codesoftlabs.tusalud.app.entities.enums.DocumentosMedicosEnums.EstadoDocumentoMedico;
import com.codesoftlabs.tusalud.app.exceptions.DemasiadosIntentosException;
import com.codesoftlabs.tusalud.app.repositories.CarneVacunacionRepository;
import com.codesoftlabs.tusalud.app.repositories.ConsentimientoRepository;
import com.codesoftlabs.tusalud.app.repositories.DocumentoMedicoRepository;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerificacionPublicaService {

    private static final String ESTABLECIMIENTO = "TuSalud";
    private static final long VENTANA_MS = 60_000L;
    private static final int MAXIMO_CLAVES = 10_000;

    private final DocumentoMedicoRepository documentos;
    private final ConsentimientoRepository consentimientos;
    private final CarneVacunacionRepository carnes;
    private final com.codesoftlabs.tusalud.app.repositories.RecetaElectronicaRepository recetas;
    private final com.codesoftlabs.tusalud.app.services.receta.RecetaFirma recetaFirma;
    private final com.codesoftlabs.tusalud.app.repositories.HceImpresionRepository historias;

    @Value("${app.documentos-medicos.verificacion-por-minuto:20}")
    private int porMinuto;

    private final Map<String, Deque<Long>> intentos = new ConcurrentHashMap<>();

    public VerificacionPublicaResponse verificar(String ip, String codigo) {
        limitar(ip == null ? "desconocida" : ip);
        String normalizado = CodigoDocumentoClinico.normalizar(codigo);
        Optional<DocumentoMedico> documento = documentos.findByCodigo(normalizado);
        Optional<Consentimiento> consentimiento =
                documento.isPresent() ? Optional.empty() : consentimientos.findByCodigo(normalizado);
        if (documento.isPresent()) {
            DocumentoMedico d = documento.get();
            String resultado = d.getEstado() == EstadoDocumentoMedico.ANULADO ? "ANULADO" : "VALIDO";
            return new VerificacionPublicaResponse(
                    resultado,
                    d.getTipo().etiqueta(),
                    d.getEmitidoEn(),
                    d.getMedico().getUsuario().getNombreCompleto(),
                    d.getMedico().getColegiatura(),
                    ESTABLECIMIENTO);
        }
        if (consentimiento.isPresent()) {
            Consentimiento c = consentimiento.get();
            EstadoConsentimiento estado = c.estadoEfectivo();
            String resultado = switch (estado) {
                case FIRMADO -> "VALIDO";
                case VENCIDO -> "VENCIDO";
                default -> "ANULADO";
            };
            return new VerificacionPublicaResponse(
                    resultado,
                    "Consentimiento informado",
                    c.getFirmadoEn(),
                    c.getMedico().getUsuario().getNombreCompleto(),
                    c.getMedico().getColegiatura(),
                    ESTABLECIMIENTO);
        }
        Optional<CarneVacunacion> carne = carnes.findByCodigo(normalizado);
        if (carne.isPresent()) {
            CarneVacunacion c = carne.get();
            return new VerificacionPublicaResponse(
                    "VALIDO",
                    "Carné de vacunación",
                    c.getEmitidoEn(),
                    c.getEmitidoPor().getNombreCompleto(),
                    null,
                    ESTABLECIMIENTO);
        }
        Optional<com.codesoftlabs.tusalud.app.entities.RecetaElectronica> receta = recetas.findByCodigo(normalizado);
        if (receta.isPresent()) {
            var r = receta.get();
            boolean intacta =
                    CodigoDocumentoClinico.sha256(r.getContenidoCanonico()).equals(r.getHash())
                            && recetaFirma.verificar(r.getContenidoCanonico(), r.getFirma(), r.getCertificadoDer());
            String resultado;
            if (!intacta) {
                resultado = "ALTERADO";
            } else if (r.getEstado()
                    == com.codesoftlabs.tusalud.app.entities.enums.RecetaEnums.EstadoRecetaElectronica.ANULADA) {
                resultado = "ANULADO";
            } else if (java.time.LocalDateTime.now().isAfter(r.getVigenteHasta())) {
                resultado = "VENCIDO";
            } else {
                resultado = "VALIDO";
            }
            return new VerificacionPublicaResponse(
                    resultado,
                    "Receta electrónica",
                    r.getEmitidaEn(),
                    r.getMedico().getUsuario().getNombreCompleto(),
                    r.getMedico().getColegiatura(),
                    ESTABLECIMIENTO);
        }
        Optional<com.codesoftlabs.tusalud.app.entities.HceImpresion> historia =
                historias.findByCodigoVerificacion(normalizado);
        if (historia.isPresent()) {
            var h = historia.get();
            return new VerificacionPublicaResponse(
                    "VALIDO",
                    "Historia clínica impresa",
                    h.getEmitidoEn(),
                    h.getEmitidoPor().getNombreCompleto(),
                    null,
                    ESTABLECIMIENTO);
        }
        return new VerificacionPublicaResponse("NO_ENCONTRADO", null, null, null, null, null);
    }

    private void limitar(String clave) {
        long ahora = Instant.now().toEpochMilli();
        if (intentos.size() > MAXIMO_CLAVES) {
            intentos.clear();
        }
        Deque<Long> marcas = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (marcas) {
            while (!marcas.isEmpty() && ahora - marcas.peekFirst() > VENTANA_MS) {
                marcas.pollFirst();
            }
            if (marcas.size() >= porMinuto) {
                throw new DemasiadosIntentosException("Demasiadas verificaciones, intente de nuevo en un minuto");
            }
            marcas.addLast(ahora);
        }
    }
}
