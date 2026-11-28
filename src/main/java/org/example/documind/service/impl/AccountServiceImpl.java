package org.example.documind.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.documind.dao.UserRepository;
import org.example.documind.domain.entity.User;
import org.example.documind.dto.user.AccountDTO;
import org.example.documind.service.interfaces.AccountService;
import org.example.documind.web.exception.InvalidCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccount(Long userId) {
        User user = findUser(userId);
        return new AccountDTO(user.getId(), user.getUsername(), user.getEmail(), user.getCreatedAt());
    }

    @Override
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
