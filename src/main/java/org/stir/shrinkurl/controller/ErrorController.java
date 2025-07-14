package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.stir.shrinkurl.service.RateLimitService;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Controller
@RequestMapping("/error")
@Slf4j
public class ErrorController {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * Rate limit error page
     */
    @GetMapping("/rate-limit")
    public String rateLimitError(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer window,
            @RequestParam(required = false) Integer retryAfter,
            @RequestParam(required = false) String redirectUrl,
            Model model) {
        
        log.info("Rate limit error page accessed with key: {}", key);
        
        // Set default values if not provided
        int currentRequests = Optional.ofNullable(current).orElse(0);
        int rateLimit = Optional.ofNullable(limit).orElse(100);
        int timeWindow = Optional.ofNullable(window).orElse(60);
        int retryAfterSeconds = Optional.ofNullable(retryAfter).orElse(60);
        
        // If key is provided, get actual rate limit status
        if (key != null && !key.isEmpty()) {
            try {
                RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key, rateLimit, timeWindow);
                currentRequests = status.getCurrentCount();
                rateLimit = status.getLimit();
                retryAfterSeconds = (int) Math.max(status.getResetTime() - System.currentTimeMillis() / 1000, 0);
            } catch (Exception e) {
                log.warn("Failed to get rate limit status for key: {}", key, e);
            }
        }
        
        // Add attributes to model
        model.addAttribute("currentRequests", currentRequests);
        model.addAttribute("rateLimit", rateLimit + " requests");
        model.addAttribute("timeWindow", timeWindow + " seconds");
        model.addAttribute("retryAfter", retryAfterSeconds);
        model.addAttribute("redirectUrl", redirectUrl != null ? redirectUrl : "/");
        
        return "error/rate-limit";
    }
    
    /**
     * Enhanced rate limit error page with better UX
     */
    @GetMapping("/rate-limit-enhanced")
    public String rateLimitErrorEnhanced(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer window,
            @RequestParam(required = false) Integer retryAfter,
            @RequestParam(required = false) String redirectUrl,
            Model model) {
        
        log.info("Enhanced rate limit error page accessed with key: {}", key);
        
        // Set default values if not provided
        int currentRequests = Optional.ofNullable(current).orElse(0);
        int rateLimit = Optional.ofNullable(limit).orElse(100);
        int timeWindow = Optional.ofNullable(window).orElse(60);
        int retryAfterSeconds = Optional.ofNullable(retryAfter).orElse(60);
        
        // If key is provided, get actual rate limit status
        if (key != null && !key.isEmpty()) {
            try {
                RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(key, rateLimit, timeWindow);
                currentRequests = status.getCurrentCount();
                rateLimit = status.getLimit();
                retryAfterSeconds = (int) Math.max(status.getResetTime() - System.currentTimeMillis() / 1000, 0);
            } catch (Exception e) {
                log.warn("Failed to get rate limit status for key: {}", key, e);
            }
        }
        
        // Add attributes to model
        model.addAttribute("currentRequests", currentRequests);
        model.addAttribute("rateLimit", rateLimit);
        model.addAttribute("timeWindow", timeWindow);
        model.addAttribute("retryAfter", retryAfterSeconds);
        model.addAttribute("redirectUrl", redirectUrl != null ? redirectUrl : "/");
        
        return "error/rate-limit-enhanced";
    }
    
    /**
     * Generic error page handler
     */
    @GetMapping("/generic")
    public String genericError(
            @RequestParam(required = false) String message,
            @RequestParam(required = false) String redirectUrl,
            Model model) {
        
        model.addAttribute("errorMessage", message != null ? message : "An unexpected error occurred");
        model.addAttribute("redirectUrl", redirectUrl != null ? redirectUrl : "/");
        
        return "error/generic";
    }
}
