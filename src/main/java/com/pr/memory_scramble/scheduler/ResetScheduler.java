package com.pr.memory_scramble.scheduler;

import com.pr.memory_scramble.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResetScheduler {

    private final Board board;

    @Scheduled(fixedRate = 10 * 1000)
    public void resetBoard() {
        board.reset();
    }
}

