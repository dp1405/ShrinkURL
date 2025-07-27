package org.stir.shrinkurl.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.stir.shrinkurl.dto.UrlShortenRequest;
import org.stir.shrinkurl.entity.Url;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.UrlService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class MainController {
    
    @Autowired
    private UrlService urlService;
    
    @GetMapping("/")
    public String home(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            User user = (User) authentication.getPrincipal();

            // To display user name at top
            model.addAttribute("user", user);

            // For getting URL shortening request
            model.addAttribute("urlForm", new UrlShortenRequest());
            
            // Check if there's a recent URL to display
            if (model.containsAttribute("shortenedUrl")) {
                // Flash attributes are already added by WebController
                log.debug("Displaying shortened URL from flash attributes");
            }
        }
        return "index";
    }
    
    @GetMapping("/dashboard")
    @PreAuthorize("isAuthenticated()")
    public String dashboard(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("user", user);
        model.addAttribute("subscription", user.getSubscription());
        
        // Get user dashboard statistics
        UrlService.UserDashboardStats stats = urlService.getUserDashboardStats(user.getId());
        model.addAttribute("stats", stats);
        
        // Get initial URLs (limit to 5 for initial load)
        List<Url> initialUrls = urlService.getRecentUserUrls(user.getId(), 5);
        model.addAttribute("recentUrls", initialUrls);
        
        // Check if there are more URLs to load
        long totalUrls = urlService.getUserUrlCount(user.getId());
        model.addAttribute("hasMoreUrls", totalUrls > 5);
        model.addAttribute("totalUrls", totalUrls);
        
        return "dashboard";
    }
}
