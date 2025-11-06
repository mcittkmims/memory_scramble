package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class Board {
    private final int rows;
    private final int columns;

    private final List<Card> cards = new ArrayList<>();

    public Board(@Value("${memory.board-file:classpath:board.txt}") Resource resource) throws IOException {
        List<String> lines = Files.readAllLines(Path.of(resource.getFile().getPath()));
        String[] size = lines.getFirst().split("x");
        rows = Integer.parseInt(size[0]);
        columns = Integer.parseInt(size[1]);

        for (int i = 1; i <= rows * columns; i++) {
            String card = lines.get(i);
            if (card.isBlank()) {
                throw new IllegalArgumentException("The board config file should not have empty cards!");
            }
            cards.add(new Card(card));
        }
    }

    public String flip(String playerId, int row, int column) throws InterruptedException {
        int index = row * columns + column;

        cards.stream()
                .filter(card -> card.wasControlledByPlayer(playerId))
                .forEach(Card::flipDown);


        List<Card> controlledByPlayer = cards.stream().filter(card -> card.isControlledByPlayer(playerId)).toList();
        if(controlledByPlayer.size() == 2){
            controlledByPlayer.forEach(Card::removeCard);
        }


        try {
            if (controlledByPlayer.isEmpty() || controlledByPlayer.size() == 2) {
                cards.get(index).flipUpAsFirst(playerId);
            } else {
                cards.get(index).flipUpAsSecond(playerId);
            }
        } catch (RestrictedCardAccessException e) {
            controlledByPlayer.forEach(Card::relinquishControl);
        }

        controlledByPlayer = cards.stream().filter(card -> card.isControlledByPlayer(playerId)).toList();

        if (controlledByPlayer.size() == 2){
            if (!controlledByPlayer.getFirst().matches(controlledByPlayer.getLast()))
                controlledByPlayer.forEach(Card::relinquishControl);
        }



        return this.toString(playerId);

    }

    public String toString(String playerId) {
        StringBuilder board = new StringBuilder(rows + "x" + columns);
        cards.forEach(card -> board.append("\n").append(card.toString(playerId)));

        return board.toString();
    }
}
