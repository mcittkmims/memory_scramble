package com.pr.memory_scramble.controller;

import com.pr.memory_scramble.service.CommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GameController {

    private final CommandService commandService;

    @GetMapping("/look/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    public String look(@PathVariable String playerId){
        return commandService.look(playerId);
    }

    @GetMapping("/flip/{playerId}/{row},{column}")
    public String flip(@PathVariable String playerId, @PathVariable int row, @PathVariable int column){
        throw new UnsupportedOperationException();
    }
}
