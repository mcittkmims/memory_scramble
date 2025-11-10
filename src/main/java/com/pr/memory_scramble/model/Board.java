package com.pr.memory_scramble.model;

import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class Board{
    @Getter
    private final int rows;
    @Getter
    private final int columns;

    private final List<Card> cards = new ArrayList<>();

    public Board(@Value("${memory.board-file:classpath:board.txt}") Resource resource) throws IOException {
        validateResource(resource);

        List<String> lines = loadLines(resource);
        String sizeLine = lines.getFirst();

        int[] dimensions = parseBoardSize(sizeLine);
        rows = dimensions[0];
        columns = dimensions[1];

        List<String> cardLines = extractCardLines(lines, rows * columns);

        initCards(cardLines);
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

    public String toString(String playerId) {
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

    public void map(Function<String, String> mapper) {
        List<Card> allCards = cards.stream()
                .sorted(Comparator.comparingInt(System::identityHashCode))
                .toList();

        map(mapper, allCards, 0);

        synchronized (this) {
            notifyAll();
        }
    }

    private void map(Function<String, String> mapper, List<Card> cards, int index) {
        if (index >= cards.size()) {
            cards.forEach(card -> card.setValue(mapper.apply(card.getValue())));
            return;
        }

        Card currentCard = cards.get(index);

        synchronized (currentCard) {
            map(mapper, cards, index + 1);
        }
    }

    public synchronized void watch() throws InterruptedException {
        wait();
    }

    public void reset() {
        List<Card> allCards = cards.stream()
                .sorted(Comparator.comparingInt(System::identityHashCode))
                .toList();

        reset(allCards, 0);

        synchronized (this) {
            notifyAll();
        }
    }

    private void reset(List<Card> cards, int index) {
        if (index >= cards.size()) {
            cards.forEach(Card::reset);
            return;
        }

        Card currentCard = cards.get(index);

        synchronized (currentCard) {
            reset(cards, index + 1);
        }
    }

    public Card getCard(int index){
        return cards.get(index);
    }

    private void validateResource(Resource resource) {
        if (resource == null || !resource.exists()) {
            throw new IllegalArgumentException("Board configuration file is missing!");
        }
    }

    private List<String> loadLines(Resource resource) throws IOException {
        List<String> lines = Files.readAllLines(resource.getFile().toPath())
                .stream()
                .filter(line -> !line.trim().isEmpty())
                .toList();

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Board configuration file is empty!");
        }

        return lines;
    }

    private int[] parseBoardSize(String sizeLine) {
        String[] parts = sizeLine.split("x");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid board size format. Expected 'NxM', found: " + sizeLine);
        }

        try {
            return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Board size must contain integers: " + sizeLine);
        }
    }

    private List<String> extractCardLines(List<String> lines, int expectedCards) {
        List<String> cardLines = lines.subList(1, lines.size());

        if (cardLines.size() < expectedCards) {
            throw new IllegalArgumentException("Not enough cards in config file. Expected " + expectedCards + ", found " + cardLines.size());
        }

        if (cardLines.size() > expectedCards) {
            throw new IllegalArgumentException("Too many cards in config file. Expected " + expectedCards + ", found " + cardLines.size());
        }

        for (int i = 0; i < cardLines.size(); i++) {
            if (cardLines.get(i).isBlank()) {
                throw new IllegalArgumentException("Card value at line " + (i + 2) + " is blank!");
            }
        }

        return cardLines;
    }

    private void initCards(List<String> cardLines) {
        for (String value : cardLines) {
            Card card = new Card(value.trim());
            card.setStateListener(() -> {
                synchronized (this) {
                    this.notifyAll();
                }
            });
            cards.add(card);
        }
    }
}