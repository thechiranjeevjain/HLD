package com.example.capstone.shortener.link;

import com.example.capstone.shortener.rate.RateLimitExceededException;
import com.example.capstone.shortener.rate.RedisRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class LinkController {

    private final LinkService linkService;
    private final RedisRateLimiter rateLimiter;

    public LinkController(LinkService linkService, RedisRateLimiter rateLimiter) {
        this.linkService = linkService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/api/links")
    public ResponseEntity<LinkResponse> create(@Valid @RequestBody CreateLinkRequest request, HttpServletRequest httpRequest) {
        requireAllowed(clientIp(httpRequest) + ":create", 20, Duration.ofMinutes(1));
        LinkResponse response = linkService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/links/{code}")
                .buildAndExpand(response.code())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/links/{code}")
    public LinkResponse get(@PathVariable String code) {
        return linkService.get(code);
    }

    @GetMapping("/{code}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        requireAllowed(clientIp(request) + ":redirect", 120, Duration.ofMinutes(1));
        URI destination = linkService.redirect(code);
        return ResponseEntity.status(HttpStatus.FOUND).location(destination).build();
    }

    private void requireAllowed(String bucket, int limit, Duration window) {
        if (!rateLimiter.allow(bucket, limit, window)) {
            throw new RateLimitExceededException(limit, window);
        }
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
