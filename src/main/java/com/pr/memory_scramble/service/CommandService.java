package com.pr.memory_scramble.service;

import com.pr.memory_scramble.exception.InvalidCardAddressException;
import com.pr.memory_scramble.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandService {
    private final Board board;

    public String look(String playerId){
        return board.toString(playerId);
    };

    public String flip(String playerId, int row, int column) throws InterruptedException {
        if(row >= board.getRows() || column >= board.getColumns())
            throw new InvalidCardAddressException("Row and Column should not be bigger than the grid");
        board.flip(playerId, row *  board.getColumns() + column);
        return board.toString(playerId);
    }

    public String map(String playerId, String from, String to) {
        board.map(from, to);
        return board.toString(playerId);
    }

    public String watch(String playerId) throws InterruptedException {
        board.watch(playerId);
        return board.toString(playerId);
    }
}
