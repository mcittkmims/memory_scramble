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
import java.util.Set;
import java.util.stream.Collectors;

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
        int index = getIndex(row, column);

        this.flipDownUnmatchedCards(playerId);
        this.removeMatchedCards(playerId);

        Card selectedCard = cards.get(index);
        Card previousCard = getPreviousControlledCard(playerId);

        if (previousCard == null) {
            selectedCard.flipUpAsFirst(playerId);
        }
        if (previousCard != null) {
            this.flipSecondCard(playerId, selectedCard, previousCard);
        }

        return this.toString(playerId);
    }

    private void flipSecondCard(String playerId, Card selectedCard, Card previousCard) {
        try {
            selectedCard.flipUpAsSecond(playerId);
            if (!selectedCard.matches(previousCard)) {
                selectedCard.relinquishControl();
                previousCard.relinquishControl();
            }
        } catch (RestrictedCardAccessException e) {
            previousCard.relinquishControl();
            throw e;
        }
    }

    public String toString(String playerId) {
        StringBuilder board = new StringBuilder(rows + "x" + columns);
        cards.forEach(card -> board.append("\n").append(card.toString(playerId)));

        return board.toString();
    }

    private int getIndex(int row, int column) {
        return row * columns + column;
    }

    private void flipDownUnmatchedCards(String playerId) {
        cards.stream()
                .filter(card -> card.wasControlledByPlayer(playerId))
                .forEach(Card::flipDown);
    }

    private void removeMatchedCards(String playerId) {
        Set<Card> controlledByPlayer = cards.stream()
                .filter(card -> card.isControlledByPlayer(playerId))
                .collect(Collectors.toSet());

        if (controlledByPlayer.size() == 2) {
            controlledByPlayer.forEach(Card::removeCard);
        }
    }

    private Card getPreviousControlledCard(String playerId) {
        return cards.stream()
                .filter(card -> card.isControlledByPlayer(playerId))
                .findAny()
                .orElse(null);
    }

}

