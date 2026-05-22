package com.systemdesign.url_shortener.controller;

import com.systemdesign.url_shortener.dto.ShortenUrlRequestDto;
import com.systemdesign.url_shortener.service.UrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/url")
public class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    // Endpoint to create a short URL
    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(@Valid @RequestBody ShortenUrlRequestDto request) {
        String shortUrl = urlService.shortenUrl(request.getLongUrl(), request.getExpirationDays(), request.getCustomCode());
        return ResponseEntity.ok(shortUrl);
    }

    // Endpoint to redirect to the original URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<String> redirectToLongUrl(@PathVariable String shortCode) {
        String longUrl = urlService.getLongUrl(shortCode);
        if (longUrl != null) {
            return ResponseEntity.ok(longUrl);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //GET `/api/analytics/{shortCode}`
    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<String> getUrlStats(@PathVariable String shortCode) {
        String stats = urlService.getUrlStats(shortCode);
        return ResponseEntity.ok(stats);
    }
}
