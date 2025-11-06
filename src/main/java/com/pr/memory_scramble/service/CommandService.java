package com.pr.memory_scramble.service;

import com.pr.memory_scramble.exception.InvalidCardAddressException;
import com.pr.memory_scramble.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class CommandService {
    private final Board board;

    @Async
    public CompletableFuture<String> look(String playerId){
        String result = board.toString(playerId);
        return CompletableFuture.completedFuture(result);
    };

    @Async
    public CompletableFuture<String> flip(String playerId, int row, int column) throws InterruptedException {
        if(row >= board.getRows() || column >= board.getColumns())
            throw new InvalidCardAddressException("Row and Column should not be bigger than the grid");
        String result =  board.flip(playerId, row *  board.getColumns() + column);
        return CompletableFuture.completedFuture(result);
    }
}
