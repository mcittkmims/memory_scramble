package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.CardRemovedException;
import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import lombok.Setter;


public class Card {
    private String value;
    private String controlledBy;
    private CardState state;

    @Setter
    private Runnable stateListener;

    public Card(String value){
        this.value = value;
        this.state = CardState.DOWN;
    }

    private void notifyChange(){
        this.stateListener.run();
    }

    public synchronized void flipUpAsFirst(String playerId) throws InterruptedException {
        if (isControlledByPlayer(playerId)) return;
        while (state == CardState.CONTROLLED) {
            wait();
        }
        if(state == CardState.NONE){
            throw new CardRemovedException("Card was already matched!");
        }
        if (state == CardState.DOWN) notifyChange();

        state = CardState.CONTROLLED;
        this.controlledBy = playerId;
    }

    public synchronized void flipUpAsSecond(String playerId){
        if (state == CardState.CONTROLLED || state == CardState.NONE)
            throw new RestrictedCardAccessException("Card is controlled by another player or is removed");

        if (state == CardState.DOWN) notifyChange();

        state = CardState.CONTROLLED;
        this.controlledBy = playerId;
    }

    public synchronized void flipDown(){
        if(state != CardState.CONTROLLED && state != CardState.NONE){
            state = CardState.DOWN;
            controlledBy = null;
        }
        notifyChange();
        notifyAll();
    }

    public synchronized void relinquishControl() {
        state = CardState.UP;
        notifyAll();
    }

    public synchronized void removeCard() {
        state = CardState.NONE;
        controlledBy = null;
        notifyChange();
        notifyAll();
    }

    @Override
    public synchronized String toString(){
        return value;
    }

    public synchronized String toString(String playerId){
        return state.toString(isControlledByPlayer(playerId), value);
    }

    public synchronized boolean isControlledByPlayer(String playerId){
        return playerId.equals(controlledBy) && state == CardState.CONTROLLED;
    }

    public synchronized boolean wasControlledByPlayer(String playerId){
        return playerId.equals(controlledBy) && state == CardState.UP;
    }

    public synchronized boolean matches(Card card){
        return value.equals(card.getValue());
    }

    public synchronized String getValue(){
        return value;
    }

    public synchronized void setValue(String value){
        this.value = value;
    }
}
