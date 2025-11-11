package com.pr.memory_scramble.model;

/**
 * Represents the possible states of a card in the memory scramble game.
 * Each state defines how the card should be displayed to players.
 * 
 * This is an immutable enumerated type.
 * 
 * Rep invariant:
 * - true (enum values are fixed and always valid)
 * 
 * Abstraction function:
 * AF(DOWN) = card is face down, hidden from all players
 * AF(UP) = card is face up, visible to all players but not controlled
 * AF(CONTROLLED) = card is face up and controlled by a player during matching
 * AF(NONE) = card has been matched and removed from the game
 * 
 * Safety from rep exposure:
 * - Enum values are immutable and publicly accessible by design
 * - toString() returns a new String based on parameters, never internal state
 * - No mutable fields in this type
 */
public enum CardState {
    /**
     * Card is face down and not visible to any player.
     */
    DOWN {
        @Override
        public String toString(boolean controlled, String value) {
            return "down";
        }
    },
    /**
     * Card is face up but not controlled by any player (after a failed match).
     */
    UP {
        @Override
        public String toString(boolean controlled, String value) {
            return "up " + value;
        }
    },
    /**
     * Card has been successfully matched and removed from play.
     */
    NONE {
        @Override
        public String toString(boolean controlled, String value) {
            return "none";
        }
    },
    /**
     * Card is currently controlled by a player during a matching attempt.
     */
    CONTROLLED {
        @Override
        public String toString(boolean controlled, String value) {
            if (controlled)
                return "my " + value;

            return "up " + value;
        }
    };

    /**
     * Returns a string representation of the card state from a player's
     * perspective.
     *
     * @param controlled whether the card is controlled by the viewing player
     * @param value      the value of the card
     * @return a string representation of the card state
     */
    public abstract String toString(boolean controlled, String value);
}