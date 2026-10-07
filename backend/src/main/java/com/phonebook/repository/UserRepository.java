package com.phonebook.repository;

import java.util.Optional;

import com.phonebook.model.User;

public interface UserRepository {

    Optional<User> getUserForLogin(String username);

    boolean insertUserIfNotExists(User user);
}