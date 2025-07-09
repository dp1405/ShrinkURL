package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.stir.shrinkurl.dto.UrlShortenRequest;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MainController {

    @Autowired
    private UserRepository userRepository;
    
    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            User user = (User) authentication.getPrincipal();

            // To display user name at top
            model.addAttribute("user", user);

            // For getting URL shortening request
            model.addAttribute("urlForm", new UrlShortenRequest());
        }
        return "index";
    }
    
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("user", user);
        model.addAttribute("subscription", user.getSubscription());
        return "dashboard";
    }
}
