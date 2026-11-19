package com.mishanthrope.queueboard.application.auth;

import java.util.Optional;

public interface TokenService {

    String issue(String email);

    Optional<String> verify(String token);
}
