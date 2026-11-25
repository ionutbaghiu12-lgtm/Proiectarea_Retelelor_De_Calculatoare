package ro.chat67;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ro.chat67.security.UtilizatorService;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:sockets;DB_CLOSE_DELAY=-1",
    "app.chats-dir=target/socket-chats", "app.admin.email=", "app.admin.password="
})
class ChatWebSocketIntegrationTest {
    @LocalServerPort int port;
    @Autowired UtilizatorService users;
    @Autowired ObjectMapper mapper;

    URI http(String path) { return URI.create("http://localhost:" + port + path); }
    URI ws() { return URI.create("ws://localhost:" + port + "/ws"); }

    String token(HttpClient client, String path) throws Exception {
        String html = client.send(HttpRequest.newBuilder(http(path)).GET().build(), HttpResponse.BodyHandlers.ofString()).body();
        var match = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertThat(match.find()).as("Token CSRF in formular").isTrue();
        return match.group(1);
    }

    HttpClient login(String name) throws Exception {
        String email = UUID.randomUUID() + "@chat67.test";
        users.inregistreaza(name, email, "test-parola-123");
        var client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String body = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&parola=test-parola-123&_csrf=" + token(client, "/login");
        var response = client.send(HttpRequest.newBuilder(http("/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location").orElse("")).doesNotContain("error");
        return client;
    }

    class Listener implements WebSocket.Listener {
        final BlockingQueue<JsonNode> messages = new LinkedBlockingQueue<>();
        final CompletableFuture<Integer> closed = new CompletableFuture<>();
        final StringBuilder buffer = new StringBuilder();
        public CompletionStage<?> onText(WebSocket socket, CharSequence data, boolean last) {
            buffer.append(data);
            if (last) {
                try { messages.add(mapper.readTree(buffer.toString())); }
                catch (Exception exception) { throw new RuntimeException(exception); }
                buffer.setLength(0);
            }
            socket.request(1);
            return null;
        }
        public CompletionStage<?> onClose(WebSocket socket, int code, String reason) {
            closed.complete(code);
            return null;
        }
        JsonNode event(String event) throws Exception {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (System.nanoTime() < deadline) {
                JsonNode message = messages.poll(100, TimeUnit.MILLISECONDS);
                if (message != null && event.equals(message.path("event").asText())) return message.path("data");
            }
            throw new AssertionError("Nu am primit evenimentul " + event);
        }
    }

    WebSocket connect(HttpClient client, Listener listener) throws Exception {
        return client.newWebSocketBuilder().header("Origin", http("").toString()).buildAsync(ws(), listener).get(5, TimeUnit.SECONDS);
    }

    void send(WebSocket socket, String event, Object data) throws Exception {
        socket.sendText(mapper.writeValueAsString(Map.of("event", event, "data", data)), true).get(5, TimeUnit.SECONDS);
    }

    @Test
    void anonimSiOrigineStrainaSuntRespinsi() throws Exception {
        assertThatThrownBy(() -> HttpClient.newHttpClient().newWebSocketBuilder()
                .buildAsync(ws(), new Listener()).get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(WebSocketHandshakeException.class);
        var client = login("Ana");
        assertThatThrownBy(() -> client.newWebSocketBuilder().header("Origin", "https://strain.example")
                .buildAsync(ws(), new Listener()).get(5, TimeUnit.SECONDS))
                .hasCauseInstanceOf(WebSocketHandshakeException.class);
    }

    @Test
    void chatIstoricSemnalizareSiIzolareaCamerelor() throws Exception {
        String room = UUID.randomUUID().toString().replace("-", "");
        var ana = login("Ana");
        var bob = login("Bob");
        var outsider = login("Alta camera");
        var a = new Listener(); var b = new Listener(); var c = new Listener();
        var sa = connect(ana, a); var sb = connect(bob, b); var sc = connect(outsider, c);
        try {
            send(sa, "join-room", Map.of("roomId", room, "name", "Nume fals"));
            String aId = a.event("joined").path("selfId").asText();
            send(sb, "join-room", Map.of("roomId", room));
            var joined = b.event("joined");
            String bId = joined.path("selfId").asText();
            assertThat(joined.path("peers").get(0).path("name").asText()).isEqualTo("Ana");
            send(sc, "join-room", Map.of("roomId", "separat"));
            String cId = c.event("joined").path("selfId").asText();
            send(sa, "signal", Map.of("to", bId, "data", Map.of("sdp", "test")));
            assertThat(b.event("signal").path("from").asText()).isEqualTo(aId);
            send(sa, "signal", Map.of("to", cId, "data", Map.of("sdp", "interzis")));
            assertThat(c.messages.poll(400, TimeUnit.MILLISECONDS)).isNull();
            send(sa, "chat", "Salut!");
            assertThat(b.event("chat").path("name").asText()).isEqualTo("Ana");
            var response = bob.send(HttpRequest.newBuilder(http("/api/chat/" + room)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(mapper.readTree(response.body()).get(0).path("text").asText()).isEqualTo("Salut!");
        } finally { sa.abort(); sb.abort(); sc.abort(); }
    }

    @Test
    void logoutInchideConexiuneaExistenta() throws Exception {
        var client = login("Ana");
        var listener = new Listener();
        var socket = connect(client, listener);
        try {
            var response = client.send(HttpRequest.newBuilder(http("/logout"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString("_csrf=" + token(client, "/"))).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(listener.closed.get(5, TimeUnit.SECONDS)).isEqualTo(1008);
            assertThat(client.send(HttpRequest.newBuilder(http("/api/chat/test")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
        } finally { socket.abort(); }
    }

    @Test
    void maximumOptParticipanti() throws Exception {
        var client = login("Ana");
        String room = UUID.randomUUID().toString().replace("-", "");
        List<WebSocket> sockets = new ArrayList<>();
        try {
            for (int i = 0; i < 9; i++) {
                var listener = new Listener();
                var socket = connect(client, listener);
                sockets.add(socket);
                send(socket, "join-room", Map.of("roomId", room));
                if (i < 8) assertThat(listener.event("joined").path("peers").size()).isEqualTo(i);
                else assertThat(listener.event("join-error").asText()).contains("plina");
            }
        } finally { sockets.forEach(WebSocket::abort); }
    }
}
