# Cerinte - chat67

Aplicație simplă de chat video/audio în timp real. Nu este matching random (Omegle/OmeTV). Te conectezi doar dacă ai **linkul camerei**.

## Functionale

- Creezi o camera si primesti un link de forma `/room/<cod>`.
- Trimiți linkul persoanei cu care vrei sa vorbesti.
- Video + audio in timp real (WebRTC).
- Chat text in dreapta, salvat pe server pentru acea camera.
- Camera, microfon si iesire din apel.

## Nefunctionale (inceput)

- Maxim 8 persoane pe camera (mesh WebRTC, pastrat simplu).
- Conversatia text ramane pe acelasi link daca revii.
- Functioneaza pe `localhost` cu camera/microfon din browser.

## Actualizare: Spring Security

- Server Java/Spring Boot, in loc de Node.js.
- Conturi in H2, parole BCrypt si login necesar pentru camere, istoric si WebSocket.
- Roluri CLIENT/ADMIN; numai ADMIN vede lista conturilor.
- Logout cu CSRF si inchiderea conexiunilor asociate.
- Detalii: [securitate.md](securitate.md).

## Ce nu e inclus

- Matching random
- Inregistrare video
- Moderare
