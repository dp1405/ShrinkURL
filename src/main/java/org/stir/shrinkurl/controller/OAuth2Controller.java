package org.stir.shrinkurl.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.stir.shrinkurl.dto.GoogleTokenResponse;
import org.stir.shrinkurl.dto.GoogleUserInfo;
import org.stir.shrinkurl.dto.TokenPair;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.OAuth2GoogleService;
import org.stir.shrinkurl.service.UserService;
import org.stir.shrinkurl.utils.JwtUtil;
import org.stir.shrinkurl.utils.OAuth2StateUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OAuth2Controller {

    @Autowired
    private OAuth2GoogleService oAuth2GoogleService;

    @Autowired
    private UserService userService;

    @Autowired
    private OAuth2StateUtil oAuth2StateUtil;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/oauth2/google")
    public void initiateGoogleOAuth(HttpServletResponse response, HttpSession session) throws IOException {
        String state = oAuth2StateUtil.generateState();
        String authorizationUrl = oAuth2GoogleService.buildAuthorizationUrl(state);
        
        // Save state in session for verification
        session.setAttribute("oauth2_state", state);
        
        log.info("Initiating OAuth2 flow with state: {}", state);
        response.sendRedirect(authorizationUrl);
    }
    
    // Step 2: Handle Google callback
    @GetMapping("/google/callback")
    public void handleGoogleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            @RequestParam(value = "error", required = false) String error,
            HttpSession session,
            HttpServletResponse response) throws IOException {
        
        try {
            // Handle errors from Google
            if (error != null) {
                log.error("OAuth2 error: {}", error);
                response.sendRedirect("/login?error=oauth2_" + error);
                return;
            }
            
            // Step 3: Verify state parameter
            String sessionState = (String) session.getAttribute("oauth2_state");
            session.removeAttribute("oauth2_state"); // Use state only once
            
            if (sessionState == null || !sessionState.equals(state)) {
                log.error("Invalid state parameter. Session: {}, Received: {}", sessionState, state);
                response.sendRedirect("/login?error=invalid_state");
                return;
            }
            
            log.info("State verified successfully");
            
            // Step 4: Exchange code for tokens
            GoogleTokenResponse tokenResponse = oAuth2GoogleService.exchangeCodeForToken(code);
            log.info("Successfully exchanged code for tokens");
            
            // Step 5: Get user info from Google
            GoogleUserInfo googleUserInfo = oAuth2GoogleService.getUserInfo(tokenResponse.getAccessToken());
            log.info("Retrieved user info: {}", googleUserInfo.getEmail());
            
            // Step 6: Create or update user in database
            User user = userService.processOAuth2User(googleUserInfo);
            
            // Step 7: Generate your own tokens
            TokenPair tokens = jwtUtil.generateTokens(user);
            
            // Step 8: Set cookies for remember-me functionality
            setAuthCookies(response, tokens);
            
            // Step 9: Redirect to home page
            response.sendRedirect("/home");
            
        } catch (Exception e) {
            log.error("OAuth2 callback error", e);
            response.sendRedirect("/login?error=authentication_failed");
        }
    }
    
    private void setAuthCookies(HttpServletResponse response, TokenPair tokens) {
        Cookie accessTokenCookie = new Cookie("access_token", tokens.getAccessToken());
        accessTokenCookie.setHttpOnly(true);
        accessTokenCookie.setSecure(false); // Set to true in production with HTTPS
        accessTokenCookie.setPath("/");
        accessTokenCookie.setMaxAge(60 * 60); // 1 hour
        
        Cookie refreshTokenCookie = new Cookie("refresh_token", tokens.getRefreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false); // Set to true in production
        refreshTokenCookie.setPath("/");
        refreshTokenCookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        
        response.addCookie(accessTokenCookie);
        response.addCookie(refreshTokenCookie);
    }
}
