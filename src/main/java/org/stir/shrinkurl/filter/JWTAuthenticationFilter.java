package org.stir.shrinkurl.filter;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.stir.shrinkurl.dto.TokenPair;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.repository.UserRepository;
import org.stir.shrinkurl.service.RefreshTokenService;
import org.stir.shrinkurl.utils.JwtUtil;

import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JWTAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String accessToken = extractTokenFromCookie(request, "access_token");
        String refreshToken = extractTokenFromCookie(request, "refresh_token");
        
        try {
            DecodedJWT decodedToken = jwtUtil.validateToken(accessToken).orElse(null);
            
            if (decodedToken != null) {
                setAuthentication(decodedToken, request);
            } else if (refreshToken != null) {
                log.info("Access token expired, attempting refresh");
                
                if (handleTokenRefresh(refreshToken, response, request)) {
                    log.info("Token refreshed successfully");
                } else {
                    clearAuthCookies(response);
                    log.warn("Refresh token invalid, clearing cookies");
                }
            }
            
        } catch (Exception e) {
            log.error("Authentication error: {}", e.getMessage());
            clearAuthCookies(response);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private boolean handleTokenRefresh(String refreshToken, 
                                     HttpServletResponse response, 
                                     HttpServletRequest request) {
        try {
            Optional<User> userOpt = refreshTokenService.getUserFromRefreshToken(refreshToken);
            
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                
                // Generate new tokens
                TokenPair newTokens = jwtUtil.generateTokens(user);
                
                // Set new cookies
                setAuthCookies(response, newTokens);
                
                // Set authentication for current request
                DecodedJWT decodedToken = jwtUtil.validateToken(newTokens.getAccessToken()).orElse(null);
                if (decodedToken != null) {
                    setAuthentication(decodedToken, request);
                    return true;
                }
            }
            
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
        }
        
        return false;
    }
    
    private void setAuthentication(DecodedJWT decodedToken, HttpServletRequest request) {
        String email = decodedToken.getClaim("email").asString();
        Long userId = decodedToken.getClaim("userId").asLong();
        List<String> roles = decodedToken.getClaim("roles").asList(String.class);
        
        // Fetch the actual User object from database
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            
            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
            if (roles != null) {
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
                }
            } else {
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            
            // Set User object as principal instead of email string
            UsernamePasswordAuthenticationToken authToken = 
                new UsernamePasswordAuthenticationToken(user, null, authorities);
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            
            SecurityContextHolder.getContext().setAuthentication(authToken);
        }
    }
    
    private void setAuthCookies(HttpServletResponse response, TokenPair tokens) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", tokens.getAccessToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ofHours(1))
            .sameSite("Lax")
            .build();
        
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokens.getRefreshToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ofDays(30))
            .sameSite("Lax")
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
    
    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie clearAccessToken = ResponseCookie.from("access_token", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
        
        ResponseCookie clearRefreshToken = ResponseCookie.from("refresh_token", "")
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, clearAccessToken.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, clearRefreshToken.toString());
    }
    
    private String extractTokenFromCookie(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}