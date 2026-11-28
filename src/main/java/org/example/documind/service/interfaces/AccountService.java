package org.example.documind.service.interfaces;

import org.example.documind.dto.user.AccountDTO;

public interface AccountService {

    AccountDTO getAccount(Long userId);

    /**
     * @throws org.example.documind.web.exception.InvalidCredentialsException if currentPassword is wrong
     */
    void changePassword(Long userId, String currentPassword, String newPassword);
}
