package com.phonebook.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.phonebook.model.User;
import com.phonebook.repository.UserRepository;

@Service
public class InitialUserProvisioningService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final String initialUsername;
    private final String initialPassword;

    public InitialUserProvisioningService(
            UserRepository userRepository,
            PasswordService passwordService,
            @Value("${initial-user.username:}") String initialUsername,
            @Value("${initial-user.password:}") String initialPassword) {

        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.initialUsername = initialUsername;
        this.initialPassword = initialPassword;
    }

    public void provision() {

        if (!StringUtils.hasText(initialUsername)) {
            throw new IllegalStateException(
                "Initial user username is not configured."
            );
        }

        if (userRepository.getUserForLogin(initialUsername).isPresent()) {
            return;
        }

        if (!StringUtils.hasText(initialPassword)) {
            throw new IllegalStateException(
                "Initial user password is not configured."
            );
        }

        User user = new User();
        user.setUsername(initialUsername);
        user.setPasswordHash(passwordService.hashPassword(initialPassword));

        userRepository.insertUserIfNotExists(user);
    }
}