package com.pr.memory_scramble.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping")
public class AliveController {

    /**
     * Health check endpoint that returns the status of the application.
     * Used to verify the application is running and responsive.
     *
     * @return "UP" string indicating the application is alive
     */
    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public String ping() {
        return "UP";
    }
}
