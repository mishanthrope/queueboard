package com.mishanthrope.queueboard.core.application.auth;

public interface PasswordHasher {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
