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

@RestController
@RequiredArgsConstructor
public class GameController {

    private final CommandService commandService;

    /**
     * Retrieves the current state of the game board for a specific player.
     * Shows cards that are visible to the player based on their interactions.
     *
     * @param playerId the unique identifier of the player viewing the board
     * @return a string representation of the board from the player's perspective
     */
    @GetMapping("/look/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    public String look(@Valid @PathVariable @NotBlank String playerId) {
        return commandService.look(playerId);
    }

    /**
     * Flips a card at the specified position for a player.
     * Handles the game logic for matching cards and player interactions.
     *
     * @param playerId the unique identifier of the player flipping the card
     * @param row      the row index of the card to flip (0-based)
     * @param column   the column index of the card to flip (0-based)
     * @return the updated board state after the flip operation
     * @throws InterruptedException if the thread is interrupted while waiting for
     *                              card access
     */
    @GetMapping("/flip/{playerId}/{row},{column}")
    @ResponseStatus(HttpStatus.OK)
    public String flip(
            @Valid @PathVariable @NotBlank String playerId,
            @Valid @PathVariable @Min(0) int row,
            @Valid @PathVariable @Min(0) int column) throws InterruptedException {
        return commandService.flip(playerId, row, column);
    }

    /**
     * Maps (replaces) all occurrences of one card value with another card value on
     * the board.
     * This operation changes the underlying card values for all matching cards.
     *
     * @param playerId the unique identifier of the player performing the map
     *                 operation
     * @param fromCard the original card value to be replaced
     * @param toCard   the new card value to replace with
     * @return the updated board state after the mapping operation
     */
    @GetMapping("/replace/{playerId}/{fromCard}/{toCard}")
    @ResponseStatus(HttpStatus.OK)
    public String map(
            @Valid @PathVariable @NotBlank String playerId,
            @Valid @PathVariable @NotBlank String fromCard,
            @Valid @PathVariable @NotBlank String toCard) {
        return commandService.map(playerId, fromCard, toCard);
    }

    /**
     * Waits for any change on the board and returns the updated board state.
     * This is a blocking operation that allows players to watch for board
     * modifications.
     *
     * @param playerId the unique identifier of the player watching the board
     * @return the board state after a change has occurred
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    @GetMapping("/watch/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    public String watch(@Valid @PathVariable @NotBlank String playerId) throws InterruptedException {
        return commandService.watch(playerId);
    }
}
