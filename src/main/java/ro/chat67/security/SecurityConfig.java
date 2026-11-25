package ro.chat67.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import ro.chat67.chat.ChatSocketHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain webSecurity(HttpSecurity http, ObjectMapper mapper,
            ChatSocketHandler sockets) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/inregistrare", "/css/**", "/error").permitAll()
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .formLogin(form -> form.loginPage("/login")
                .usernameParameter("email").passwordParameter("parola")
                .defaultSuccessUrl("/").permitAll())
            .logout(logout -> logout
                .addLogoutHandler((request, response, authentication) -> {
                    var session = request.getSession(false);
                    if (session != null) sockets.inchideSesiunea(session.getId());
                })
                .logoutSuccessUrl("/login?iesire").permitAll())
            .exceptionHandling(errors -> errors.authenticationEntryPoint(
                (request, response, exception) -> {
                    if (!request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
                        new LoginUrlAuthenticationEntryPoint("/login").commence(request, response, exception);
                        return;
                    }
                    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                            "Autentifica-te pentru a accesa conversatia.");
                    problem.setTitle("Neautentificat");
                    problem.setInstance(URI.create(request.getRequestURI()));
                    response.setStatus(401);
                    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    mapper.writeValue(response.getOutputStream(), problem);
                }));
        // Folosim cookie de sesiune inclusiv pentru API; CSRF ramane activ.
        return http.build();
    }
}
