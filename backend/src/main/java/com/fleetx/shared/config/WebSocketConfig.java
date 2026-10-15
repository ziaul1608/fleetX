package com.fleetx.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("#{'${fleetx.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}'.split(',')}")
    private String[] allowedOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory STOMP broker destination prefix for broadcasting
        registry.enableSimpleBroker("/topic");
        // Application destination prefix for incoming messages from clients
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket connection endpoint with SockJS fallback
        registry.addEndpoint("/ws/fleetx")
                .setAllowedOriginPatterns(allowedOrigins)
                .withSockJS();
    }
}

