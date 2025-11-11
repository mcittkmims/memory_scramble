package com.pr.memory_scramble;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MemoryScrambleApplication {

    /**
     * Main entry point for the Memory Scramble application.
     * Initializes the Spring Boot application context and starts the application.
     *
     * @param args command line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(MemoryScrambleApplication.class, args);
    }

}
