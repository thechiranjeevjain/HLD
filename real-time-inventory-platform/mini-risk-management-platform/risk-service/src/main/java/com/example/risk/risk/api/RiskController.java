package com.example.risk.risk.api;

import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.risk.service.RiskEvaluationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk")
public class RiskController {
    private final RiskEvaluationService riskEvaluationService;

    public RiskController(RiskEvaluationService riskEvaluationService) {
        this.riskEvaluationService = riskEvaluationService;
    }

    @PostMapping("/check")
    RiskCheckResponse check(@Valid @RequestBody RiskCheckRequest request) {
        return riskEvaluationService.evaluate(request);
    }
}

