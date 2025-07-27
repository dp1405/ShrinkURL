package org.stir.shrinkurl.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.stir.shrinkurl.annotation.RateLimit;
import org.stir.shrinkurl.entity.Url;
import org.stir.shrinkurl.entity.UrlAnalytics;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.service.UrlService;

import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
public class AnalyticsPageController {

    @Autowired
    private UrlService urlService;

    /**
     * Get URL analytics as HTML page
     */
    @GetMapping("/api/urls/{id}/analytics-page")
    @PreAuthorize("hasRole('USER')")
    @RateLimit(value = 50, timeWindow = 60)
    public String getUrlAnalyticsPage(
            @PathVariable Long id,
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal User user,
            Model model) {
        
        try {
            // Verify URL belongs to user
            Optional<Url> urlOpt = urlService.getUserUrl(id, user.getId());
            if (!urlOpt.isPresent()) {
                model.addAttribute("error", "URL not found or you don't have access to it");
                return "error/generic";
            }
            
            Url url = urlOpt.get();
            List<UrlAnalytics> analytics = urlService.getUrlAnalytics(id, days);
            
            // Calculate additional statistics
            int totalClicks = url.getClickCount();
            int analyticsClicks = analytics.stream().mapToInt(UrlAnalytics::getClickCount).sum();
            
            // Calculate last 7 days clicks (take the last 7 entries since now ordered ASC)
            int last7DaysClicks = analytics.stream()
                .skip(Math.max(0, analytics.size() - 7))
                .mapToInt(UrlAnalytics::getClickCount)
                .sum();
            
            // Calculate today's clicks (last entry in analytics for ASC order)
            int todaysClicks = analytics.size() > 0 ? analytics.get(analytics.size() - 1).getClickCount() : 0;
            
            // Prepare data for charts with proper date formatting
            List<String> dates = analytics.stream()
                .map(a -> a.getClickDate().format(java.time.format.DateTimeFormatter.ofPattern("dd MMM")))
                .collect(Collectors.toList());
            List<Integer> clicks = analytics.stream()
                .map(UrlAnalytics::getClickCount)
                .collect(Collectors.toList());
            
            // Convert to JSON strings for JavaScript
            String datesJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(dates);
            String clicksJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clicks);
            
            // Debug logging
            System.out.println("Analytics data - Dates: " + dates);
            System.out.println("Analytics data - Clicks: " + clicks);
            System.out.println("Analytics data - Dates JSON: " + datesJson);
            System.out.println("Analytics data - Clicks JSON: " + clicksJson);
            System.out.println("Analytics data - Total entries: " + analytics.size());
            
            model.addAttribute("url", url);
            model.addAttribute("analytics", analytics);
            model.addAttribute("totalClicks", totalClicks);
            model.addAttribute("analyticsClicks", analyticsClicks);
            model.addAttribute("last7DaysClicks", last7DaysClicks);
            model.addAttribute("todaysClicks", todaysClicks);
            model.addAttribute("days", days);
            model.addAttribute("dates", dates);
            model.addAttribute("clicks", clicks);
            model.addAttribute("datesJson", datesJson);
            model.addAttribute("clicksJson", clicksJson);
            model.addAttribute("user", user);
            
            return "analytics/url-analytics";
        } catch (Exception e) {
            log.error("Error fetching analytics page for URL {}: {}", id, e.getMessage());
            model.addAttribute("error", "Failed to fetch analytics data");
            return "error/generic";
        }
    }
}
