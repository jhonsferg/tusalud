package com.codesoftlabs.tusalud.app.services.canales;

import com.codesoftlabs.tusalud.app.canales.FirmasCanales;
import com.codesoftlabs.tusalud.app.config.CanalesProperties;
import com.codesoftlabs.tusalud.app.dto.request.CanalesRequests.EstadoEntregaWebhookRequest;
import com.codesoftlabs.tusalud.app.entities.ContactoCanal;
import com.codesoftlabs.tusalud.app.entities.EntregaNotificacion;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.Canal;
import com.codesoftlabs.tusalud.app.entities.enums.CanalesEnums.EstadoEntrega;
import com.codesoftlabs.tusalud.app.exceptions.DatosInvalidosException;
import com.codesoftlabs.tusalud.app.exceptions.ForbiddenOperationException;
import com.codesoftlabs.tusalud.app.repositories.ContactoCanalRepository;
import com.codesoftlabs.tusalud.app.repositories.EntregaNotificacionRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookCanalesService {

    private static final long TOLERANCIA_SEG = 300;
    private static final String PREFIJO = "sha256=";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final EntregaNotificacionRepository entregas;
    private final ContactoCanalRepository contactos;
    private final CanalesProperties propiedades;

    @Transactional
    public boolean procesar(Canal canal, String marcaTiempo, String firma, byte[] cuerpo) {
        validarFirma(marcaTiempo, firma, cuerpo);
        EstadoEntregaWebhookRequest evento;
        try {
            evento = MAPPER.readValue(new String(cuerpo, StandardCharsets.UTF_8), EstadoEntregaWebhookRequest.class);
        } catch (RuntimeException ex) {
            throw new DatosInvalidosException("El cuerpo del evento no es válido");
        }
        if (evento.idExterno() == null || evento.estado() == null) {
            throw new DatosInvalidosException("El evento necesita idExterno y estado");
        }
        EntregaNotificacion entrega = entregas.findByIdExterno(evento.idExterno())
                .filter(e -> e.getCanal() == canal)
                .orElse(null);
        if (entrega == null) {
            return false;
        }
        LocalDateTime ahora = LocalDateTime.now();
        switch (evento.estado()) {
            case ENTREGADO -> {
                if (entrega.getEstado() == EstadoEntrega.ENVIADO) {
                    entrega.setEstado(EstadoEntrega.ENTREGADO);
                    entrega.setEntregadoEn(ahora);
                    entrega.setMotivo(null);
                }
            }
            case FALLIDO -> {
                if (entrega.getEstado() == EstadoEntrega.ENVIADO) {
                    entrega.setEstado(EstadoEntrega.FALLIDO);
                    entrega.setMotivo(recortar(evento.motivo(), "El proveedor reportó un fallo"));
                }
            }
            case REBOTADO -> {
                if (entrega.getEstado() == EstadoEntrega.ENVIADO || entrega.getEstado() == EstadoEntrega.ENTREGADO) {
                    entrega.setEstado(EstadoEntrega.REBOTADO);
                    entrega.setMotivo(recortar(evento.motivo(), "El destino rebotó el mensaje"));
                    contactos
                            .findByUsuarioIdAndCanal(entrega.getUsuarioId(), canal)
                            .ifPresent(c -> marcarRebotado(c, ahora));
                }
            }
            default -> throw new DatosInvalidosException("El estado no es válido para un evento del proveedor");
        }
        entregas.save(entrega);
        return true;
    }

    private void marcarRebotado(ContactoCanal contacto, LocalDateTime ahora) {
        contacto.setRebotadoEn(ahora);
        contacto.setActualizadoEn(ahora);
    }

    private void validarFirma(String marcaTiempo, String firma, byte[] cuerpo) {
        String secreto = propiedades.webhookSecreto();
        if (secreto == null || secreto.isBlank()) {
            throw new ForbiddenOperationException("Los eventos de entrega no están habilitados");
        }
        if (marcaTiempo == null || firma == null || !firma.startsWith(PREFIJO)) {
            throw new ForbiddenOperationException("Firma de evento inválida");
        }
        long segundos;
        try {
            segundos = Long.parseLong(marcaTiempo);
        } catch (NumberFormatException ex) {
            throw new ForbiddenOperationException("Firma de evento inválida");
        }
        if (Math.abs(Instant.now().getEpochSecond() - segundos) > TOLERANCIA_SEG) {
            throw new ForbiddenOperationException("El evento está fuera de la ventana permitida");
        }
        byte[] firmado = (marcaTiempo + ".").getBytes(StandardCharsets.UTF_8);
        byte[] datos = new byte[firmado.length + cuerpo.length];
        System.arraycopy(firmado, 0, datos, 0, firmado.length);
        System.arraycopy(cuerpo, 0, datos, firmado.length, cuerpo.length);
        String esperado = FirmasCanales.hmacHex(secreto, datos);
        if (!FirmasCanales.iguales(esperado, firma.substring(PREFIJO.length()))) {
            throw new ForbiddenOperationException("Firma de evento inválida");
        }
    }

    private static String recortar(String motivo, String porDefecto) {
        if (motivo == null || motivo.isBlank()) {
            return porDefecto;
        }
        return motivo.length() > 200 ? motivo.substring(0, 200) : motivo;
    }
}
