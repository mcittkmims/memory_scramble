package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A mutable Board representing the game state of a memory scramble game.
 * Manages a rectangular grid of cards and coordinates player interactions with
 * them.
 */
@Component
public class Board {
    @Getter
    private final int rows;
    @Getter
    private final int columns;
    private final List<Card> cards = new ArrayList<>();

    // Rep invariant:
    // - rows > 0 && columns > 0
    // - cards.size() == rows * columns
    // - all elements in cards are non-null
    // - no duplicate Card references in cards (each Card object appears exactly
    // once)
    // - each Card in cards satisfies its own rep invariant
    //
    // Abstraction function:
    // AF(rows, columns, cards) =
    // A rectangular game board with dimensions rows x columns, where
    // cards[i] represents the card at position i in row-major order
    // (row = i / columns, column = i % columns)
    //
    // Safety from rep exposure:
    // - rows and columns are primitive final int values, exposed via getters (safe
    // - immutable)
    // - cards is a private final List<Card>; never returned directly to clients
    // - getCard(index) returns references to mutable Card objects, but this is
    // intentional:
    // Cards are designed to be shared and mutated through their synchronized
    // methods
    // - Card objects maintain their own invariants through synchronization
    // - map() and reset() acquire locks in a consistent order to prevent deadlocks
    // - Board's own synchronization protects access to the cards list structure

    /**
     * Constructs a Board by loading configuration from a resource file.
     * Parses the board dimensions and card values, initializing all cards.
     *
     * @param resource the resource containing board configuration (format: first
     *                 line "NxM", followed by card values)
     * @throws IOException              if the resource cannot be read
     * @throws IllegalArgumentException if the board configuration is invalid
     */
    public Board(@Value("${memory.board-file:classpath:board.txt}") Resource resource) throws IOException {
        validateResource(resource);

        List<String> lines = loadLines(resource);
        String sizeLine = lines.getFirst();

        int[] dimensions = parseBoardSize(sizeLine);
        rows = dimensions[0];
        columns = dimensions[1];

        List<String> cardLines = extractCardLines(lines, rows * columns);

        initCards(cardLines);
        checkRep();
    }

    /**
     * Flips a card at the specified index for a player.
     * Manages the card matching logic, player control, and card state transitions.
     * Automatically flips down unmatched cards and removes matched pairs.
     *
     * @param playerId the unique identifier of the player flipping the card
     * @param index    the index of the card to flip
     * @throws InterruptedException          if the thread is interrupted while
     *                                       waiting for card access
     * @throws RestrictedCardAccessException if the card is controlled by another
     *                                       player
     */
    public void flip(String playerId, int index) throws InterruptedException {
        this.flipDownUnmatchedCards(playerId);
        this.removeMatchedCards(playerId);

        Card selectedCard = cards.get(index);
        Card previousCard = getPreviousControlledCard(playerId);

        if (previousCard == null) {
            selectedCard.flipUpAsFirst(playerId);
        }
        if (previousCard != null) {
            this.flipSecondCard(playerId, selectedCard, previousCard);
        }
        checkRep();
    }

    /**
     * Generates a string representation of the board from a player's perspective.
     * Shows the board dimensions and the state of each card as visible to the
     * specified player.
     *
     * @param playerId the unique identifier of the player viewing the board
     * @return a string representation of the board including dimensions and card
     *         states
     */
    public String toString(String playerId) {
        StringBuilder board = new StringBuilder(rows + "x" + columns);
        cards.forEach(card -> board.append("\n").append(card.toString(playerId)));
        return board.toString();
    }

    /**
     * Flips the second card in a matching attempt and handles match logic.
     * If cards don't match, relinquishes control of both cards.
     * If an exception occurs, ensures the first card's control is released.
     *
     * @param playerId     the unique identifier of the player flipping cards
     * @param selectedCard the second card being flipped
     * @param previousCard the first card that was previously flipped
     * @throws RestrictedCardAccessException if the selected card cannot be accessed
     */
    private void flipSecondCard(String playerId, Card selectedCard, Card previousCard) {
        try {
            selectedCard.flipUpAsSecond(playerId);
            if (!selectedCard.matches(previousCard)) {
                selectedCard.relinquishControl();
                previousCard.relinquishControl();
            }
        } catch (RestrictedCardAccessException e) {
            previousCard.relinquishControl();
            throw e;
        }
    }

    /**
     * Flips down all unmatched cards that were previously controlled by the
     * specified player.
     * Only affects cards in the UP state that belonged to this player.
     *
     * @param playerId the unique identifier of the player whose unmatched cards
     *                 should be flipped down
     */
    private void flipDownUnmatchedCards(String playerId) {
        cards.stream()
                .filter(card -> card.wasControlledByPlayer(playerId))
                .forEach(Card::flipDown);
    }

    /**
     * Removes matched card pairs that are currently controlled by the specified
     * player.
     * Only removes cards when exactly two cards are controlled by the player (a
     * matched pair).
     *
     * @param playerId the unique identifier of the player whose matched cards
     *                 should be removed
     */
    private void removeMatchedCards(String playerId) {
        Set<Card> controlledByPlayer = cards.stream()
                .filter(card -> card.isControlledByPlayer(playerId))
                .collect(Collectors.toSet());

        if (controlledByPlayer.size() == 2) {
            controlledByPlayer.forEach(Card::removeCard);
        }
    }

    /**
     * Retrieves the card that is currently controlled by the specified player.
     * Used to find the first card in a matching attempt.
     *
     * @param playerId the unique identifier of the player
     * @return the card controlled by the player, or null if no card is controlled
     */
    private Card getPreviousControlledCard(String playerId) {
        return cards.stream()
                .filter(card -> card.isControlledByPlayer(playerId))
                .findAny()
                .orElse(null);
    }

    /**
     * Applies a mapping function to all card values on the board.
     * Groups cards by their current value and applies the mapper to transform
     * values.
     * Notifies all waiting threads after the mapping is complete.
     * Uses synchronized access to ensure thread-safe value updates.
     *
     * @param mapper a function that transforms card values (oldValue -> newValue)
     */
    public void map(Function<String, String> mapper) {
        Map<String, List<Card>> groupsOfCards = cards.stream()
                .collect(Collectors.groupingBy(Card::getValue));

        groupsOfCards.values().forEach(list -> {
            list.sort(Comparator.comparingInt(System::identityHashCode));
            map(mapper, list, 0);
        });

        synchronized (this) {
            notifyAll();
        }
        checkRep();
    }

    /**
     * Recursively applies a mapping function to a list of cards with proper
     * synchronization.
     * Acquires locks on cards in a consistent order to prevent deadlocks.
     *
     * @param mapper the function to apply to card values
     * @param cards  the list of cards to map
     * @param index  the current index in the recursion
     */
    private void map(Function<String, String> mapper, List<Card> cards, int index) {
        if (index >= cards.size()) {
            cards.forEach(card -> card.setValue(mapper.apply(card.getValue())));
            return;
        }

        Card currentCard = cards.get(index);

        synchronized (currentCard) {
            map(mapper, cards, index + 1);
        }
    }

    /**
     * Waits for any change to occur on the board.
     * This is a blocking operation that waits until the board is modified.
     * Used by the watch functionality to observe board changes.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized void watch() throws InterruptedException {
        wait();
    }

    /**
     * Resets the entire board to its initial state.
     * Flips all cards face down and clears all player controls.
     * Notifies all waiting threads after the reset is complete.
     * Uses recursive synchronization to prevent deadlocks.
     */
    public void reset() {
        List<Card> allCards = cards.stream()
                .sorted(Comparator.comparingInt(System::identityHashCode))
                .toList();

        reset(allCards, 0);

        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * Recursively resets a list of cards with proper synchronization.
     * Acquires locks on cards in a consistent order to prevent deadlocks.
     *
     * @param cards the list of cards to reset
     * @param index the current index in the recursion
     */
    private void reset(List<Card> cards, int index) {
        if (index >= cards.size()) {
            cards.forEach(Card::reset);
            return;
        }

        Card currentCard = cards.get(index);

        synchronized (currentCard) {
            reset(cards, index + 1);
        }
    }

    /**
     * Retrieves a snapshot copy of the card at the specified index.
     * Returns a defensive copy to prevent external modification of the internal card state.
     * The returned copy reflects the card's state at the time of retrieval but is not
     * synchronized with subsequent changes to the original card.
     *
     * @param index the index of the card to retrieve (0-based, in row-major order)
     * @return a copy of the card at the specified index with the same value, state, and controller
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index >= rows * columns)
     */
    public Card getCard(int index) {
        return cards.get(index).copy();
    }

    /**
     * Validates that the board resource exists and is accessible.
     *
     * @param resource the resource to validate
     * @throws IllegalArgumentException if the resource is null or does not exist
     */
    private void validateResource(Resource resource) {
        if (resource == null || !resource.exists()) {
            throw new IllegalArgumentException("Board configuration file is missing!");
        }
    }

    /**
     * Parses the board size from a string in the format "NxM".
     *
     * @param sizeLine the string containing board dimensions (e.g., "4x5")
     * @return an array with two elements: [rows, columns]
     * @throws IllegalArgumentException if the format is invalid or contains
     *                                  non-integer values
     */
    private int[] parseBoardSize(String sizeLine) {
        String[] parts = sizeLine.split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid board size format. Expected 'NxM', found: " + sizeLine);
        }

        try {
            return new int[] { Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) };
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Board size must contain integers: " + sizeLine);
        }
    }

    /**
     * Extracts and validates card values from the configuration lines.
     * Ensures the number of cards matches the board dimensions.
     *
     * @param lines         all lines from the configuration file
     * @param expectedCards the expected number of cards based on board dimensions
     * @return a list of card value strings
     * @throws IllegalArgumentException if the card count doesn't match or any card
     *                                  value is blank
     */
    private List<String> extractCardLines(List<String> lines, int expectedCards) {
        List<String> cardLines = lines.subList(1, lines.size());

        if (cardLines.size() < expectedCards) {
            throw new IllegalArgumentException(
                    "Not enough cards in config file. Expected " + expectedCards + ", found " + cardLines.size());
        }

        if (cardLines.size() > expectedCards) {
            throw new IllegalArgumentException(
                    "Too many cards in config file. Expected " + expectedCards + ", found " + cardLines.size());
        }

        for (int i = 0; i < cardLines.size(); i++) {
            if (cardLines.get(i).isBlank()) {
                throw new IllegalArgumentException("Card value at line " + (i + 2) + " is blank!");
            }
        }

        return cardLines;
    }

    /**
     * Initializes all cards from the configuration and sets up state change
     * listeners.
     * Each card is configured to notify the board when its state changes.
     *
     * @param cardLines the list of card value strings from the configuration
     */
    private void initCards(List<String> cardLines) {
        for (String value : cardLines) {
            Card card = new Card(value.trim());
            card.setStateListener(() -> {
                synchronized (this) {
                    this.notifyAll();
                }
            });
            cards.add(card);
        }
    }

    /**
     * Loads all non-empty lines from the board configuration resource.
     *
     * @param resource the resource containing the board configuration
     * @return a list of non-empty lines from the resource
     * @throws IOException              if the resource cannot be read
     * @throws IllegalArgumentException if the file is empty
     */
    private List<String> loadLines(Resource resource) throws IOException {
        List<String> lines;
        try (var reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            lines = reader.lines()
                    .filter(line -> !line.trim().isEmpty())
                    .toList();
        }

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Board configuration file is empty!");
        }

        return lines;
    }

    /**
     * Checks the representation invariant of the board.
     * Verifies that the board has the correct number of cards, no null cards,
     * no duplicate card references, and that each card's invariants hold.
     *
     * @throws IllegalStateException if any invariant is violated
     */
    private void checkRep() {
        if (cards.size() != rows * columns) {
            throw new IllegalStateException(
                    "Board size mismatch: expected " + (rows * columns) + " cards, found " + cards.size());
        }

        Set<Card> unique = new HashSet<>();
        for (Card card : cards) {
            if (card == null) {
                throw new IllegalStateException("Board contains a null card");
            }
            if (!unique.add(card)) {
                throw new IllegalStateException("Duplicate card reference detected in board");
            }

            card.checkRep();
        }
    }

}