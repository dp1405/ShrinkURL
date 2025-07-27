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
public class PlanComparisonDTO {
    private SubscriptionPlan plan;
    private boolean isCurrent;
    private boolean isUpgrade;
    private boolean isDowngrade;
    private boolean canUpgrade;
    
    public String getUpgradeButtonText() {
        if (isCurrent) {
            return "Current Plan";
        }
        if (isUpgrade) {
            return "Upgrade";
        }
        if (isDowngrade) {
            return "Downgrade";
        }
        return "Select Plan";
    }
    
    public String getUpgradeButtonClass() {
        if (isCurrent) {
            return "btn btn-secondary";
        }
        if (plan.isPopular()) {
            return "btn btn-success";
        }
        return "btn btn-primary";
    }
}
