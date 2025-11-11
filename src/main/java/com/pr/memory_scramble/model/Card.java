package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.CardRemovedException;
import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import lombok.Getter;
import lombok.Setter;

/**
 * A mutable Card in a memory scramble game that can be flipped, matched, and
 * controlled by players.
 * Cards transition through states (DOWN, UP, CONTROLLED, NONE) and track player
 * ownership.
 */
public class Card {
    private String value;
    private String controlledBy;
    @Getter
    private CardState state;

    @Setter
    private Runnable stateListener;

    // Rep invariant:
    // - value != null && !value.isBlank()
    // - state in {DOWN, UP, CONTROLLED, NONE}
    // - if state == DOWN || state == NONE, then controlledBy == null
    // - if state == CONTROLLED, then controlledBy != null &&
    // !controlledBy.isBlank()
    // - stateListener may be null (listener is optional)
    //
    // Abstraction function:
    // AF(value, controlledBy, state, stateListener) =
    // A game card with display value 'value' that is:
    // - face down and unowned if state == DOWN
    // - face up and unowned (after failed match) if state == UP
    // - face up and owned by player 'controlledBy' if state == CONTROLLED
    // - removed from play if state == NONE
    //
    // Safety from rep exposure:
    // - All fields are private
    // - value is a String (immutable), but setValue() allows mutation for map
    // operations
    // - controlledBy is a String (immutable) and never returned directly
    // - state is a CardState enum (immutable), returned via getter but cannot be
    // modified externally
    // - stateListener is a Runnable interface reference; clients can set it but
    // cannot
    // observe or modify the rep through it since it only calls back to the board
    // - All methods are synchronized or properly guard access to mutable state
    // - The card maintains its own invariants through synchronized state
    // transitions

    /**
     * Constructs a new Card with the specified value.
     * The card is initialized in the DOWN state with no controller.
     *
     * @param value the display value of the card
     */
    public Card(String value) {
        this.value = value;
        this.state = CardState.DOWN;
    }

    /**
     * Notifies the registered state listener that a change has occurred.
     * Used to trigger board-level notifications when card state changes.
     */
    private void notifyChange() {
        if (stateListener != null)
            this.stateListener.run();
    }

    /**
     * Flips the card up as the first card in a matching attempt.
     * Waits if the card is currently controlled by another player.
     * Sets the card to CONTROLLED state and assigns ownership to the player.
     *
     * @param playerId the unique identifier of the player flipping the card
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws CardRemovedException if the card has already been matched and removed
     */
    public void flipUpAsFirst(String playerId) throws InterruptedException {
        boolean notify = false;
        synchronized (this) {
            if (isControlledByPlayer(playerId))
                return;
            while (state == CardState.CONTROLLED) {
                wait();
            }
            if (state == CardState.NONE) {
                throw new CardRemovedException("Card was already matched!");
            }
            if (state == CardState.DOWN)
                notify = true;

            state = CardState.CONTROLLED;
            this.controlledBy = playerId;
        }

        if (notify)
            notifyChange();
    }

    /**
     * Flips the card up as the second card in a matching attempt.
     * Does not wait - throws an exception if the card is not available.
     * Sets the card to CONTROLLED state and assigns ownership to the player.
     *
     * @param playerId the unique identifier of the player flipping the card
     * @throws RestrictedCardAccessException if the card is controlled by another
     *                                       player or already removed
     */
    public void flipUpAsSecond(String playerId) {
        boolean notify = false;
        synchronized (this) {
            if (state == CardState.CONTROLLED || state == CardState.NONE)
                throw new RestrictedCardAccessException("Card is controlled by another player or is removed");

            if (state == CardState.DOWN)
                notify = true;

            state = CardState.CONTROLLED;
            this.controlledBy = playerId;
        }
        if (notify)
            notifyChange();
    }

    /**
     * Flips the card face down if it is in CONTROLLED state.
     * Clears player ownership and notifies waiting threads.
     * Does not affect cards in NONE state (removed cards).
     */
    public void flipDown() {
        boolean notify = false;
        synchronized (this) {
            if (state != CardState.CONTROLLED && state != CardState.NONE) {
                state = CardState.DOWN;
                controlledBy = null;
                notifyAll();
                notify = true;
            }
        }
        if (notify)
            notifyChange();
    }

    /**
     * Relinquishes control of the card after a failed match attempt.
     * Changes the card state to UP (visible but uncontrolled) and notifies waiting
     * threads.
     */
    public void relinquishControl() {
        synchronized (this) {
            state = CardState.UP;
            notifyAll();
        }
    }

    /**
     * Removes the card from play after a successful match.
     * Sets the card state to NONE, clears ownership, and notifies waiting threads.
     */
    public void removeCard() {
        synchronized (this) {
            state = CardState.NONE;
            controlledBy = null;
            notifyAll();
        }
        notifyChange();
    }

    /**
     * Resets the card to its initial state.
     * Sets the card face down, clears ownership, and notifies waiting threads.
     */
    public void reset() {
        synchronized (this) {
            state = CardState.DOWN;
            controlledBy = null;
            notifyAll();
        }
        notifyChange();
    }

    /**
     * Returns the card's value as a string.
     *
     * @return the card's display value
     */
    @Override
    public synchronized String toString() {
        return value;
    }

    /**
     * Returns a string representation of the card from a player's perspective.
     * The representation depends on the card's state and whether the player
     * controls it.
     *
     * @param playerId the unique identifier of the player viewing the card
     * @return a string representation of the card's state (e.g., "down", "up
     *         value", "my value")
     */
    public synchronized String toString(String playerId) {
        return state.toString(isControlledByPlayer(playerId), value);
    }

    /**
     * Checks if this card is currently controlled by the specified player.
     * A card is controlled by a player if they own it and it's in CONTROLLED state.
     *
     * @param playerId the unique identifier of the player to check
     * @return true if the card is controlled by the specified player, false
     *         otherwise
     */
    public synchronized boolean isControlledByPlayer(String playerId) {
        return playerId.equals(controlledBy) && state == CardState.CONTROLLED;
    }

    /**
     * Checks if this card was previously controlled by the specified player.
     * A card was controlled by a player if they own it and it's in UP state
     * (unmatched).
     *
     * @param playerId the unique identifier of the player to check
     * @return true if the card was controlled by the specified player, false
     *         otherwise
     */
    public synchronized boolean wasControlledByPlayer(String playerId) {
        return playerId.equals(controlledBy) && state == CardState.UP;
    }

    /**
     * Checks if this card matches another card based on their values.
     *
     * @param card the card to compare with
     * @return true if both cards have the same value, false otherwise
     */
    public synchronized boolean matches(Card card) {
        return value.equals(card.getValue());
    }

    /**
     * Gets the current value of the card.
     *
     * @return the card's display value
     */
    public synchronized String getValue() {
        return value;
    }

    /**
     * Sets the value of the card.
     * Used during map operations to transform card values.
     *
     * @param value the new display value for the card
     */
    public synchronized void setValue(String value) {
        this.value = value;
    }

    /**
     * Creates a shallow copy of this card with the same value, state, and controller.
     * Used to provide read-only snapshots of card state without exposing the internal
     * mutable card reference. The copy is not registered with any state listeners.
     * 
     * Note: This method is not synchronized as it only reads immutable references.
     * The copied card maintains the same state and controlledBy values at the time
     * of copying, but subsequent changes to the original will not affect the copy.
     *
     * @return a new Card instance with the same value, state, and controller as this card
     */
    public Card copy() {
        Card copy = new Card(this.value);
        copy.controlledBy = this.controlledBy;
        copy.state = this.state;
        return copy;
    }

    /**
     * Checks the representation invariant of the card.
     * Verifies that the card value is valid and the state-ownership relationship is
     * consistent.
     *
     * @throws IllegalStateException if any invariant is violated
     */
    public void checkRep() {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Card value must not be null or blank");
        }

        switch (state) {
            case NONE, DOWN -> {
                if (controlledBy != null) {
                    throw new IllegalStateException("Card in state " + state + " must not be controlled by any player");
                }
            }
            case CONTROLLED -> {
                if (controlledBy == null || controlledBy.isBlank()) {
                    throw new IllegalStateException("Controlled card must have a valid player ID");
                }
            }
        }
    }

}
