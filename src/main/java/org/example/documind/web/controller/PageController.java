package org.example.documind.web.controller;

import org.example.documind.dto.auth.RegisterRequestDTO;
import org.example.documind.service.interfaces.AuthService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.example.documind.web.exception.UsernameAlreadyExistsException;
import org.example.documind.web.exception.EmailAlreadyExistsException;

@Controller
public class PageController {

    private final AuthService authService;

    public PageController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequestDTO());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registerRequest") RegisterRequestDTO request,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.register(request);
            return "redirect:/login?registered";

        } catch (UsernameAlreadyExistsException e) {
            bindingResult.rejectValue(
                    "username",
                    "username.exists",
                    e.getMessage()
            );

        } catch (EmailAlreadyExistsException e) {
            bindingResult.rejectValue(
                    "email",
                    "email.exists",
                    e.getMessage()
            );
        }

        return "auth/register";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}