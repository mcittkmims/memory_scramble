package com.pr.memory_scramble.service;

import com.pr.memory_scramble.exception.RestrictedCardAccessException;
import com.pr.memory_scramble.model.Board;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Log4j2
@Profile("simulation")
public class SimulationService {

    private final Board board;

    @Value("${simulation.players:1}")
    private int players;

    @Value("${simulation.tries:10}")
    private int tries;

    @Value("${simulation.size:5}")
    private int size;

    @Value("${simulation.max.delay.ms:100}")
    private int maxDelayMilliseconds;

    private final Random random = new Random();

    @PostConstruct
    public void runSimulation() {
        log.info("Starting simulation: {} players, {} tries each, board {}x{}", players, tries, board.getRows(), board.getColumns());

        ExecutorService executor = Executors.newFixedThreadPool(players);

        for (int i = 0; i < players; i++) {
            final String playerId = "P" + i;
            executor.submit(() -> simulatePlayer(playerId));
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(2, TimeUnit.MINUTES)) {
                executor.shutdownNow();
                log.warn("Simulation timed out");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Simulation interrupted", e);
        }

        log.info("Simulation finished.");
    }

    private void simulatePlayer(String playerId) {
        log.info("{} joined the game", playerId);

        for (int attempt = 0; attempt < tries; attempt++) {
            try {
                randomDelay();
                int firstIndex = randomIndex();
                log.info("{} attempting first flip at index {}", playerId, firstIndex);
                board.flip(playerId, firstIndex);

                randomDelay();
                int secondIndex = randomIndex();
                log.info("{} attempting second flip at index {}", playerId, secondIndex);
                board.flip(playerId, secondIndex);

            } catch (RestrictedCardAccessException e) {
                log.info("{} attempted restricted card access: {}", playerId, e.getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("{} interrupted", playerId);
                return;
            } catch (Exception e) {
                log.error("{} encountered error: {}", playerId, e.getMessage());
            }
        }

        log.info("{} finished playing", playerId);
    }


    private int randomIndex() {
        return random.nextInt(board.getRows() * board.getColumns());
    }

    private void randomDelay() throws InterruptedException {
        double minMs = 0.1;
        double delayMs = minMs + random.nextDouble() * (maxDelayMilliseconds - minMs);
        long delayNanos = (long) (delayMs * 1_000_000);
        TimeUnit.NANOSECONDS.sleep(delayNanos);
    }

}
