package org.stir.shrinkurl.service;

import org.springframework.stereotype.Service;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.dto.PlanComparisonDTO;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PlanComparisonService {
    
    public List<PlanComparisonDTO> getPlansForComparison(User user) {
        SubscriptionPlan currentPlan = user.getSubscriptionPlan();
        
        return Arrays.stream(SubscriptionPlan.values())
                .map(plan -> PlanComparisonDTO.builder()
                        .plan(plan)
                        .isCurrent(plan == currentPlan)
                        .isUpgrade(isUpgrade(currentPlan, plan))
                        .isDowngrade(isDowngrade(currentPlan, plan))
                        .canUpgrade(canUpgrade(currentPlan, plan))
                        .build())
                .collect(Collectors.toList());
    }
    
    private boolean isUpgrade(SubscriptionPlan current, SubscriptionPlan target) {
        return getOrder(target) > getOrder(current);
    }
    
    private boolean isDowngrade(SubscriptionPlan current, SubscriptionPlan target) {
        return getOrder(target) < getOrder(current);
    }
    
    private boolean canUpgrade(SubscriptionPlan current, SubscriptionPlan target) {
        return target != SubscriptionPlan.FREE && current != target;
    }
    
    private int getOrder(SubscriptionPlan plan) {
        switch (plan) {
            case FREE: return 0;
            case MONTHLY: return 1;
            case YEARLY: return 2;
            case LIFETIME: return 3;
            default: return 0;
        }
    }
    
    public String getUpgradeMessage(SubscriptionPlan from, SubscriptionPlan to) {
        if (from == SubscriptionPlan.FREE) {
            return "Upgrade to " + to.getDisplayName() + " to unlock premium features!";
        }
        
        if (to == SubscriptionPlan.YEARLY && from == SubscriptionPlan.MONTHLY) {
            return "Switch to yearly billing and save " + SubscriptionPlan.YEARLY.getSavingsPercentage() + "%!";
        }
        
        if (to == SubscriptionPlan.LIFETIME) {
            return "Get lifetime access - pay once, use forever!";
        }
        
        return "Upgrade to " + to.getDisplayName() + " for enhanced features!";
    }
    
    public List<String> getUpgradeHighlights(SubscriptionPlan from, SubscriptionPlan to) {
        List<String> highlights = new ArrayList<>();
        
        if (from == SubscriptionPlan.FREE) {
            highlights.add("Remove usage limits");
            highlights.add("Advanced analytics");
            highlights.add("Custom short codes");
            highlights.add("Priority support");
        }
        
        if (to == SubscriptionPlan.YEARLY && from == SubscriptionPlan.MONTHLY) {
            highlights.add("2 months free");
            highlights.add("Better value");
            highlights.add("Same great features");
        }
        
        if (to == SubscriptionPlan.LIFETIME) {
            highlights.add("Unlimited everything");
            highlights.add("No recurring payments");
            highlights.add("Best long-term value");
        }
        
        return highlights;
    }
}
