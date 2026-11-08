
package org.example.documind.service.impl;

import org.example.documind.dao.UserRepository;
import org.example.documind.dao.RoleRepository;
import org.example.documind.domain.entity.Role;
import org.example.documind.domain.entity.User;
import org.example.documind.domain.enums.RoleName;
import org.example.documind.dto.auth.AuthResponseDTO;
import org.example.documind.dto.auth.LoginRequestDTO;
import org.example.documind.dto.auth.RegisterRequestDTO;
import org.example.documind.security.JwtService;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.AuthService;
import org.example.documind.web.exception.EmailAlreadyExistsException;
import org.example.documind.web.exception.InvalidCredentialsException;
import org.example.documind.web.exception.UsernameAlreadyExistsException;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // INSCRIPTION
    @Override
    public AuthResponseDTO register(RegisterRequestDTO request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException(
                    "Username already exists"
            );
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException(
                    "Email already exists"
            );
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() ->
                        new IllegalStateException("ROLE_USER not found")
                );

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        user.getRoles().add(userRole);

        User savedUser = userRepository.save(user);

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);

        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(
                token,
                "Bearer",
                savedUser.getUsername(),
                "Registration successful"
        );
    }

    // CONNEXION
    @Override
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Invalid username or password"
                        )
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new InvalidCredentialsException(
                    "Invalid username or password"
            );
        }

        CustomUserDetails userDetails = new CustomUserDetails(user);

        String token = jwtService.generateToken(userDetails);

        return new AuthResponseDTO(
                token,
                "Bearer",
                user.getUsername(),
                "Authentication successful"
        );
    }
}
