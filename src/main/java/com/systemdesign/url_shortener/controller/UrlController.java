package com.systemdesign.url_shortener.controller;

import com.systemdesign.url_shortener.service.UrlService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/url")
public class UrlController {
    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    // Endpoint to create a short URL
    @PostMapping("/shorten")
    public ResponseEntity<String> shortenUrl(String longUrl, Integer expirationDays) {
        urlService.shortenUrl(longUrl);
        return ResponseEntity.ok("URL shortened successfully");
    }

    // Endpoint to redirect to the original URL
    @GetMapping("/{shortCode}")
    public ResponseEntity<String> redirectToLongUrl(String shortCode) {
        String longUrl = urlService.getLongUrl(shortCode);
        if (longUrl != null) {
            return ResponseEntity.ok(longUrl);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    //GET `/api/analytics/{shortCode}`
    @GetMapping("/analytics/{shortCode}")
    public ResponseEntity<String> getUrlStats(String shortCode) {
        urlService.getUrlStats(shortCode);
        return ResponseEntity.ok("URL statistics for " + shortCode);
    }
}
