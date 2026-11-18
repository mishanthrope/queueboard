package com.mishanthrope.queueboard;

import org.springframework.boot.SpringApplication;

public class TestQueueboardApplication {

    public static void main(String[] args) {
        SpringApplication.from(QueueboardApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }

}
