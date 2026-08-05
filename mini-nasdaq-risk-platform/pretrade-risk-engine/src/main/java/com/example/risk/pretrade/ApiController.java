package com.example.risk.pretrade;

import com.example.risk.pretrade.Models.ApiError;
import com.example.risk.pretrade.Models.CircuitBreakerRequest;
import com.example.risk.pretrade.Models.EngineState;
import com.example.risk.pretrade.Models.FixOrderRequest;
import com.example.risk.pretrade.Models.KillSwitchRequest;
import com.example.risk.pretrade.Models.MarketPriceRequest;
import com.example.risk.pretrade.Models.OrderDecision;
import com.example.risk.pretrade.Models.OrderRequest;
import com.example.risk.pretrade.Models.ScenarioResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final PreTradeRiskEngine engine;
    private final FixMessageParser parser;

    public ApiController(PreTradeRiskEngine engine, FixMessageParser parser) {
        this.engine = engine;
        this.parser = parser;
    }

    @GetMapping("/state")
    public EngineState state() {
        return engine.state();
    }

    @PostMapping("/orders")
    public OrderDecision submit(@Valid @RequestBody OrderRequest request) {
        return engine.submit(request);
    }

    @PostMapping("/fix/orders")
    public OrderDecision submitFix(@Valid @RequestBody FixOrderRequest request) {
        return engine.submit(parser.parse(request.message()));
    }

    @PostMapping("/market-data")
    public EngineState updateMarketData(@Valid @RequestBody MarketPriceRequest request) {
        return engine.updateMarketPrice(request);
    }

    @PostMapping("/kill-switch")
    public EngineState killSwitch(@Valid @RequestBody KillSwitchRequest request) {
        return engine.setKillSwitch(request);
    }

    @PostMapping("/circuit-breaker")
    public EngineState circuitBreaker(@RequestBody CircuitBreakerRequest request) {
        return engine.setCircuitBreaker(request);
    }

    @PostMapping("/fills/{orderId}")
    public EngineState fill(@PathVariable String orderId) {
        return engine.fill(orderId);
    }

    @PostMapping("/reset")
    public EngineState reset() {
        return engine.reset();
    }

    @PostMapping("/scenarios/{name}")
    public ScenarioResult scenario(@PathVariable String name) {
        return engine.runScenario(name);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> illegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message));
    }
}
