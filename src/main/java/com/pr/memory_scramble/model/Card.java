package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.CardRemovedException;
import com.pr.memory_scramble.exception.RestrictedCardAccessException;


public class Card {
    private String value;
    private String controlledBy;
    private CardState state;

    public Card(String value){
        this.value = value;
        this.state = CardState.DOWN;
    }

    public synchronized void flipUpAsFirst(String playerId) throws InterruptedException {
        if (isControlledByPlayer(playerId)) return;
        while (state == CardState.CONTROLLED) {
            wait();
        }
        if(state == CardState.NONE){
            throw new CardRemovedException("Card was already matched!");
        }
        state = CardState.CONTROLLED;
        this.controlledBy = playerId;
    }

    public synchronized void flipUpAsSecond(String playerId){
        if(isControlledByPlayer(playerId)) return;
        if (state == CardState.CONTROLLED || state == CardState.NONE)
            throw new RestrictedCardAccessException("Card is controlled by another player or is removed");
        state = CardState.CONTROLLED;
        this.controlledBy = playerId;
    }

    public synchronized void flipDown(){
        if(state != CardState.CONTROLLED && state != CardState.NONE){
            state = CardState.DOWN;
            controlledBy = null;
        }
        notifyAll();
    }

    public synchronized void relinquishControl() {
        state = CardState.UP;
        notifyAll();
    }

    public synchronized void removeCard() {
        state = CardState.NONE;
        controlledBy = null;
        notifyAll();
    }

    @Override
    public String toString(){
        return value;
    }

    public String toString(String playerId){
        return state.toString(isControlledByPlayer(playerId), value);
    }

    public boolean isControlledByPlayer(String playerId){
        return playerId.equals(controlledBy) && state == CardState.CONTROLLED;
    }

    public boolean wasControlledByPlayer(String playerId){
        return playerId.equals(controlledBy) && state == CardState.UP;
    }

    public boolean matches(Card card){
        return value.equals(card.value);
    }
}
