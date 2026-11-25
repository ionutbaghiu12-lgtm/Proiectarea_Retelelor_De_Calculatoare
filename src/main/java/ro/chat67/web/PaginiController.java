package ro.chat67.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import ro.chat67.security.UtilizatorService;

@Controller
public class PaginiController {
    private final UtilizatorService utilizatori;

    public PaginiController(UtilizatorService utilizatori) { this.utilizatori = utilizatori; }

    public record Inregistrare(
            @NotBlank @Size(max = 24) String nume,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 72) String parola,
            @NotBlank String confirmare) { }

    @GetMapping("/login")
    public String login() { return "login"; }

    @GetMapping("/inregistrare")
    public String inregistrare() { return "inregistrare"; }

    @PostMapping("/inregistrare")
    public String inregistreaza(@Valid @ModelAttribute Inregistrare formular,
            BindingResult errors, Model model) {
        if (errors.hasErrors() || !formular.parola().equals(formular.confirmare())) {
            model.addAttribute("eroare", "Completeaza corect toate campurile. Parola: minimum 8 caractere; confirmarile trebuie sa coincida.");
            return "inregistrare";
        }
        try {
            utilizatori.inregistreaza(formular.nume(), formular.email(), formular.parola());
        } catch (DuplicateKeyException exception) {
            model.addAttribute("eroare", "Exista deja un cont cu acest email.");
            return "inregistrare";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("eroare", exception.getMessage());
            return "inregistrare";
        }
        return "redirect:/login?inregistrat";
    }

    @GetMapping({"/", "/index.html"})
    public String acasa(Principal principal, Model model) {
        model.addAttribute("cont", utilizatori.cont(principal.getName()));
        return "index";
    }

    @GetMapping("/room/{id:[a-z0-9-]{1,32}}")
    public String camera(Principal principal, Model model) {
        model.addAttribute("cont", utilizatori.cont(principal.getName()));
        return "room";
    }

    @GetMapping("/admin")
    public String admin(Model model) {
        model.addAttribute("conturi", utilizatori.lista());
        return "admin";
    }
}
