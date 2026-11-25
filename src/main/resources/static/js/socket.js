// Aceleasi evenimente ale chatului, transmise prin WebSocket nativ.
function createChatSocket() {
  const handlers = new Map();
  const protocol = location.protocol === "https:" ? "wss:" : "ws:";
  const ws = new WebSocket(`${protocol}//${location.host}/ws`);
  const dispatch = (event, data) => handlers.get(event)?.(data);
  ws.addEventListener("open", () => dispatch("connect"));
  ws.addEventListener("close", () => dispatch("disconnect"));
  ws.addEventListener("message", ({ data }) => {
    const message = JSON.parse(data);
    dispatch(message.event, message.data);
  });
  window.addEventListener("pagehide", () => ws.close());
  return {
    on(event, handler) { handlers.set(event, handler); },
    emit(event, data) {
      if (ws.readyState !== WebSocket.OPEN) return false;
      ws.send(JSON.stringify({ event, data }));
      return true;
    }
  };
}
