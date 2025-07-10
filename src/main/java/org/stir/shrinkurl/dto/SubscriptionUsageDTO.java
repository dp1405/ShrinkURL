package org.stir.shrinkurl.dto;

import org.stir.shrinkurl.enums.SubscriptionPlan;

import jakarta.annotation.PostConstruct;
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
    
    @PostConstruct
    public void calculatePercentages() {
        this.urlUsagePercentage = urlLimit > 0 ? (double) urlsCreated / urlLimit * 100 : 0;
        this.apiUsagePercentage = apiCallLimit > 0 ? (double) apiCallsToday / apiCallLimit * 100 : 0;
        this.storageUsagePercentage = storageLimit > 0 ? (double) storageUsed / storageLimit * 100 : 0;
    }
}
