package com.promptengineering;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PromptEngineeringApplication {

    public static void main(String[] args) {
        SpringApplication.run(PromptEngineeringApplication.class, args);
    }

}
