package com.mishanthrope.queueboard.core.application.common;

public interface CommandHandler<C> {

    void handle(C command);
}
