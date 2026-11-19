package com.mishanthrope.queueboard.application.common;

public interface QueryHandler<Q, R> {

    R handle(Q query);
}
