# Securitatea chat67

## Alegerea pentru proiect

Folosim **sesiune**, deoarece paginile sunt servite de Spring Boot/Thymeleaf. Serverul pastreaza identitatea, iar browserul trimite cookie-ul `JSESSIONID` (`HttpOnly`, `SameSite=Lax`). Sesiunea expira dupa 30 de minute de inactivitate HTTP; daca ramai numai in apel, poate fi necesara reautentificarea.

## Resursa x rol

| Resursa / actiune | Anonim | CLIENT | ADMIN |
| --- | --- | --- | --- |
| Login, inregistrare, CSS | Permis | Permis | Permis |
| Pagina principala si camere | Redirect la login | Permis | Permis |
| GET /api/chat/{id} | 401 ProblemDetail | Permis | Permis |
| WebSocket /ws | Refuzat | Permis | Permis |
| Mesaje si apeluri in camera curenta | Refuzat | Permis | Permis |
| Semnalizare spre alta camera | Refuzat | Refuzat | Refuzat |
| /admin si lista utilizatorilor | Redirect la login | 403 | Permis |
| POST /logout cu CSRF valid | Fara sesiune de inchis | Permis | Permis |

Un cont autentificat poate accesa o camera al carei cod il cunoaste. ADMIN adauga accesul la lista utilizatorilor, fara a ocoli izolarea semnalizarii intre camere.

## Fisiere importante

- `SecurityConfig.java`: reguli URL, login, logout, BCrypt, `@EnableMethodSecurity`.
- `UtilizatorService.java`: conturile H2 si rolul CLIENT fixat la inregistrare. Lista conturilor are `@PreAuthorize("hasRole('ADMIN')")`.
- `AdminInitial.java`: administrator optional din variabile de mediu; nu modifica un cont existent.
- `PaginiController.java`: formulare validate si pagini.
- `WebSocketConfig.java`: handshake autentificat, doar din aceeasi origine.
- `ChatSocketHandler.java`: verifica sesiunea la fiecare mesaj, limita de participanti si camera destinatarului. Logout inchide conexiunile; verificarea periodica inchide sesiunile invalidate.

## CSRF si JWT

CSRF protejeaza cererile care modifica starea atunci cand browserul trimite automat cookie-uri. Fiecare formular POST foloseste `th:action` pentru token. POST fara token valid primeste 403. Iesirea din cont se face prin POST, nu printr-un link GET.

JWT ar fi trimis explicit in `Authorization: Bearer ...`, potrivit unui API separat. Nu il folosim aici si nu dezactivam CSRF pentru `/api/**`. Nu exista endpoint de emitere JWT sau Swagger in aceasta varianta.

## Parole si date

Parolele sunt salvate cu `DelegatingPasswordEncoder` in format `{bcrypt}...`. Emailurile sunt normalizate la litere mici. Inregistrarea nu poate crea ADMIN nici daca browserul trimite `rol=ADMIN`. Baza si istoricul sunt excluse din Git; testele folosesc date separate.

## Verificare manuala

1. Deschide `/room/test` fara cont: redirect la login.
2. Creeaza cont si autentifica-te: camera devine accesibila.
3. Ca CLIENT, deschide `/admin`: 403.
4. Creeaza ADMIN conform README: acesta vede `/admin`.
5. Deschide aceeasi camera din doua browsere autentificate: verifica mesajele si apelul audio/video.
6. Iesi din cont intr-un tab: conexiunile din aceeasi sesiune se inchid.
7. Ruleaza `mvnw.cmd verify` pentru testele automate.
