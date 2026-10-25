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

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new org.springframework.messaging.support.ChannelInterceptor() {
            @Override
            public org.springframework.messaging.Message<?> preSend(
                    org.springframework.messaging.Message<?> message,
                    org.springframework.messaging.MessageChannel channel
            ) {
                org.springframework.messaging.simp.stomp.StompHeaderAccessor accessor =
                        org.springframework.messaging.support.MessageHeaderAccessor.getAccessor(
                                message, org.springframework.messaging.simp.stomp.StompHeaderAccessor.class);

                if (accessor != null && org.springframework.messaging.simp.stomp.StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destination = accessor.getDestination();
                    if (destination != null) {
                        java.security.Principal principal = accessor.getUser();
                        Long principalTenantId = null;
                        if (principal instanceof org.springframework.security.core.Authentication auth
                                && auth.getPrincipal() instanceof com.fleetx.auth.security.FleetxUserPrincipal userPrincipal) {
                            principalTenantId = userPrincipal.getTenantId();
                        }

                        // Validate tenant-partitioned destination: /topic/tenants/{tenantId}/**
                        if (destination.startsWith("/topic/tenants/")) {
                            String[] parts = destination.split("/");
                            if (parts.length >= 4) {
                                try {
                                    Long requestedTenantId = Long.parseLong(parts[3]);
                                    if (principalTenantId == null || !principalTenantId.equals(requestedTenantId)) {
                                        throw new IllegalArgumentException("Cross-tenant WebSocket subscription denied.");
                                    }
                                } catch (NumberFormatException ignored) {}
                            }
                        } else if ("/topic/alerts".equals(destination) || "/topic/fleet-locations".equals(destination)) {
                            // Non-legacy tenants are prohibited from subscribing to global unpartitioned topics
                            if (principalTenantId != null && principalTenantId != 1L) {
                                throw new IllegalArgumentException("Global broadcast subscription restricted. Subscribe to /topic/tenants/{tenantId}/... instead.");
                            }
                        }
                    }
                }
                return message;
            }
        });
    }
}

