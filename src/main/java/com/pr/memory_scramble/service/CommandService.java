package com.pr.memory_scramble.service;

import com.pr.memory_scramble.model.Board;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommandService {
    private final Board board;

    String look(String playerId){
        throw new RuntimeException();
    };

    String flip(String playerId, int row, int column){
        throw new RuntimeException();
    }
}
