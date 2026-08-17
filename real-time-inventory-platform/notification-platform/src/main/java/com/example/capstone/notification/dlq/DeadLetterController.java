package com.example.capstone.notification.dlq;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dead-letter")
public class DeadLetterController {

    private final DeadLetterRepository deadLetterRepository;

    public DeadLetterController(DeadLetterRepository deadLetterRepository) {
        this.deadLetterRepository = deadLetterRepository;
    }

    @GetMapping
    public List<DeadLetterResponse> list() {
        return deadLetterRepository.findAll().stream()
                .map(DeadLetterResponse::from)
                .toList();
    }
}
