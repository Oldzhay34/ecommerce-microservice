package com.promptengineering.auth.infrastructure.persistence.adapter;

import com.promptengineering.auth.application.port.out.UserPersistencePort;
import com.promptengineering.auth.domain.model.User;
import com.promptengineering.auth.infrastructure.persistence.entity.UserJpaEntity;
import com.promptengineering.auth.infrastructure.persistence.mapper.UserEntityMapper;
import com.promptengineering.auth.infrastructure.persistence.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserPersistenceAdapter implements UserPersistencePort {

    private final UserRepository userRepository;
    private final UserEntityMapper userEntityMapper;

    public UserPersistenceAdapter(UserRepository userRepository, UserEntityMapper userEntityMapper) {
        this.userRepository = userRepository;
        this.userEntityMapper = userEntityMapper;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entityToSave = userEntityMapper.toEntity(user);
        UserJpaEntity savedEntity = userRepository.save(entityToSave);
        return userEntityMapper.toDomain(savedEntity);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id).map(userEntityMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email).map(userEntityMapper::toDomain);
    }
}