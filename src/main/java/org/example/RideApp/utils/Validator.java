package org.example.RideApp.utils;

import org.example.RideApp.dtos.requests.LoginRequest;
import org.example.RideApp.dtos.requests.RegisterRequest;
import org.example.RideApp.exceptions.InvalidRequestException;

import java.util.regex.Pattern;

public class Validator {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    public static final int MIN_PASSWORD_LENGTH = 6;

    private Validator() {
    }

    public static void validateRegistration(RegisterRequest request) {
        if (request == null) throw new InvalidRequestException("Request body is required");
        requireText(request.getName(), "Name");
        requireText(request.getEmail(), "Email");
        if (!EMAIL.matcher(request.getEmail().trim()).matches())
            throw new InvalidRequestException("Email is not valid");
        requireText(request.getPhoneNumber(), "PhoneNumber");
        requireText(request.getPassword(), "Password");
        if (request.getPassword().length() < MIN_PASSWORD_LENGTH)
            throw new InvalidRequestException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
    }

    public static void validateLogin(LoginRequest request) {
        if (request == null) throw new InvalidRequestException("Request body is required");
        requireText(request.getEmail(), "Email");
        requireText(request.getPassword(), "Password");
    }

    public static void requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new InvalidRequestException(field + " is required");
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
