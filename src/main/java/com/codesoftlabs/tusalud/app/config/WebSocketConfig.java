package com.codesoftlabs.tusalud.app.config;

import com.codesoftlabs.tusalud.app.websocket.SenalizacionHandler;
import com.codesoftlabs.tusalud.app.websocket.TicketHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final SenalizacionHandler senalizacionHandler;
    private final TicketHandshakeInterceptor ticketInterceptor;
    private final CorsProperties cors;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(senalizacionHandler, "/ws/teleconsulta")
                .addInterceptors(ticketInterceptor)
                .setAllowedOrigins(cors.allowedOrigins().toArray(String[]::new));
    }
}
