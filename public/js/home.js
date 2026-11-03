const nameInput = document.getElementById("name");
const saved = localStorage.getItem("chat67-name");
if (saved) nameInput.value = saved;

function saveName() {
  const name = nameInput.value.trim() || "Oaspete";
  localStorage.setItem("chat67-name", name);
  return name;
}

document.getElementById("create").addEventListener("click", () => {
  saveName();
  const id = Math.random().toString(36).slice(2, 10);
  location.href = `/room/${id}`;
});

document.getElementById("join").addEventListener("click", () => {
  saveName();
  const raw = document.getElementById("link").value.trim();
  if (!raw) return;
  try {
    const url = new URL(raw, location.origin);
    location.href = url.pathname.startsWith("/room/") ? url.pathname : `/room/${raw}`;
  } catch {
    location.href = `/room/${raw}`;
  }
});
