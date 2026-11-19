package com.mishanthrope.queueboard.application.common;

public interface CommandHandler<C> {

    void handle(C command);
}
