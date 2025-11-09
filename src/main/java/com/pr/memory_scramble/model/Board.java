package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class Board {
    @Getter
    private final int rows;
    @Getter
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

    public void flip(String playerId, int index) throws InterruptedException {
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
    }

    public synchronized String toString(String playerId) {
        StringBuilder board = new StringBuilder(rows + "x" + columns);
        cards.forEach(card -> board.append("\n").append(card.toString(playerId)));

        return board.toString();
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

    public void map(String from, String to){
        List<Card> fromCards = cards.stream()
                .filter(card -> card.getValue().equals(from))
                .sorted(Comparator.comparingInt(System::identityHashCode))
                .toList();

        map(to, fromCards, 0);
    }

    private void map(String to, List<Card> fromCards, int index){
        if(index >= fromCards.size()) {
            fromCards.forEach(card-> card.setValue(to));
            return;
        }
        Card currentCard = fromCards.get(index);

        synchronized (currentCard){
            map(to, fromCards, index + 1);
        }
    }

}

