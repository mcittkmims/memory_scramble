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

    @Scheduled(fixedRate = 2 * 60 * 1000)
    public void resetBoard() {
        board.reset();
    }
}

