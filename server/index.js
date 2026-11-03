const fs = require("fs");
const path = require("path");
const http = require("http");
const express = require("express");
const { Server } = require("socket.io");

const PORT = Number(process.env.PORT) || 3000;
const MAX_PEERS = 8;
const ROOT = path.join(__dirname, "..");
const PUBLIC_DIR = path.join(ROOT, "public");
const CHATS_DIR = path.join(ROOT, "data", "chats");

fs.mkdirSync(CHATS_DIR, { recursive: true });

const app = express();
const server = http.createServer(app);
const io = new Server(server);

app.use(express.json());
app.use(express.static(PUBLIC_DIR));

function safeRoomId(id) {
  return String(id || "").toLowerCase().replace(/[^a-z0-9-]/g, "").slice(0, 32);
}

function chatFile(roomId) {
  return path.join(CHATS_DIR, `${roomId}.json`);
}

function loadChat(roomId) {
  const file = chatFile(roomId);
  if (!fs.existsSync(file)) return [];
  try {
    const data = JSON.parse(fs.readFileSync(file, "utf8"));
    return Array.isArray(data) ? data : [];
  } catch {
    return [];
  }
}

function saveChat(roomId, messages) {
  fs.writeFileSync(chatFile(roomId), JSON.stringify(messages.slice(-500), null, 2));
}

app.get("/room/:id", (_req, res) => {
  res.sendFile(path.join(PUBLIC_DIR, "room.html"));
});

app.get("/api/chat/:id", (req, res) => {
  const roomId = safeRoomId(req.params.id);
  if (!roomId) return res.status(400).json({ error: "camera invalida" });
  res.json(loadChat(roomId));
});

io.on("connection", (socket) => {
  socket.on("join-room", ({ roomId, name }) => {
    const id = safeRoomId(roomId);
    if (!id) {
      socket.emit("join-error", "Linkul camerei nu este valid.");
      return;
    }

    const room = io.sockets.adapter.rooms.get(id);
    const count = room ? room.size : 0;
    if (count >= MAX_PEERS) {
      socket.emit("join-error", "Camera este plina.");
      return;
    }

    socket.data.name = String(name || "Oaspete").trim().slice(0, 24) || "Oaspete";
    socket.join(id);
    socket.data.roomId = id;

    const others = [];
    for (const otherId of io.sockets.adapter.rooms.get(id) || []) {
      if (otherId === socket.id) continue;
      const other = io.sockets.sockets.get(otherId);
      others.push({ id: otherId, name: other?.data?.name || "Oaspete" });
    }

    socket.emit("joined", {
      selfId: socket.id,
      peers: others,
      messages: loadChat(id)
    });
    socket.to(id).emit("peer-joined", { id: socket.id, name: socket.data.name });
  });

  socket.on("signal", ({ to, data }) => {
    io.to(to).emit("signal", { from: socket.id, name: socket.data.name, data });
  });

  socket.on("chat", (text) => {
    const roomId = socket.data.roomId;
    const messageText = String(text || "").trim().slice(0, 500);
    if (!roomId || !messageText) return;

    const message = {
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      name: socket.data.name || "Oaspete",
      text: messageText,
      at: new Date().toISOString()
    };
    const history = loadChat(roomId);
    history.push(message);
    saveChat(roomId, history);
    io.to(roomId).emit("chat", message);
  });

  socket.on("disconnect", () => {
    const roomId = socket.data.roomId;
    if (roomId) {
      socket.to(roomId).emit("peer-left", { id: socket.id });
    }
  });
});

server.listen(PORT, () => {
  console.log(`chat67 pornit pe http://localhost:${PORT}`);
});
