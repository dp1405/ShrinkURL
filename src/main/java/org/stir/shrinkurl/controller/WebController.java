package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.stir.shrinkurl.dto.UrlShortenRequest;
import org.stir.shrinkurl.entity.Url;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.UrlService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class WebController {
    
    @Autowired
    private UrlService urlService;

    @Value("${url-shortener.base-url:http://localhost:8085}")
    private String baseUrl;
    
    /**
     * Handle URL shortening form submission
     */
    @PostMapping("/shorten")
    public String shortenUrl(
            @ModelAttribute UrlShortenRequest request,
            @AuthenticationPrincipal User user,
            RedirectAttributes redirectAttributes) {
        
        try {
            log.info("Shortening URL: {} for user: {}", request.getOriginalUrl(), user.getName());
            
            // Create URL using the service
            Url shortenedUrl = urlService.createShortenedUrl(request.getOriginalUrl(), user, null);
            
            // Build the full shortened URL
            String fullShortenedUrl = baseUrl + "/" + shortenedUrl.getShortCode();
            
            // Add success message and URL to redirect attributes
            redirectAttributes.addFlashAttribute("shortenedUrl", fullShortenedUrl);
            redirectAttributes.addFlashAttribute("shortCode", shortenedUrl.getShortCode());
            redirectAttributes.addFlashAttribute("originalUrl", request.getOriginalUrl());
            redirectAttributes.addFlashAttribute("success", "URL shortened successfully!");
            
        } catch (Exception e) {
            log.error("Error shortening URL", e);
            redirectAttributes.addFlashAttribute("error", "Error shortening URL: " + e.getMessage());
        }
        
        return "redirect:/?success=true";
    }
}
