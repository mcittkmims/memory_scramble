package com.pr.memory_scramble.model;

public enum CardState {
    DOWN("down"),
    UP("up"),
    NONE("none"),
    CONTROLLED("up"); // or "controlled" if preferred

    private final String label;

    CardState(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}