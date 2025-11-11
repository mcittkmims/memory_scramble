package com.pr.memory_scramble.service;

import com.pr.memory_scramble.exception.InvalidCardAddressException;
import com.pr.memory_scramble.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandService {
    private final Board board;

    /**
     * Retrieves the current state of the board for a specific player.
     * Shows how the board appears from the player's perspective.
     *
     * @param playerId the unique identifier of the player viewing the board
     * @return a string representation of the board state
     */
    public String look(String playerId) {
        return board.toString(playerId);
    };

    /**
     * Flips a card at the specified row and column position for a player.
     * Converts 2D coordinates to a linear index before flipping.
     *
     * @param playerId the unique identifier of the player flipping the card
     * @param row      the row index of the card to flip (0-based)
     * @param column   the column index of the card to flip (0-based)
     * @return the updated board state after the flip operation
     * @throws InterruptedException        if the thread is interrupted while
     *                                     waiting for card access
     * @throws InvalidCardAddressException if the row or column is out of bounds
     */
    public String flip(String playerId, int row, int column) throws InterruptedException {
        if (row >= board.getRows() || column >= board.getColumns())
            throw new InvalidCardAddressException("Row and Column should not be bigger than the grid");
        board.flip(playerId, row * board.getColumns() + column);
        return board.toString(playerId);
    }

    /**
     * Maps all cards with a specific value to a new value.
     * Applies a transformation to all matching cards on the board.
     *
     * @param playerId the unique identifier of the player performing the map
     *                 operation
     * @param from     the original card value to be replaced
     * @param to       the new card value to replace with
     * @return the updated board state after the mapping operation
     */
    public String map(String playerId, String from, String to) {
        board.map(value -> value.equals(from) ? to : value);
        return board.toString(playerId);
    }

    /**
     * Waits for any change to occur on the board and returns the updated state.
     * This is a blocking operation that waits until the board is modified.
     *
     * @param playerId the unique identifier of the player watching the board
     * @return the board state after a change has occurred
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String watch(String playerId) throws InterruptedException {
        board.watch();
        return board.toString(playerId);
    }
}
