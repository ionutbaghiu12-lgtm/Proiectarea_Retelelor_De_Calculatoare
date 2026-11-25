package ro.chat67.chat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ChatStore {
    private final Path directory;
    private final ObjectMapper mapper;

    public record Mesaj(String id, String name, String text, String at) { }

    public ChatStore(ObjectMapper mapper, @Value("${app.chats-dir}") String directory) throws IOException {
        this.mapper = mapper;
        this.directory = Path.of(directory);
        Files.createDirectories(this.directory);
    }

    public static boolean cameraValida(String id) {
        return id != null && id.matches("[a-z0-9-]{1,32}");
    }

    private Path fisier(String id) {
        if (!cameraValida(id)) throw new IllegalArgumentException("Camera invalida");
        return directory.resolve(id + ".json");
    }

    public synchronized List<Mesaj> istoric(String id) throws IOException {
        Path file = fisier(id);
        if (!Files.exists(file)) return new ArrayList<>();
        return mapper.readValue(file.toFile(), new TypeReference<ArrayList<Mesaj>>() { });
    }

    public synchronized Mesaj adauga(String id, String nume, String text) throws IOException {
        var messages = istoric(id);
        var message = new Mesaj(UUID.randomUUID().toString(), nume, text, Instant.now().toString());
        messages.add(message);
        var last = messages.subList(Math.max(0, messages.size() - 500), messages.size());
        Path temporary = Files.createTempFile(directory, "chat-", ".tmp");
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), last);
            Files.move(temporary, fisier(id), StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
        return message;
    }
}
