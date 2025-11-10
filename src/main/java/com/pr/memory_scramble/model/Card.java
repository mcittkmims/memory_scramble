package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.CardRemovedException;
import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import lombok.Getter;
import lombok.Setter;


public class Card {
    private String value;
    private String controlledBy;
    @Getter
    private CardState state;

    @Setter
    private Runnable stateListener;

    public Card(String value) {
        this.value = value;
        this.state = CardState.DOWN;
    }

    private void notifyChange() {
        if (stateListener != null)
            this.stateListener.run();
    }

    public void flipUpAsFirst(String playerId) throws InterruptedException {
        boolean notify = false;
        synchronized (this) {
            if (isControlledByPlayer(playerId)) return;
            while (state == CardState.CONTROLLED) {
                wait();
            }
            if (state == CardState.NONE) {
                throw new CardRemovedException("Card was already matched!");
            }
            if (state == CardState.DOWN) notify = true;

            state = CardState.CONTROLLED;
            this.controlledBy = playerId;
        }

        if(notify) notifyChange();
    }

    public void flipUpAsSecond(String playerId) {
        boolean notify = false;
        synchronized (this) {
            if (state == CardState.CONTROLLED || state == CardState.NONE)
                throw new RestrictedCardAccessException("Card is controlled by another player or is removed");

            if (state == CardState.DOWN) notify = true;

            state = CardState.CONTROLLED;
            this.controlledBy = playerId;
        }
        if(notify) notifyChange();
    }

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
        if(notify) notifyChange();
    }

    public void relinquishControl() {
        synchronized (this) {
            state = CardState.UP;
            notifyAll();
        }
    }

    public void removeCard() {
        synchronized (this) {
            state = CardState.NONE;
            controlledBy = null;
            notifyAll();
        }
        notifyChange();
    }

    public void reset(){
        synchronized (this){
            state = CardState.DOWN;
            controlledBy = null;
            notifyAll();
        }
        notifyChange();
    }

    @Override
    public synchronized String toString() {
        return value;
    }

    public synchronized String toString(String playerId) {
        return state.toString(isControlledByPlayer(playerId), value);
    }

    public synchronized boolean isControlledByPlayer(String playerId) {
        return playerId.equals(controlledBy) && state == CardState.CONTROLLED;
    }

    public synchronized boolean wasControlledByPlayer(String playerId) {
        return playerId.equals(controlledBy) && state == CardState.UP;
    }

    public synchronized boolean matches(Card card) {
        return value.equals(card.getValue());
    }

    public synchronized String getValue() {
        return value;
    }

    public synchronized void setValue(String value) {
        this.value = value;
    }

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
