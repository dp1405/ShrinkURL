package org.stir.shrinkurl.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.stir.shrinkurl.dto.SubscriptionUsageDTO;
import org.stir.shrinkurl.entity.RefreshToken;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.SubscriptionService;
import org.stir.shrinkurl.utils.JwtUtil;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping("/user/usage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SubscriptionUsageDTO> getUsageStats(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(subscriptionService.getUsageStats(user));
    }
    
    @PostMapping("/auth/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refresh_token", required = false) String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token required");
        }
        
        Optional<RefreshToken> validToken = jwtUtil.validateRefreshToken(refreshToken);
        if (validToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
        }
        
        // Generate new access token
        // Implementation depends on how you retrieve user from refresh token
        return ResponseEntity.ok("New access token");
    }
}
