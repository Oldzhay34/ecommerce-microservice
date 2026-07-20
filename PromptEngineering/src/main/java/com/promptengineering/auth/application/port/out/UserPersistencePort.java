package com.promptengineering.auth.application.port.out;

import com.promptengineering.auth.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserPersistencePort {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
}