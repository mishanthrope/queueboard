package com.mishanthrope.queueboard.application.auth;

import com.mishanthrope.queueboard.application.common.ApplicationError;

public class InvalidCredentialsError extends ApplicationError {

    public InvalidCredentialsError() {
        super("Invalid email or password");
    }
}
