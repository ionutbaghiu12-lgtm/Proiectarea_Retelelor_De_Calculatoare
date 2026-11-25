package ro.chat67.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AdminInitial implements ApplicationRunner {
    private final UtilizatorService utilizatori;
    private final String email;
    private final String parola;

    public AdminInitial(UtilizatorService utilizatori,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String parola) {
        this.utilizatori = utilizatori;
        this.email = email;
        this.parola = parola;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!email.isBlank() && !parola.isBlank()) utilizatori.creeazaAdministrator(email, parola);
    }
}
