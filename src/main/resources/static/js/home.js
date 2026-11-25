document.getElementById("create").addEventListener("click", () => {
  const id = crypto.randomUUID().replaceAll("-", "");
  location.href = `/room/${id}`;
});

document.getElementById("join").addEventListener("click", () => {
  const raw = document.getElementById("link").value.trim();
  if (!raw) return;
  try {
    const url = new URL(raw, location.origin);
    const id = url.pathname.startsWith("/room/") ? url.pathname.slice(6) : raw;
    if (!/^[a-z0-9-]{1,32}$/.test(id)) throw new Error("Camera invalida");
    location.href = `/room/${id}`;
  } catch {
    alert("Introdu un link de camera valid sau codul camerei.");
  }
});
