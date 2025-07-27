package org.stir.shrinkurl.dto;

import org.stir.shrinkurl.enums.SubscriptionPlan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionUsageDTO {
    private Long userId;
    private SubscriptionPlan plan;
    private int urlsCreated;
    private int urlLimit;
    private int apiCallsToday;
    private int apiCallLimit;
    private long storageUsed;
    private long storageLimit;
    private long totalClicks;
    private long clicksToday;
    private long clicksThisMonth;
    private double urlUsagePercentage;
    private double apiUsagePercentage;
    private double storageUsagePercentage;
    
    public void calculatePercentages() {
        this.urlUsagePercentage = urlLimit > 0 ? Math.min((double) urlsCreated / urlLimit * 100, 100) : 0;
        this.apiUsagePercentage = apiCallLimit > 0 ? Math.min((double) apiCallsToday / apiCallLimit * 100, 100) : 0;
        this.storageUsagePercentage = storageLimit > 0 ? Math.min((double) storageUsed / storageLimit * 100, 100) : 0;
    }
    
    public String getUrlUsageDisplay() {
        if (urlLimit == Integer.MAX_VALUE) {
            return urlsCreated + " / Unlimited";
        }
        return urlsCreated + " / " + urlLimit;
    }
    
    public String getApiUsageDisplay() {
        if (apiCallLimit == Integer.MAX_VALUE) {
            return apiCallsToday + " / Unlimited";
        }
        return apiCallsToday + " / " + apiCallLimit;
    }
    
    public String getStorageUsageDisplay() {
        if (storageLimit == 1000L * 1024 * 1024 * 1024) { // 1 TB
            return String.format("%.2f MB / 1 TB", storageUsed / (1024.0 * 1024.0));
        }
        return String.format("%.2f MB / %.2f MB", 
            storageUsed / (1024.0 * 1024.0), 
            storageLimit / (1024.0 * 1024.0));
    }
}
