package com.pr.memory_scramble.service;

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

    public String flip(String playerId, int row, int column){
        throw new RuntimeException();
    }
}
