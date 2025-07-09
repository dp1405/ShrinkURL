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
    private double urlUsagePercentage;
    private double apiUsagePercentage;
    private double storageUsagePercentage;
    
    @PostConstruct
    public void calculatePercentages() {
        this.urlUsagePercentage = (double) urlsCreated / urlLimit * 100;
        this.apiUsagePercentage = (double) apiCallsToday / apiCallLimit * 100;
        this.storageUsagePercentage = (double) storageUsed / storageLimit * 100;
    }
}
