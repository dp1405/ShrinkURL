package org.stir.shrinkurl.controller;

import java.io.IOException;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.stir.shrinkurl.dto.GoogleTokenResponse;
import org.stir.shrinkurl.dto.GoogleUserInfo;
import org.stir.shrinkurl.dto.LoginRequest;
import org.stir.shrinkurl.dto.RegisterRequest;
import org.stir.shrinkurl.dto.TokenPair;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.exceptions.EmailAlreadyExistsException;
import org.stir.shrinkurl.service.UserService;
import org.stir.shrinkurl.utils.JwtUtil;
import org.stir.shrinkurl.utils.OAuth2StateUtil;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

import org.stir.shrinkurl.service.OAuth2GoogleService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OAuth2GoogleService oAuth2GoogleService;
    
    @Autowired
    private OAuth2StateUtil oAuth2StateUtil;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @GetMapping("/login")
    public String loginPage(Model model) {
        model.addAttribute("loginForm", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("loginForm") LoginRequest loginRequest,
                       BindingResult result,
                       HttpServletResponse response,
                       RedirectAttributes redirectAttributes,
                       Model model) {
        
        if(result.hasErrors()) {
            return "auth/login";
        }
        
        try{
            // Authenticate user
            User user = userService.authenticateUser(loginRequest.getEmail(), loginRequest.getPassword());
            
            if (user == null) {
                result.rejectValue("email", "error.credentials", "Invalid email or password");
                return "auth/login";
            }
            
            // Generate JWT tokens
            TokenPair tokens = jwtUtil.generateTokens(user);
            
            // Set cookies
            setAuthCookies(response, tokens, loginRequest.isRememberMe());
            
            redirectAttributes.addFlashAttribute("success", "Login successful!");
            return "redirect:/";
        }
        catch (Exception e) {
            log.error("Login error", e);
            result.rejectValue("email", "error.login", "Login failed. Please try again.");
            return "auth/login";
        }
    }
    
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerForm", new RegisterRequest());
        return "auth/register";
    }
    
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerForm") RegisterRequest request,
                          BindingResult result,
                          RedirectAttributes redirectAttributes) {
        
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerUser(request);
            redirectAttributes.addFlashAttribute("success", "Registration successful! Please login.");
            return "redirect:/login";
        }
        catch (EmailAlreadyExistsException e) {
            result.rejectValue("email", "error.email", "Email already registered");
            return "auth/register";
        }
    }
    
    // OAuth2 endpoints
    @GetMapping("/auth/oauth2/google")
    public void initiateGoogleOAuth(HttpServletResponse response, HttpSession session) throws IOException {
        String state = oAuth2StateUtil.generateState();
        session.setAttribute("oauth2_state", state);
        
        String authorizationUrl = oAuth2GoogleService.buildAuthorizationUrl(state);
        response.sendRedirect(authorizationUrl);
    }
    
    @GetMapping("/auth/google/callback")
    public String handleGoogleCallback(@RequestParam("code") String code,
                                    @RequestParam("state") String state,
                                    @RequestParam(value = "error", required = false) String error,
                                    HttpSession session,
                                    HttpServletResponse response,
                                    RedirectAttributes redirectAttributes) {
        
        if (error != null) {
            redirectAttributes.addFlashAttribute("error", "OAuth2 authentication failed");
            return "redirect:/login";
        }
        
        String sessionState = (String) session.getAttribute("oauth2_state");
        session.removeAttribute("oauth2_state");
        
        if (sessionState == null || !sessionState.equals(state)) {
            redirectAttributes.addFlashAttribute("error", "Invalid state parameter");
            return "redirect:/login";
        }
        
        try {
            GoogleTokenResponse tokenResponse = oAuth2GoogleService.exchangeCodeForToken(code);
            GoogleUserInfo googleUserInfo = oAuth2GoogleService.getUserInfo(tokenResponse.getAccessToken());
            User user = userService.processOAuth2User(googleUserInfo);
            
            // Generate tokens and set cookies
            TokenPair tokens = jwtUtil.generateTokens(user);
            setAuthCookies(response, tokens, true);
            
            return "redirect:/";
        } catch (Exception e) {
            log.error("OAuth2 callback error", e);
            redirectAttributes.addFlashAttribute("error", "Authentication failed");
            return "redirect:/login";
        }
    }

    private void setAuthCookies(HttpServletResponse response, TokenPair tokens, boolean rememberMe) {
        ResponseCookie accessTokenCookie = ResponseCookie.from("access_token", tokens.getAccessToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ofHours(1))
            .sameSite("Lax")
            .build();
        
        Duration refreshTokenDuration = rememberMe ? Duration.ofDays(30) : Duration.ofHours(1);
        ResponseCookie refreshTokenCookie = ResponseCookie.from("refresh_token", tokens.getRefreshToken())
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(refreshTokenDuration)
            .sameSite("Lax")
            .build();
        
        response.addHeader(HttpHeaders.SET_COOKIE, accessTokenCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString());
    }
}
