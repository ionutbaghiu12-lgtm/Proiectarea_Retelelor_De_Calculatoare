package ro.chat67.security;

import java.util.List;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UtilizatorService implements UserDetailsService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public UtilizatorService(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    public record Cont(String email, String nume, String rol) { }

    public static String normalizeaza(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return jdbc.query("SELECT email, parola_hash, rol FROM utilizator WHERE email = ?",
                (rs, row) -> User.withUsername(rs.getString("email"))
                        .password(rs.getString("parola_hash")).roles(rs.getString("rol")).build(),
                normalizeaza(email)).stream().findFirst()
                .orElseThrow(() -> new UsernameNotFoundException("Cont inexistent"));
    }

    public void inregistreaza(String nume, String email, String parola) {
        // Rolul nu vine niciodata din formular.
        salveaza(nume, email, parola, "CLIENT");
    }

    void creeazaAdministrator(String email, String parola) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM utilizator WHERE email = ?",
                Integer.class, normalizeaza(email));
        if (count != null && count == 0) salveaza("Administrator", email, parola, "ADMIN");
    }

    private void salveaza(String nume, String email, String parola, String rol) {
        if (nume == null || nume.isBlank() || nume.trim().length() > 24
                || normalizeaza(email).length() > 150 || !normalizeaza(email).contains("@")
                || parola == null || parola.length() < 8
                || parola.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Date invalide. Parola trebuie sa aiba minimum 8 caractere si maximum 72 de octeti.");
        }
        jdbc.update("INSERT INTO utilizator (email, nume, parola_hash, rol) VALUES (?, ?, ?, ?)",
                normalizeaza(email), nume.trim(), encoder.encode(parola), rol);
    }

    public Cont cont(String email) {
        return jdbc.queryForObject("SELECT email, nume, rol FROM utilizator WHERE email = ?",
                (rs, row) -> new Cont(rs.getString("email"), rs.getString("nume"), rs.getString("rol")),
                normalizeaza(email));
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Cont> lista() {
        return jdbc.query("SELECT email, nume, rol FROM utilizator ORDER BY id",
                (rs, row) -> new Cont(rs.getString("email"), rs.getString("nume"), rs.getString("rol")));
    }
}
