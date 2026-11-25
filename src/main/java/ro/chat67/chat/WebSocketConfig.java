package ro.chat67.chat;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.*;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatSocketHandler handler;

    public WebSocketConfig(ChatSocketHandler handler) { this.handler = handler; }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws").addInterceptors(new HandshakeInterceptor() {
            @Override
            public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
                if (!(request instanceof ServletServerHttpRequest servlet)) return false;
                HttpSession session = servlet.getServletRequest().getSession(false);
                if (session == null || request.getPrincipal() == null) return false;
                attributes.put("httpSession", session);
                return true;
            }

            @Override
            public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                    WebSocketHandler wsHandler, Exception exception) { }
        });
        // Fara allowedOrigins("*"): Spring accepta implicit doar aceeasi origine.
    }
}
