package com.pr.memory_scramble.model;

public enum CardState {
    DOWN {
        @Override
        public String toString(boolean controlled, String value) {
            return "down";
        }
    },
    UP {
        @Override
        public String toString(boolean controlled, String value) {
            return "up " + value;
        }
    },
    NONE {
        @Override
        public String toString(boolean controlled, String value) {
            return "none";
        }
    },
    CONTROLLED {
        @Override
        public String toString(boolean controlled, String value) {
            if (controlled)
                return "my " + value;

            return "up " + value;
        }
    };


    public abstract String toString(boolean controlled, String value);
}