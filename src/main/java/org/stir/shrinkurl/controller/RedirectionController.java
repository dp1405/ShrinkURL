package org.stir.shrinkurl.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;
import org.stir.shrinkurl.annotation.RateLimit;
import org.stir.shrinkurl.service.UrlService;

import java.util.Optional;

@RestController
@Slf4j
public class RedirectionController {
    
    @Autowired
    private UrlService urlService;
    
    /**
     * Handle URL redirection
     */
    @GetMapping("/{shortCode}")
    @RateLimit(value = 200, timeWindow = 60, perUser = false, key = "redirect")
    public ResponseEntity<?> redirectToOriginalUrl(@PathVariable String shortCode) {
        try {
            Optional<String> originalUrl = urlService.resolveUrl(shortCode);
            
            if (originalUrl.isPresent()) {
                RedirectView redirectView = new RedirectView(originalUrl.get());
                redirectView.setStatusCode(HttpStatus.FOUND); // 302 redirect
                return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", originalUrl.get())
                    .build();
            } else {
                log.warn("Short code not found or expired: {}", shortCode);
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error redirecting short code {}: {}", shortCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred while processing your request");
        }
    }
    
    /**
     * Get redirection info without redirecting (for API clients)
     */
    @GetMapping("/api/resolve/{shortCode}")
    @RateLimit(value = 100, timeWindow = 60, perUser = false, key = "resolve")
    public ResponseEntity<?> resolveUrl(@PathVariable String shortCode) {
        try {
            Optional<String> originalUrl = urlService.resolveUrl(shortCode);
            
            if (originalUrl.isPresent()) {
                return ResponseEntity.ok(new UrlResolveResponse(shortCode, originalUrl.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (Exception e) {
            log.error("Error resolving short code {}: {}", shortCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An error occurred while resolving the URL"));
        }
    }
    
    /**
     * Response DTOs
     */
    public static class UrlResolveResponse {
        private final String shortCode;
        private final String originalUrl;
        
        public UrlResolveResponse(String shortCode, String originalUrl) {
            this.shortCode = shortCode;
            this.originalUrl = originalUrl;
        }
        
        public String getShortCode() { return shortCode; }
        public String getOriginalUrl() { return originalUrl; }
    }
    
    public static class ErrorResponse {
        private final String error;
        
        public ErrorResponse(String error) {
            this.error = error;
        }
        
        public String getError() { return error; }
    }
}
