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
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.DashboardService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Controller
public class PageController {

    private final AuthService authService;
    private final DashboardService dashboardService;

    public PageController(
            AuthService authService,
            DashboardService dashboardService) {

        this.authService = authService;
        this.dashboardService = dashboardService;
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
    public String dashboard(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        Long userId = userDetails.getId();
        model.addAttribute("activePage", "dashboard");

        model.addAttribute(
                "stats",
                dashboardService.getStatsForUser(userId)
        );

        String username = userDetails.getUsername();

        model.addAttribute("username", username);
        model.addAttribute(
                "avatarInitial",
                username.substring(0, 1).toUpperCase()
        );
        model.addAttribute(
                "recentDocuments",
                dashboardService.getRecentDocuments(userId)
        );


        return "dashboard";
    }
}