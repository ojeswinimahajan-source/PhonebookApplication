package com.phonebook.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.phonebook.model.LoginRequest;
import com.phonebook.model.LoginResponse;
import com.phonebook.model.User;
import com.phonebook.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordService passwordService,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordService = passwordService;
        this.jwtService = jwtService;
    }

    public Optional<LoginResponse> login(LoginRequest request) {

        Optional<User> userOptional =
            userRepository.getUserForLogin(request.getUsername());

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        User user = userOptional.get();

        boolean passwordMatches = passwordService.verifyPassword(
            request.getPassword(),
            user.getPasswordHash()
        );

        if (!passwordMatches) {
            return Optional.empty();
        }

        return Optional.of(jwtService.createToken(user));
    }
}