package org.stir.shrinkurl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageInsightsDTO {
    private String pattern;
    private String recommendation;
    private String highestUsage;
    private String overallHealth; // CRITICAL, HIGH, MODERATE, LOW
    private int daysUntilLimit;
    private boolean needsUpgrade;
    
    public static UsageInsightsDTO fromUsage(SubscriptionUsageDTO usage) {
        String pattern = determinePattern(usage);
        String recommendation = generateRecommendation(usage);
        String highestUsage = findHighestUsage(usage);
        String overallHealth = calculateOverallHealth(usage);
        int daysUntilLimit = calculateDaysUntilLimit(usage);
        boolean needsUpgrade = shouldUpgrade(usage);
        
        return UsageInsightsDTO.builder()
            .pattern(pattern)
            .recommendation(recommendation)
            .highestUsage(highestUsage)
            .overallHealth(overallHealth)
            .daysUntilLimit(daysUntilLimit)
            .needsUpgrade(needsUpgrade)
            .build();
    }
    
    private static String determinePattern(SubscriptionUsageDTO usage) {
        double maxUsage = Math.max(usage.getUrlUsagePercentage(), 
                         Math.max(usage.getApiUsagePercentage(), usage.getStorageUsagePercentage()));
        
        if (maxUsage > 80) {
            return "Heavy usage - approaching limits";
        } else if (maxUsage > 50) {
            return "Moderate usage - steady growth";
        } else {
            return "Light usage - within comfortable limits";
        }
    }
    
    private static String generateRecommendation(SubscriptionUsageDTO usage) {
        if (usage.getUrlUsagePercentage() > 80) {
            return "Consider upgrading for more URL capacity";
        } else if (usage.getApiUsagePercentage() > 80) {
            return "Upgrade for higher API call limits";
        } else if (usage.getStorageUsagePercentage() > 80) {
            return "More storage space recommended";
        } else {
            return "Current plan suits your usage well";
        }
    }
    
    private static String findHighestUsage(SubscriptionUsageDTO usage) {
        double urlUsage = usage.getUrlUsagePercentage();
        double apiUsage = usage.getApiUsagePercentage();
        double storageUsage = usage.getStorageUsagePercentage();
        
        if (urlUsage >= apiUsage && urlUsage >= storageUsage) {
            return "URLs";
        } else if (apiUsage >= storageUsage) {
            return "API Calls";
        } else {
            return "Storage";
        }
    }
    
    private static String calculateOverallHealth(SubscriptionUsageDTO usage) {
        double maxUsage = Math.max(usage.getUrlUsagePercentage(), 
                         Math.max(usage.getApiUsagePercentage(), usage.getStorageUsagePercentage()));
        
        if (maxUsage > 95) {
            return "CRITICAL";
        } else if (maxUsage > 80) {
            return "HIGH";
        } else if (maxUsage > 50) {
            return "MODERATE";
        } else {
            return "LOW";
        }
    }
    
    private static int calculateDaysUntilLimit(SubscriptionUsageDTO usage) {
        // Simple estimation based on current usage
        double maxUsage = Math.max(usage.getUrlUsagePercentage(), 
                         Math.max(usage.getApiUsagePercentage(), usage.getStorageUsagePercentage()));
        
        if (maxUsage > 95) {
            return 1; // Very close to limit
        } else if (maxUsage > 80) {
            return 7; // About a week
        } else if (maxUsage > 50) {
            return 30; // About a month
        } else {
            return 90; // More than 3 months
        }
    }
    
    private static boolean shouldUpgrade(SubscriptionUsageDTO usage) {
        return usage.getUrlUsagePercentage() > 80 || 
               usage.getApiUsagePercentage() > 80 || 
               usage.getStorageUsagePercentage() > 80;
    }
}
