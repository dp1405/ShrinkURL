package org.stir.shrinkurl.filter;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.stir.shrinkurl.service.RateLimitService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@Slf4j
public class GlobalRateLimitFilter extends OncePerRequestFilter {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Value("${rate-limit.global.enabled:true}")
    private boolean globalRateLimitEnabled;
    
    @Value("${rate-limit.global.requests-per-minute:1000}")
    private int globalRequestsPerMinute;
    
    @Value("${rate-limit.global.requests-per-hour:10000}")
    private int globalRequestsPerHour;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, 
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        if (!globalRateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }
        
        String clientIp = getClientIpAddress(request);
        
        // Check per-minute limit
        if (!rateLimitService.isGloballyAllowed(clientIp + ":minute", globalRequestsPerMinute, 60)) {
            sendRateLimitResponse(response, "Global rate limit exceeded (per minute)");
            return;
        }
        
        // Check per-hour limit
        if (!rateLimitService.isGloballyAllowed(clientIp + ":hour", globalRequestsPerHour, 3600)) {
            sendRateLimitResponse(response, "Global rate limit exceeded (per hour)");
            return;
        }
        
        filterChain.doFilter(request, response);
    }
    
    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
