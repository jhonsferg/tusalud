package com.codesoftlabs.tusalud.app.websocket;

import com.codesoftlabs.tusalud.app.config.TelemedicinaProperties;
import com.codesoftlabs.tusalud.app.services.impl.TeleconsultaFinalizadaEvento;
import com.codesoftlabs.tusalud.app.services.interfaces.TeleconsultaService;
import com.codesoftlabs.tusalud.app.services.interfaces.TeleconsultaService.IngresoSala;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Slf4j
public class SenalizacionHandler extends TextWebSocketHandler {

    private static final int MAX_MENSAJE = 32 * 1024;
    private static final int MAX_SDP = 20_000;
    private static final int MAX_ICE = 2_000;
    private static final int MAX_CHAT = 500;
    private static final long VENTANA_MS = 10_000;
    private static final Set<String> CANDIDATOS = Set.of("host", "srflx", "prflx", "relay");
    private static final Set<String> TIPOS =
            Set.of("admitir", "offer", "answer", "ice", "chat", "calidad", "finalizar", "ping", "salir");

    private static final class Sala {
        volatile WebSocketSession medico;
        volatile WebSocketSession paciente;
        volatile boolean admitido;
    }

    private static final class Control {
        volatile long ultimaActividad = System.currentTimeMillis();
        volatile long inicioVentana = System.currentTimeMillis();
        volatile int mensajes;
    }

    private final TeleconsultaService teleconsultaService;
    private final TelemedicinaProperties propiedades;
    private final ObjectMapper mapper;
    private final Map<UUID, Sala> salas = new ConcurrentHashMap<>();
    private final Map<String, Control> controles = new ConcurrentHashMap<>();

    public SenalizacionHandler(
            TeleconsultaService teleconsultaService, TelemedicinaProperties propiedades, ObjectMapper mapper) {
        this.teleconsultaService = teleconsultaService;
        this.propiedades = propiedades;
        this.mapper = mapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setTextMessageSizeLimit(MAX_MENSAJE);
        session.setBinaryMessageSizeLimit(1024);
        UUID teleconsultaId = (UUID) session.getAttributes().get(TicketHandshakeInterceptor.ATRIBUTO_TELECONSULTA);
        UUID usuarioId = (UUID) session.getAttributes().get(TicketHandshakeInterceptor.ATRIBUTO_USUARIO);
        IngresoSala ingreso;
        try {
            ingreso = teleconsultaService.alConectar(teleconsultaId, usuarioId);
        } catch (RuntimeException e) {
            enviar(
                    session,
                    Map.of("t", "error", "mensaje", e.getMessage() == null ? "Acceso denegado" : e.getMessage()));
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.getAttributes().put("rol", ingreso.rol());
        controles.put(session.getId(), new Control());
        Sala sala = salas.computeIfAbsent(teleconsultaId, id -> new Sala());
        synchronized (sala) {
            sala.admitido = ingreso.enCurso();
            WebSocketSession anterior = "MEDICO".equals(ingreso.rol()) ? sala.medico : sala.paciente;
            if ("MEDICO".equals(ingreso.rol())) {
                sala.medico = session;
            } else {
                sala.paciente = session;
            }
            if (anterior != null && anterior.isOpen() && !anterior.getId().equals(session.getId())) {
                enviar(anterior, Map.of("t", "reemplazada"));
                anterior.close(CloseStatus.NORMAL);
            }
        }
        difundirEstado(sala);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage mensaje) throws Exception {
        Control control = controles.get(session.getId());
        if (control == null) {
            return;
        }
        long ahora = System.currentTimeMillis();
        control.ultimaActividad = ahora;
        if (ahora - control.inicioVentana > VENTANA_MS) {
            control.inicioVentana = ahora;
            control.mensajes = 0;
        }
        if (++control.mensajes > propiedades.maxMensajesPorVentana() || mensaje.getPayloadLength() > MAX_MENSAJE) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        JsonNode nodo;
        try {
            nodo = mapper.readTree(mensaje.getPayload());
        } catch (RuntimeException e) {
            enviar(session, Map.of("t", "error", "mensaje", "Mensaje inválido"));
            return;
        }
        String tipo = nodo.path("t").asString("");
        if (!TIPOS.contains(tipo)) {
            enviar(session, Map.of("t", "error", "mensaje", "Tipo de mensaje no permitido"));
            return;
        }
        UUID teleconsultaId = (UUID) session.getAttributes().get(TicketHandshakeInterceptor.ATRIBUTO_TELECONSULTA);
        String rol = (String) session.getAttributes().get("rol");
        Sala sala = salas.get(teleconsultaId);
        if (sala == null) {
            return;
        }
        switch (tipo) {
            case "ping" -> enviar(session, Map.of("t", "pong"));
            case "salir" -> session.close(CloseStatus.NORMAL);
            case "admitir" -> admitir(sala, teleconsultaId, rol, session);
            case "finalizar" -> finalizar(sala, teleconsultaId, rol, session);
            case "calidad" -> calidad(teleconsultaId, nodo);
            case "chat" -> retransmitirChat(sala, rol, nodo, session);
            default -> retransmitirSenal(sala, rol, tipo, nodo, session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        controles.remove(session.getId());
        UUID teleconsultaId = (UUID) session.getAttributes().get(TicketHandshakeInterceptor.ATRIBUTO_TELECONSULTA);
        if (teleconsultaId == null) {
            return;
        }
        Sala sala = salas.get(teleconsultaId);
        if (sala == null) {
            return;
        }
        synchronized (sala) {
            if (sala.medico != null && sala.medico.getId().equals(session.getId())) {
                sala.medico = null;
            }
            if (sala.paciente != null && sala.paciente.getId().equals(session.getId())) {
                sala.paciente = null;
            }
            if (sala.medico == null && sala.paciente == null) {
                salas.remove(teleconsultaId);
                return;
            }
        }
        difundirEstado(sala);
    }

    @EventListener
    public void alFinalizar(TeleconsultaFinalizadaEvento evento) {
        Sala sala = salas.remove(evento.teleconsultaId());
        if (sala != null) {
            cerrarSala(sala);
        }
    }

    @Scheduled(fixedRate = 15_000)
    public void cerrarInactivas() {
        long limite = System.currentTimeMillis() - propiedades.inactividadSeg() * 1000L;
        for (Sala sala : salas.values()) {
            for (WebSocketSession s : new WebSocketSession[] {sala.medico, sala.paciente}) {
                Control c = s == null ? null : controles.get(s.getId());
                if (c != null && c.ultimaActividad < limite) {
                    try {
                        s.close(CloseStatus.GOING_AWAY);
                    } catch (IOException e) {
                        log.debug("No se pudo cerrar la sesión inactiva");
                    }
                }
            }
        }
    }

    private void admitir(Sala sala, UUID id, String rol, WebSocketSession origen) {
        if (!"MEDICO".equals(rol)) {
            enviar(origen, Map.of("t", "error", "mensaje", "Solo el médico admite al paciente"));
            return;
        }
        if (sala.paciente == null) {
            enviar(origen, Map.of("t", "error", "mensaje", "El paciente aún no está en la sala"));
            return;
        }
        teleconsultaService.alAdmitir(id);
        sala.admitido = true;
        difundirEstado(sala);
    }

    private void finalizar(Sala sala, UUID id, String rol, WebSocketSession origen) {
        if (!"MEDICO".equals(rol)) {
            enviar(origen, Map.of("t", "error", "mensaje", "Solo el médico puede finalizar la teleconsulta"));
            return;
        }
        teleconsultaService.alFinalizarDesdeSala(id);
        salas.remove(id);
        cerrarSala(sala);
    }

    private void calidad(UUID id, JsonNode nodo) {
        int reconexiones = Math.max(0, Math.min(nodo.path("reconexiones").asInt(0), 50));
        String candidato = nodo.path("candidato").asString("");
        teleconsultaService.alCalidad(id, reconexiones, CANDIDATOS.contains(candidato) ? candidato : null);
    }

    private void retransmitirChat(Sala sala, String rol, JsonNode nodo, WebSocketSession origen) {
        if (!sala.admitido) {
            return;
        }
        String texto = nodo.path("texto").asString("").strip();
        if (texto.isEmpty() || texto.length() > MAX_CHAT) {
            enviar(origen, Map.of("t", "error", "mensaje", "El mensaje debe tener entre 1 y 500 caracteres"));
            return;
        }
        enviar(contraparte(sala, rol), Map.of("t", "chat", "de", rol, "texto", texto));
    }

    private void retransmitirSenal(Sala sala, String rol, String tipo, JsonNode nodo, WebSocketSession origen) {
        if (!sala.admitido || sala.medico == null || sala.paciente == null) {
            enviar(origen, Map.of("t", "error", "mensaje", "La sala aún no está lista"));
            return;
        }
        if ("ice".equals(tipo)) {
            JsonNode candidato = nodo.path("candidate");
            if (candidato.isMissingNode() || candidato.toString().length() > MAX_ICE) {
                enviar(origen, Map.of("t", "error", "mensaje", "Candidato ICE inválido"));
                return;
            }
            enviar(contraparte(sala, rol), Map.of("t", "ice", "de", rol, "candidate", candidato));
            return;
        }
        String sdp = nodo.path("sdp").asString("");
        if (sdp.isEmpty() || sdp.length() > MAX_SDP) {
            enviar(origen, Map.of("t", "error", "mensaje", "Descripción de sesión inválida"));
            return;
        }
        enviar(contraparte(sala, rol), Map.of("t", tipo, "de", rol, "sdp", sdp));
    }

    private WebSocketSession contraparte(Sala sala, String rol) {
        return "MEDICO".equals(rol) ? sala.paciente : sala.medico;
    }

    private void difundirEstado(Sala sala) {
        for (String rol : new String[] {"MEDICO", "PACIENTE"}) {
            WebSocketSession destino = "MEDICO".equals(rol) ? sala.medico : sala.paciente;
            enviar(
                    destino,
                    Map.of(
                            "t",
                            "estado",
                            "rol",
                            rol,
                            "medicoEnSala",
                            sala.medico != null,
                            "pacienteEnSala",
                            sala.paciente != null,
                            "admitido",
                            sala.admitido));
        }
    }

    private void cerrarSala(Sala sala) {
        for (WebSocketSession s : new WebSocketSession[] {sala.medico, sala.paciente}) {
            if (s != null && s.isOpen()) {
                enviar(s, Map.of("t", "finalizada"));
                try {
                    s.close(CloseStatus.NORMAL);
                } catch (IOException e) {
                    log.debug("No se pudo cerrar la sesión de la sala");
                }
            }
        }
    }

    private void enviar(WebSocketSession destino, Map<String, ?> contenido) {
        if (destino == null || !destino.isOpen()) {
            return;
        }
        try {
            String json = mapper.writeValueAsString(contenido);
            synchronized (destino) {
                destino.sendMessage(new TextMessage(json));
            }
        } catch (IOException | RuntimeException e) {
            log.debug("No se pudo enviar un mensaje de señalización");
        }
    }
}
