package org.stir.shrinkurl.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.stir.shrinkurl.annotation.RateLimit;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.RateLimitService;

import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Aspect
@Component
@Slf4j
public class RateLimitAspect {
    
    @Autowired
    private RateLimitService rateLimitService;
    
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        
        HttpServletRequest request = getCurrentRequest();
        HttpServletResponse response = getCurrentResponse();
        
        if (request == null || response == null) {
            return joinPoint.proceed();
        }
        
        String rateLimitKey = buildRateLimitKey(request, rateLimit);
        
        // Check rate limit
        boolean allowed = rateLimitService.isAllowed(
            rateLimitKey,
            rateLimit.value(),
            rateLimit.timeWindow()
        );
        
        if (!allowed) {
            log.warn("Rate limit exceeded for key: {}", rateLimitKey);
            
            // Set rate limit headers
            RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(
                rateLimitKey,
                rateLimit.value(),
                rateLimit.timeWindow()
            );
            
            response.setHeader("X-RateLimit-Limit", String.valueOf(status.getLimit()));
            response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemaining()));
            response.setHeader("X-RateLimit-Reset", String.valueOf(status.getResetTime()));
            
            // Check if this is an API request (JSON expected) or browser request
            String acceptHeader = request.getHeader("Accept");
            boolean isApiRequest = acceptHeader != null && acceptHeader.contains("application/json");
            
            if (isApiRequest) {
                // Return JSON for API requests
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"" + rateLimit.message() + "\"}");
            } else {
                // Redirect to error page for browser requests
                int retryAfter = (int) Math.max(status.getResetTime() - System.currentTimeMillis() / 1000, 0);
                String errorUrl = "/error/rate-limit?key=" + rateLimitKey + 
                                 "&current=" + status.getCurrentCount() + 
                                 "&limit=" + status.getLimit() + 
                                 "&window=" + rateLimit.timeWindow() + 
                                 "&retryAfter=" + retryAfter +
                                 "&redirectUrl=" + request.getRequestURI();
                response.sendRedirect(errorUrl);
            }
            
            return null;
        }
        
        // Add rate limit headers to successful response
        RateLimitService.RateLimitStatus status = rateLimitService.getRateLimitStatus(
            rateLimitKey,
            rateLimit.value(),
            rateLimit.timeWindow()
        );
        
        response.setHeader("X-RateLimit-Limit", String.valueOf(status.getLimit()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(status.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(status.getResetTime()));
        
        return joinPoint.proceed();
    }
    
    private String buildRateLimitKey(HttpServletRequest request, RateLimit rateLimit) {
        if (!rateLimit.key().isEmpty()) {
            // Use custom key if provided
            return rateLimit.key();
        }
        
        if (rateLimit.perUser()) {
            // Use user ID if authenticated
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User) {
                User user = (User) auth.getPrincipal();
                return "user:" + user.getId();
            }
        }
        
        // Fallback to IP address
        String clientIp = getClientIpAddress(request);
        return "ip:" + clientIp;
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
    
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
    
    private HttpServletResponse getCurrentResponse() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getResponse() : null;
    }
}
