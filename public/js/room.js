const roomId = location.pathname.split("/").pop();
const name = localStorage.getItem("chat67-name") || "Oaspete";
const statusEl = document.getElementById("status");
const videosEl = document.getElementById("videos");
const messagesEl = document.getElementById("messages");
const iceServers = [{ urls: "stun:stun.l.google.com:19302" }];

const peers = new Map();
let localStream;
let socket;

function addVideo(id, stream, label) {
  let tile = document.getElementById(`tile-${id}`);
  if (!tile) {
    tile = document.createElement("div");
    tile.className = "tile";
    tile.id = `tile-${id}`;
    tile.innerHTML = `<video autoplay playsinline ${id === "local" ? "muted" : ""}></video><span></span>`;
    videosEl.appendChild(tile);
  }
  tile.querySelector("span").textContent = label;
  const video = tile.querySelector("video");
  video.srcObject = stream;
}

function removeVideo(id) {
  document.getElementById(`tile-${id}`)?.remove();
}

function renderMessage(msg) {
  const p = document.createElement("p");
  p.className = "msg";
  const time = new Date(msg.at).toLocaleTimeString("ro-RO", { hour: "2-digit", minute: "2-digit" });
  p.innerHTML = `<b></b><time></time><br><span></span>`;
  p.querySelector("b").textContent = msg.name;
  p.querySelector("time").textContent = time;
  p.querySelector("span").textContent = msg.text;
  messagesEl.appendChild(p);
  messagesEl.scrollTop = messagesEl.scrollHeight;
}

async function makePeer(peerId, isOfferer) {
  const pc = new RTCPeerConnection({ iceServers });
  localStream.getTracks().forEach((track) => pc.addTrack(track, localStream));

  pc.onicecandidate = (event) => {
    if (event.candidate) {
      socket.emit("signal", { to: peerId, data: { candidate: event.candidate } });
    }
  };

  pc.ontrack = (event) => {
    const stream = event.streams[0] || new MediaStream([event.track]);
    const label = peers.get(peerId)?.name || "Participant";
    addVideo(peerId, stream, label);
  };

  peers.set(peerId, { pc, name: "Participant" });

  if (isOfferer) {
    const offer = await pc.createOffer();
    await pc.setLocalDescription(offer);
    socket.emit("signal", { to: peerId, data: { sdp: pc.localDescription } });
  }

  return pc;
}

async function handleSignal(from, nameFrom, data) {
  let entry = peers.get(from);
  if (!entry) {
    const pc = await makePeer(from, false);
    entry = { pc, name: nameFrom };
    peers.set(from, entry);
  }
  entry.name = nameFrom || entry.name;

  if (data.sdp) {
    await entry.pc.setRemoteDescription(new RTCSessionDescription(data.sdp));
    if (data.sdp.type === "offer") {
      const answer = await entry.pc.createAnswer();
      await entry.pc.setLocalDescription(answer);
      socket.emit("signal", { to: from, data: { sdp: entry.pc.localDescription } });
    }
  } else if (data.candidate) {
    try {
      await entry.pc.addIceCandidate(new RTCIceCandidate(data.candidate));
    } catch {
      /* candidate poate veni inainte de descriere */
    }
  }
}

document.getElementById("copy").addEventListener("click", async () => {
  await navigator.clipboard.writeText(location.href);
  statusEl.textContent = "Link copiat. Trimite-l persoanei cu care vrei sa vorbesti.";
});

document.getElementById("mic").addEventListener("click", (event) => {
  const track = localStream.getAudioTracks()[0];
  if (!track) return;
  track.enabled = !track.enabled;
  event.target.textContent = track.enabled ? "Microfon pornit" : "Microfon oprit";
});

document.getElementById("cam").addEventListener("click", (event) => {
  const track = localStream.getVideoTracks()[0];
  if (!track) return;
  track.enabled = !track.enabled;
  event.target.textContent = track.enabled ? "Camera pornita" : "Camera oprita";
});

document.getElementById("chat-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const input = document.getElementById("chat-input");
  const text = input.value.trim();
  if (!text) return;
  socket.emit("chat", text);
  input.value = "";
});

async function start() {
  try {
    localStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
  } catch {
    statusEl.textContent = "Permite camera si microfonul, apoi reincarca pagina.";
    return;
  }

  addVideo("local", localStream, `${name} (tu)`);
  socket = io();

  socket.on("join-error", (message) => {
    statusEl.textContent = message;
  });

  socket.on("joined", async ({ peers: existing, messages }) => {
    statusEl.textContent = `Camera ${roomId} · ${existing.length + 1} conectat(i)`;
    messagesEl.innerHTML = "";
    messages.forEach(renderMessage);
    for (const peer of existing) {
      const pc = await makePeer(peer.id, true);
      peers.set(peer.id, { pc, name: peer.name });
    }
  });

  socket.on("peer-joined", ({ id, name: peerName }) => {
    statusEl.textContent = `${peerName} a intrat in camera.`;
  });

  socket.on("signal", ({ from, name: peerName, data }) => {
    handleSignal(from, peerName, data);
  });

  socket.on("chat", renderMessage);

  socket.on("peer-left", ({ id }) => {
    peers.get(id)?.pc.close();
    peers.delete(id);
    removeVideo(id);
    statusEl.textContent = "Un participant a iesit.";
  });

  socket.emit("join-room", { roomId, name });
}

start();
