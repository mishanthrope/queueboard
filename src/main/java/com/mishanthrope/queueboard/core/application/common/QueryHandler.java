package com.mishanthrope.queueboard.core.application.common;

public interface QueryHandler<Q, R> {

    R handle(Q query);
}
