package ro.chat67;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ro.chat67.security.UtilizatorService;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:security;DB_CLOSE_DELAY=-1",
    "app.chats-dir=target/test-chats", "app.admin.email=", "app.admin.password="
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UtilizatorService users;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;

    private String contNou() {
        String email = UUID.randomUUID() + "@chat67.test";
        users.inregistreaza("Participant", email, "test-parola-123");
        return email;
    }

    @Test
    void anonimNuPoateVedeaCameraSauIstoricul() throws Exception {
        mvc.perform(get("/room/abc123")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/api/chat/abc123")).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/ws")).andExpect(status().is3xxRedirection());
    }

    @Test
    void loginSiLogoutCuParolaReala() throws Exception {
        String email = contNou();
        var login = mvc.perform(formLogin("/login").user("email", email.toUpperCase())
                        .password("parola", "test-parola-123"))
                .andExpect(authenticated().withUsername(email)).andReturn();
        var session = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/").session(session)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Participant")));
        mvc.perform(get("/room/abc123").session(session)).andExpect(status().isOk());
        mvc.perform(post("/logout").session(session).with(csrf()))
                .andExpect(redirectedUrl("/login?iesire")).andExpect(unauthenticated());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void parolaGresitaEsteRespinsa() throws Exception {
        mvc.perform(formLogin("/login").user("email", contNou()).password("parola", "gresita"))
                .andExpect(unauthenticated()).andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void formulareleNecesitaCsrf() throws Exception {
        mvc.perform(post("/login").param("email", contNou()).param("parola", "test-parola-123"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/inregistrare")).andExpect(status().isForbidden());
        mvc.perform(post("/logout")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void clientNuPoateAccesaAdministrareaNiciDirectServiceul() throws Exception {
        assertThatThrownBy(() -> users.lista()).isInstanceOf(AccessDeniedException.class);
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
        mvc.perform(get("/api/chat/abc123")).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void administratorulVedeListaConturilor() throws Exception {
        contNou();
        mvc.perform(get("/admin")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Utilizatori")));
    }

    @Test
    void inregistrareaFolosesteBcryptSiNuAcceptaRolAdmin() throws Exception {
        String email = UUID.randomUUID() + "@chat67.test";
        mvc.perform(post("/inregistrare").with(csrf()).param("nume", "Ana")
                .param("email", email).param("parola", "test-parola-123")
                .param("confirmare", "test-parola-123").param("rol", "ADMIN"))
                .andExpect(redirectedUrl("/login?inregistrat"));
        var user = users.loadUserByUsername(email);
        assertThat(user.getPassword()).startsWith("{bcrypt}");
        assertThat(encoder.matches("test-parola-123", user.getPassword())).isTrue();
        assertThat(user.getAuthorities()).extracting("authority").containsExactly("ROLE_CLIENT");
        mvc.perform(formLogin("/login").user("email", email).password("parola", "test-parola-123"))
                .andExpect(authenticated());
    }

    @Test
    void emailDuplicatSiConfirmareGresitaNuCreeazaConturi() throws Exception {
        String email = contNou();
        mvc.perform(post("/inregistrare").with(csrf()).param("nume", "Ana")
                .param("email", email.toUpperCase()).param("parola", "test-parola-123")
                .param("confirmare", "test-parola-123"))
                .andExpect(status().isOk()).andExpect(model().attributeExists("eroare"));
        String other = UUID.randomUUID() + "@chat67.test";
        mvc.perform(post("/inregistrare").with(csrf()).param("nume", "Ana")
                .param("email", other).param("parola", "test-parola-123").param("confirmare", "diferita"))
                .andExpect(model().attributeExists("eroare"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM utilizator WHERE email = ?", Integer.class, other)).isZero();
    }
}
