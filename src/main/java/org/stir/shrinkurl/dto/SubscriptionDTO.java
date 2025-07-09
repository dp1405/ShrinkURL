package org.stir.shrinkurl.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.enums.SubscriptionStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionDTO {
    private Long id;
    private SubscriptionPlan plan;
    private SubscriptionStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime trialEndDate;
    private BigDecimal amount;
    private String currency;
    private boolean autoRenew;
    private boolean isActive;
    private long daysRemaining;
    
    public static SubscriptionDTO from(Subscription subscription) {
        return SubscriptionDTO.builder()
            .id(subscription.getId())
            .plan(subscription.getPlan())
            .status(subscription.getStatus())
            .startDate(subscription.getStartDate())
            .endDate(subscription.getEndDate())
            .trialEndDate(subscription.getTrialEndDate())
            .amount(subscription.getAmount())
            .currency(subscription.getCurrency())
            .autoRenew(subscription.isAutoRenew())
            .isActive(subscription.isActive())
            .daysRemaining(subscription.getDaysRemaining())
            .build();
    }
}
