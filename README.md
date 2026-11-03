# chat67

Aplicatie simpla de chat **video + audio** in timp real. Te conectezi prin **link de camera**, nu prin matching random (nu e Omegle/OmeTV).

In dreapta ai chat text. Conversatia se salveaza pe acelasi link, ca sa o vezi si daca revii in camera.

## Cum pornesti

Ai nevoie de Node.js 18+.

```bash
npm install
npm start
```

Deschide [http://localhost:3000](http://localhost:3000)

1. Pune un nume.
2. Apasa **Creeaza camera**.
3. Copiaza linkul si trimite-l persoanei cu care vrei sa vorbesti.
4. Permite camera si microfonul in browser.

Pentru a vorbi de pe doua calculatoare din retea, inlocuieste `localhost` cu IP-ul tau (ex. `http://192.168.0.10:3000`). Camera din browser functioneaza pe HTTP doar pe localhost; in rest e nevoie de HTTPS.

## Foldere

- `server/` — semnalizare Socket.IO + salvare chat
- `public/` — interfata (pagina de start, camera, CSS, JS)
- `docs/` — cerintele proiectului
- `data/chats/` — istoricul mesajelor (nu se urca pe Git)

## Limite de inceput

- Maxim 8 persoane pe o camera
- Fara conturi
- Fara matching random
