package org.example.documind.service.interfaces;

import org.example.documind.dto.auth.AuthResponseDTO;
import org.example.documind.dto.auth.LoginRequestDTO;
import org.example.documind.dto.auth.RegisterRequestDTO;

public interface AuthService {

    AuthResponseDTO register(RegisterRequestDTO request);

    AuthResponseDTO login(LoginRequestDTO request);
}