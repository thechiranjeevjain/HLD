package com.example.risk.history.api;

import com.example.risk.common.ExposureSummary;
import com.example.risk.history.domain.ExposureEvent;
import com.example.risk.history.service.ExposureService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/exposures")
public class ExposureController {
    private final ExposureService exposureService;

    public ExposureController(ExposureService exposureService) {
        this.exposureService = exposureService;
    }

    @GetMapping("/{clientId}/{symbol}")
    ExposureSummary summary(@PathVariable String clientId, @PathVariable String symbol) {
        return exposureService.summary(clientId, symbol);
    }

    @GetMapping("/{clientId}")
    List<ExposureEvent> recent(@PathVariable String clientId) {
        return exposureService.recent(clientId);
    }
}

