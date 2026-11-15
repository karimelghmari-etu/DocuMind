package org.example.documind.service.interfaces;

import org.example.documind.domain.entity.User;

public interface UserService {
    User getCurrentUser();
    Long getCurrentUserId();
}
