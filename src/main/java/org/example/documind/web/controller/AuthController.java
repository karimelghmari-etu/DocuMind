
package org.example.documind.web.controller;

import org.example.documind.dto.auth.AuthResponseDTO;
import org.example.documind.dto.auth.LoginRequestDTO;
import org.example.documind.dto.auth.RegisterRequestDTO;
import org.example.documind.service.interfaces.AuthService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // Inscription
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(
            @RequestBody RegisterRequestDTO request
    ) {
        AuthResponseDTO response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Connexion
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(
            @RequestBody LoginRequestDTO request
    ) {
        AuthResponseDTO response = authService.login(request);

        return ResponseEntity.ok(response);
    }
}
