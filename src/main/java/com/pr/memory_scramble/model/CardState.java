package com.pr.memory_scramble.model;

public enum CardState {
    DOWN {
        @Override
        public String toString(String playerId, String controlledBy, String value) {
            return "down";
        }
    },
    UP {
        @Override
        public String toString(String playerId, String controlledBy, String value) {
            return "up " + value;
        }
    },
    NONE {
        @Override
        public String toString(String playerId, String controlledBy, String value) {
            return "none";
        }
    },
    CONTROLLED {
        @Override
        public String toString(String playerId, String controlledBy, String value) {
            if (playerId.equals(controlledBy))
                return "my " + value;

            return "up " + value;
        }
    };


    public abstract String toString(String playerId, String controlledBy, String value);
}