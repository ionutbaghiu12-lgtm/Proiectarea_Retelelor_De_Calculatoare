package ro.chat67.chat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ro.chat67.security.UtilizatorService;

@Component
public class ChatSocketHandler extends TextWebSocketHandler {
    private final Map<String, WebSocketSession> connections = new HashMap<>();
    private final ObjectMapper mapper;
    private final ChatStore store;
    private final UtilizatorService utilizatori;

    public ChatSocketHandler(ObjectMapper mapper, ChatStore store, UtilizatorService utilizatori) {
        this.mapper = mapper;
        this.store = store;
        this.utilizatori = utilizatori;
    }

    @Override
    public synchronized void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (!autentificat(session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        session.setTextMessageSizeLimit(16 * 1024);
        session.getAttributes().put("name", utilizatori.cont(session.getPrincipal().getName()).nume());
        connections.put(session.getId(), session);
    }

    private boolean autentificat(WebSocketSession session) {
        try {
            var http = (HttpSession) session.getAttributes().get("httpSession");
            if (http == null || session.getPrincipal() == null) return false;
            var context = http.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            return context instanceof SecurityContext security && security.getAuthentication() != null
                    && security.getAuthentication().isAuthenticated()
                    && security.getAuthentication().getName().equals(session.getPrincipal().getName());
        } catch (IllegalStateException exception) {
            return false; // Sesiunea a expirat sau utilizatorul a iesit din cont.
        }
    }

    @Override
    protected synchronized void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!autentificat(session)) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        JsonNode envelope;
        try {
            envelope = mapper.readTree(message.getPayload());
        } catch (IOException exception) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }
        if (envelope == null || !envelope.isObject()) return;
        JsonNode data = envelope.path("data");
        String room = camera(session);
        switch (envelope.path("event").asText()) {
            case "join-room" -> {
                if (room != null) return; // O conexiune apartine unei singure camere.
                String id = data.path("roomId").asText();
                if (!ChatStore.cameraValida(id)) {
                    trimite(session, "join-error", "Linkul camerei nu este valid.");
                    return;
                }
                List<WebSocketSession> others = participanti(id);
                if (others.size() >= 8) {
                    trimite(session, "join-error", "Camera este plina.");
                    return;
                }
                var history = store.istoric(id);
                session.getAttributes().put("roomId", id);
                trimite(session, "joined", Map.of("selfId", session.getId(), "messages", history,
                        "peers", others.stream().map(this::participant).toList()));
                for (var other : others) trimite(other, "peer-joined", participant(session));
            }
            case "signal" -> {
                var target = connections.get(data.path("to").asText());
                // Nu permitem trimiterea semnalizarii catre o alta camera.
                if (room != null && target != null && room.equals(camera(target)) && autentificat(target)) {
                    trimite(target, "signal", Map.of("from", session.getId(),
                            "name", session.getAttributes().get("name"), "data", data.path("data")));
                }
            }
            case "chat" -> {
                String text = data.asText("").trim();
                if (room == null || text.isEmpty()) return;
                text = text.substring(0, Math.min(text.length(), 500));
                var saved = store.adauga(room, (String) session.getAttributes().get("name"), text);
                for (var other : participanti(room)) trimite(other, "chat", saved);
            }
            default -> { }
        }
    }

    private String camera(WebSocketSession session) {
        return (String) session.getAttributes().get("roomId");
    }

    private Map<String, String> participant(WebSocketSession session) {
        return Map.of("id", session.getId(), "name", (String) session.getAttributes().get("name"));
    }

    private List<WebSocketSession> participanti(String room) {
        return connections.values().stream()
                .filter(s -> s.isOpen() && room.equals(camera(s)) && autentificat(s)).toList();
    }

    private void trimite(WebSocketSession session, String event, Object data) throws IOException {
        if (!session.isOpen()) return;
        var message = new TextMessage(mapper.writeValueAsString(Map.of("event", event, "data", data)));
        try {
            session.sendMessage(message);
        } catch (IOException | IllegalStateException exception) {
            // Un tab inchis brusc nu trebuie sa intrerupa mesajele celorlalti.
            connections.remove(session.getId());
            inchide(session);
        }
    }

    @Override
    public synchronized void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        connections.remove(session.getId());
        if (camera(session) != null) {
            for (var other : participanti(camera(session))) trimite(other, "peer-left", Map.of("id", session.getId()));
        }
    }

    public synchronized void inchideSesiunea(String httpSessionId) {
        for (var session : List.copyOf(connections.values())) {
            var http = (HttpSession) session.getAttributes().get("httpSession");
            if (http != null && httpSessionId.equals(http.getId())) inchide(session);
        }
    }

    @Scheduled(fixedDelay = 30000)
    public synchronized void inchideSesiunileExpirate() {
        for (var session : List.copyOf(connections.values())) {
            if (!autentificat(session)) inchide(session);
        }
    }

    private void inchide(WebSocketSession session) {
        try { session.close(CloseStatus.POLICY_VIOLATION); }
        catch (IOException exception) { connections.remove(session.getId()); }
    }
}
