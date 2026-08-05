package com.example.risk.history.api;

import com.example.risk.common.ExposureSummary;
import com.example.risk.common.OrderEvent;
import com.example.risk.history.service.HistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping
public class HistoryController {
    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExposureSummary record(@RequestBody OrderEvent event) {
        return historyService.record(event);
    }

    @GetMapping("/exposures/{clientId}/{symbol}")
    public ExposureSummary exposure(@PathVariable String clientId, @PathVariable String symbol) {
        return historyService.exposure(clientId, symbol);
    }

    @GetMapping("/exposures")
    public List<ExposureSummary> exposures() {
        return historyService.exposures();
    }
}
