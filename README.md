
# chat67

**chat67** este o aplicație web de comunicare în timp real, prin apeluri video/audio și mesaje text. Participanții se conectează într-o cameră folosind un link partajat, fără cont și fără asociere aleatorie cu alte persoane.

## Funcționalități

- **Camere prin link:** creezi o cameră și distribui adresa de forma `/room/<cod>`.
- **Apeluri video și audio:** comunicare între browsere prin WebRTC.
- **Chat text în timp real:** mesajele sunt transmise prin Socket.IO.
- **Istoric persistent:** ultimele 500 de mesaje ale fiecărei camere sunt păstrate pe server în fișiere JSON și reîncărcate la revenirea pe același link.
- **Control media:** poți opri sau porni microfonul și camera.
- **Maximum 8 participanți** într-o cameră.
- **Nume de afișare:** numele ales este păstrat local în browser.

## Tehnologii utilizate

| Componentă | Tehnologie | Rol |
| --- | --- | --- |
| Server | Node.js 18+, Express 4 | Server HTTP și servirea interfeței |
| Comunicare în timp real | Socket.IO 4 | Mesaje, prezență și semnalizare WebRTC |
| Apeluri audio/video | WebRTC | Conexiuni directe între participanți (mesh) |
| Interfață | HTML, CSS, JavaScript | Pagina principală și camera de chat |
| Persistență | Fișiere JSON | Stocarea mesajelor pe cameră |

## Cerințe

- **Node.js 18** sau o versiune mai nouă, împreună cu npm.
- Un browser modern cu suport WebRTC.
- Permisiuni pentru cameră și microfon, dacă dorești apeluri audio/video.

## Instalare și rulare

1. Descarcă sau clonează proiectul și deschide folderul în terminal.
2. Instalează dependențele:

   ```bash
   npm install
   ```

3. Pornește serverul:

   ```bash
   npm start
   ```

4. Deschide în browser **http://localhost:3000**.

Pe Windows poți folosi și `scripts/start.bat`, care instalează dependențele și pornește aplicația. În VS Code este disponibilă și configurația de lansare din `.vscode/launch.json`.

Serverul folosește implicit portul **3000**; îl poți schimba prin variabila de mediu `PORT`.

## Cum se utilizează

1. Introdu numele pe pagina principală.
2. Apasă **Creează camera**.
3. Permite accesul la cameră și microfon.
4. Apasă **Copiază linkul** și trimite-l celorlalți participanți.
5. Ceilalți utilizatori deschid linkul pentru a intra în aceeași cameră.
6. Folosește butoanele pentru cameră și microfon și panoul din dreapta pentru mesaje.
7. Pentru a părăsi camera, apasă **Ieși**.

Poți intra și într-o cameră existentă introducând linkul în câmpul de pe pagina principală.

## Structura proiectului

```text
chat67/
├── data/
│   └── chats/            # Istoricul mesajelor, în fișiere JSON
├── docs/
│   └── cerinte.md        # Cerințele aplicației
├── public/
│   ├── css/
│   │   └── style.css     # Stilurile interfeței
│   ├── js/
│   │   ├── home.js       # Crearea și accesarea camerelor
│   │   └── room.js       # Apeluri WebRTC și chat
│   ├── index.html        # Pagina principală
│   └── room.html         # Interfața camerei
├── scripts/
│   └── start.bat         # Pornire rapidă pe Windows
├── server/
│   └── index.js          # Server Express și Socket.IO
├── package.json
└── README.md
```

## Cum funcționează

Serverul Express furnizează paginile web și gestionează camerele. Socket.IO transmite evenimentele de conectare, deconectare, mesajele text și datele necesare negocierii conexiunilor WebRTC. Fluxurile audio/video sunt transmise prin conexiuni WebRTC între participanți, într-o arhitectură **mesh**.

Istoricul mesajelor este salvat separat pentru fiecare cameră în `data/chats/<cod>.json`. La intrarea în cameră, serverul trimite istoricul disponibil. Există și un endpoint pentru consultarea istoricului: `GET /api/chat/:id`.

## Limitări și observații

- Aplicația nu include autentificare, moderare, înregistrarea apelurilor sau asociere aleatorie a utilizatorilor.
- Accesul se bazează pe cunoașterea linkului; acesta **nu reprezintă un mecanism complet de securizare a camerei**.
- În implementarea actuală, istoricul text este păstrat pe server în fișiere JSON, fără criptare suplimentară la stocare.
- WebRTC utilizează un server STUN public configurat în cod, dar **nu este configurat un server TURN**. Prin urmare, unele conexiuni între rețele restrictive pot eșua.
- Accesul la cameră și microfon funcționează pe `localhost`; pentru acces din alte dispozitive sau prin internet, browserul solicită în mod normal **HTTPS** (context securizat).
- Aplicația este un proiect de bază și nu include mecanisme complete pentru utilizarea în producție.

## Posibile îmbunătățiri

- Protejarea camerelor prin parolă și autentificare.
- Configurarea unui server TURN pentru conectivitate mai bună.
- Implementarea moderării și a gestionării erorilor de conexiune.
- Implementarea unei soluții de stocare și protecție a mesajelor pentru producție.

## Licență

În `package.json`, proiectul declară licența **MIT**. Pentru publicarea formală sub această licență, adaugă și un fișier `LICENSE` cu textul licenței.
