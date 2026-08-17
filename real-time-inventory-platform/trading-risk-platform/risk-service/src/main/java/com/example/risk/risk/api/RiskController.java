package com.example.risk.risk.api;

import com.example.risk.common.RiskCheckRequest;
import com.example.risk.common.RiskCheckResponse;
import com.example.risk.risk.service.RiskPolicyService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/risk")
public class RiskController {
    private final RiskPolicyService riskPolicyService;

    public RiskController(RiskPolicyService riskPolicyService) {
        this.riskPolicyService = riskPolicyService;
    }

    @PostMapping("/check")
    public RiskCheckResponse check(@Valid @RequestBody RiskCheckRequest request) {
        return riskPolicyService.check(request);
    }
}
