package com.phonebook.exception;

public class DuplicatePhoneException extends RuntimeException {

    public DuplicatePhoneException() {
        super("Phone number already exists.");
    }
}