package com.example.risk.pretrade;

import com.example.risk.pretrade.ptr.ControlPlane.RiskConfig;
import com.example.risk.pretrade.ptr.PtrRuntime;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Control/operations API only. Orders deliberately have no HTTP endpoint. */
@RestController @RequestMapping("/api")
public class ApiController {
    private final PtrRuntime runtime;
    public ApiController(PtrRuntime runtime){this.runtime=runtime;}
    @PostMapping("/config") @PreAuthorize("hasRole('RISK_ADMIN')") public RiskConfig configure(@RequestBody RiskConfig config){return runtime.write(config);}
    @GetMapping("/operations/runtime") @PreAuthorize("hasAnyRole('RISK_ADMIN','RISK_VIEWER')") public PtrRuntime.RuntimeView runtime(){return runtime.view();}
    @GetMapping("/internal/runtime") public PtrRuntime.RuntimeView sidecarRuntime(){return runtime.view();}
}
