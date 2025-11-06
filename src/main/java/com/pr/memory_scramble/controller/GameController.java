package com.pr.memory_scramble.controller;

import com.pr.memory_scramble.service.CommandService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
public class GameController {

    private final CommandService commandService;

    @GetMapping("/look/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    public CompletableFuture<String> look(@Valid @PathVariable @NotBlank String playerId){
        return commandService.look(playerId);
    }

    @GetMapping("/flip/{playerId}/{row},{column}")
    public CompletableFuture<String> flip(
            @Valid @PathVariable @NotBlank String playerId,
            @Valid @PathVariable @Min(0) int row,
            @Valid @PathVariable @Min(0) int column) throws InterruptedException {
        return commandService.flip(playerId, row, column);
    }
}
