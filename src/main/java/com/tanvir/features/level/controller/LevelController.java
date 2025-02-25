package com.tanvir.features.level.controller;

import com.tanvir.features.level.application.port.in.LevelUseCase;
import com.tanvir.features.level.domain.Level;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class LevelController {

    private final LevelUseCase levelUseCase;

    public LevelController(LevelUseCase levelUseCase) {
        this.levelUseCase = levelUseCase;
    }


    @GetMapping("/level")
    public Mono<Level> getLevelByExpValue(@RequestParam("expValue") long expValue) {
        // Call the service to find the level for the given expValue.
        return levelUseCase.getLevelByExpValue(expValue);
    }
}

