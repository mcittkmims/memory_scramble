package com.pr.memory_scramble;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Card {
    private String value;
    private String controlledBy;
    private CardState state;

    public Card(String value){
        this.value = value;
        this.state = CardState.DOWN;
    }

    @Override
    public String toString(){
        return value;
    }

}
