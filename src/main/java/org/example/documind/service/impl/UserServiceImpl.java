package org.example.documind.service.impl;

import org.example.documind.domain.entity.User;
import org.example.documind.security.CustomUserDetails;
import org.example.documind.service.interfaces.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Override
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)){
            throw new IllegalStateException("No authenticated user");
        }
        return ((CustomUserDetails) auth.getPrincipal()).getUser();
    }

    @Override
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
