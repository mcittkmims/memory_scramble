package com.pr.memory_scramble.scheduler;

import com.pr.memory_scramble.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!simulation")
public class ResetScheduler {

    private final Board board;

    /**
     * Periodically resets the game board to its initial state.
     * Runs every 2 minutes to ensure fresh game starts.
     * Flips all cards face down and clears all player controls.
     * Only active when not in simulation profile.
     */
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void resetBoard() {
        board.reset();
    }
}
