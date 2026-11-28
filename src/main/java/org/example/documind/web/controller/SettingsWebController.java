package org.example.documind.web.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.documind.dto.user.ChangePasswordDTO;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.AccountService;
import org.example.documind.web.exception.InvalidCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Account information and password change.
 * Form errors are shown next to each field (like the register page), not as a flash message.
 */
@Controller
@RequiredArgsConstructor
public class SettingsWebController {

    private final AccountService accountService;

    private void fillPage(Model model, CustomUserDetails userDetails) {
        String username = userDetails.getUsername();
        model.addAttribute("username", username);
        model.addAttribute("avatarInitial", username.substring(0, 1).toUpperCase());
        model.addAttribute("activePage", "settings");
        model.addAttribute("currentPage", "Settings");
        model.addAttribute("account", accountService.getAccount(userDetails.getId()));
    }

    @GetMapping("/settings")
    public String settingsPage(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model) {

        fillPage(model, userDetails);
        model.addAttribute("passwordForm", new ChangePasswordDTO());
        return "settings";
    }

    @PostMapping("/settings/password")
    public String changePassword(
            @Valid @ModelAttribute("passwordForm") ChangePasswordDTO form,
            BindingResult bindingResult,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasFieldErrors("confirmPassword")
                && form.getNewPassword() != null
                && !form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "The two passwords do not match");
        }

        if (!bindingResult.hasErrors()) {
            try {
                accountService.changePassword(userDetails.getId(), form.getCurrentPassword(), form.getNewPassword());
                redirectAttributes.addFlashAttribute("successMessage", "Your password was changed.");
                return "redirect:/settings";
            } catch (InvalidCredentialsException e) {
                bindingResult.rejectValue("currentPassword", "password.wrong", e.getMessage());
            }
        }

        // Never send typed passwords back to the browser.
        form.setCurrentPassword(null);
        form.setNewPassword(null);
        form.setConfirmPassword(null);

        fillPage(model, userDetails);
        return "settings";
    }
}
