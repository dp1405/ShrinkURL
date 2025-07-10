package org.stir.shrinkurl.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.stir.shrinkurl.utils.UrlShortenerUtil;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/demo")
@Slf4j
public class DemoController {

    @Autowired
    private UrlShortenerUtil urlShortenerUtil;

    /**
     * Demo endpoint to test URL shortening algorithm
     */
    @PostMapping("/test-shortening")
    public Map<String, Object> testShortening(@RequestBody Map<String, String> request) {
        String originalUrl = request.get("url");
        
        if (originalUrl == null || originalUrl.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "URL is required");
            return error;
        }
        
        String shortCode = urlShortenerUtil.generateShortCode(originalUrl);
        String shortCodeWithSalt = urlShortenerUtil.generateShortCodeWithSalt(originalUrl, 1);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalUrl", originalUrl);
        response.put("shortCode", shortCode);
        response.put("shortCodeWithSalt", shortCodeWithSalt);
        response.put("algorithm", "CRC32 + Base62");
        response.put("timestamp", System.currentTimeMillis());
        
        return response;
    }
    
    /**
     * Demo endpoint to test custom code validation
     */
    @PostMapping("/validate-custom-code")
    public Map<String, Object> validateCustomCode(@RequestBody Map<String, String> request) {
        String customCode = request.get("customCode");
        
        if (customCode == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Custom code is required");
            return error;
        }
        
        boolean isValid = urlShortenerUtil.isValidCustomCode(customCode);
        
        Map<String, Object> response = new HashMap<>();
        response.put("customCode", customCode);
        response.put("isValid", isValid);
        response.put("length", customCode.length());
        response.put("rules", "3-20 characters, alphanumeric only");
        
        return response;
    }
    
    /**
     * Demo endpoint to show algorithm info
     */
    @GetMapping("/algorithm-info")
    public Map<String, Object> getAlgorithmInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("algorithm", "CRC32 Hash + Base62 Encoding");
        info.put("characters", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ");
        info.put("minLength", 6);
        info.put("collisionHandling", "Salt-based retry with attempt counter");
        info.put("cacheStorage", "Redis DB 0");
        info.put("rateLimitStorage", "Redis DB 1");
        info.put("features", new String[]{
            "Deterministic short codes",
            "Collision detection",
            "Premium custom codes",
            "Analytics tracking",
            "Subscription-based limits"
        });
        
        return info;
    }
}
