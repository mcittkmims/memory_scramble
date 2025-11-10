package com.pr.memory_scramble.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping")
public class AliveController {

    @GetMapping()
    @ResponseStatus(HttpStatus.OK)
    public String ping(){
        return "UP";
    }
}
