package com.pr.memory_scramble.scheduler;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Log4j2
public class AliveScheduler {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void keepAlive() {
        try {
            restTemplate.getForObject(baseUrl + "/ping", Void.class);
        } catch (Exception e) {
            log.error("Keep-alive ping to /ping failed: {}", e.getMessage());
        }
    }
}
