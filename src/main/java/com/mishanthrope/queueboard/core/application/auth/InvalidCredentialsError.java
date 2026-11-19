package com.mishanthrope.queueboard.core.application.auth;

import com.mishanthrope.queueboard.core.application.common.ApplicationError;

public class InvalidCredentialsError extends ApplicationError {

    public InvalidCredentialsError() {
        super("Invalid email or password");
    }
}
