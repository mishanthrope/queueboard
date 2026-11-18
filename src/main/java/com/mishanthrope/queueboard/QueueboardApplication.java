package com.mishanthrope.queueboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class QueueboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueueboardApplication.class, args);
    }

}
