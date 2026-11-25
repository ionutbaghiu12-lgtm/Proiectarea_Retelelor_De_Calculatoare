package ro.chat67.chat;

import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ChatApiController {
    private final ChatStore store;

    public ChatApiController(ChatStore store) { this.store = store; }

    @GetMapping("/api/chat/{id}")
    public List<ChatStore.Mesaj> istoric(@PathVariable String id) throws IOException {
        if (!ChatStore.cameraValida(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        return store.istoric(id);
    }
}
