package com.codesoftlabs.tusalud.app.websocket;

import com.codesoftlabs.tusalud.app.services.impl.TicketTeleconsultaService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class TicketHandshakeInterceptor implements HandshakeInterceptor {

    static final String ATRIBUTO_TELECONSULTA = "teleconsultaId";
    static final String ATRIBUTO_USUARIO = "usuarioId";

    private final TicketTeleconsultaService ticketService;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler handler,
            Map<String, Object> atributos) {
        String ticket = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams()
                .getFirst("ticket");
        var datos = ticketService.consumir(ticket);
        if (datos.isEmpty()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        atributos.put(ATRIBUTO_TELECONSULTA, datos.get().teleconsultaId());
        atributos.put(ATRIBUTO_USUARIO, datos.get().usuarioId());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {}
}
