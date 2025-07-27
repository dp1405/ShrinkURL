package org.stir.shrinkurl.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.stir.shrinkurl.dto.CancelSubscriptionRequest;
import org.stir.shrinkurl.dto.SubscriptionUsageDTO;
import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.exceptions.TrialAlreadyUsedException;
import org.stir.shrinkurl.service.PlanComparisonService;
import org.stir.shrinkurl.service.SubscriptionService;
import org.stir.shrinkurl.util.PlanRecommendationUtil;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/subscription")
@Slf4j
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private PlanComparisonService planComparisonService;
    
    @Autowired
    private PlanRecommendationUtil planRecommendationUtil;
    
    @GetMapping("/plans")
    @PreAuthorize("isAuthenticated()")
    public String viewPlans(Model model, @AuthenticationPrincipal User user) {
        SubscriptionUsageDTO usage = subscriptionService.getUsageStats(user);
        
        model.addAttribute("currentPlan", user.getSubscriptionPlan());
        model.addAttribute("plans", SubscriptionPlan.values());
        model.addAttribute("planComparisons", planComparisonService.getPlansForComparison(user));
        model.addAttribute("usage", usage);
        model.addAttribute("hasUsedTrial", subscriptionService.hasUsedTrial(user));
        
        // Add recommendation data
        model.addAttribute("recommendedPlan", planRecommendationUtil.recommendPlan(user, usage));
        model.addAttribute("recommendationReason", planRecommendationUtil.getRecommendationReason(user, usage));
        model.addAttribute("usageInsights", planRecommendationUtil.getUsageInsights(usage));
        
        // Add upgrade messages for each plan
        for (SubscriptionPlan plan : SubscriptionPlan.values()) {
            if (plan != user.getSubscriptionPlan()) {
                model.addAttribute("upgradeMessage_" + plan.name(), 
                    planComparisonService.getUpgradeMessage(user.getSubscriptionPlan(), plan));
                model.addAttribute("upgradeHighlights_" + plan.name(), 
                    planComparisonService.getUpgradeHighlights(user.getSubscriptionPlan(), plan));
            }
        }
        
        return "subscription/plans";
    }
    
    @PostMapping("/upgrade")
    @PreAuthorize("isAuthenticated()")
    public String upgradeSubscription(@RequestParam("plan") SubscriptionPlan plan,
                                    @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        
        try {
            // Validate upgrade request
            if (plan == SubscriptionPlan.FREE) {
                redirectAttributes.addFlashAttribute("error", "Cannot downgrade to free plan. Please contact support.");
                return "redirect:/subscription/plans";
            }
            
            SubscriptionPlan currentPlan = user.getSubscriptionPlan();
            
            // Check if user is trying to downgrade
            if (isDowngrade(currentPlan, plan)) {
                redirectAttributes.addFlashAttribute("error", "Downgrades are not supported. Please contact support.");
                return "redirect:/subscription/plans";
            }
            
            // Check if user is already on this plan
            if (currentPlan == plan) {
                redirectAttributes.addFlashAttribute("info", "You are already on the " + plan.getDisplayName() + " plan.");
                return "redirect:/subscription/plans";
            }
            
            // Create pending subscription
            Subscription pendingSubscription = subscriptionService.createPendingSubscription(user, plan);
            
            // Add subscription details to flash attributes for payment page
            redirectAttributes.addFlashAttribute("planName", plan.getDisplayName());
            redirectAttributes.addFlashAttribute("planPrice", plan.getFormattedPrice());
            redirectAttributes.addFlashAttribute("currentPlan", currentPlan.getDisplayName());
            
            // Simulate payment gateway redirect
            String paymentUrl = "/subscription/payment/simulate?subscriptionId=" + pendingSubscription.getId();
            return "redirect:" + paymentUrl;
            
        } catch (Exception e) {
            log.error("Failed to initiate subscription upgrade for user: {}", user.getEmail(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to initiate upgrade. Please try again.");
            return "redirect:/subscription/plans";
        }
    }
    
    /**
     * Check if the plan change is a downgrade
     */
    private boolean isDowngrade(SubscriptionPlan current, SubscriptionPlan target) {
        // Define plan hierarchy: FREE < MONTHLY < YEARLY < LIFETIME
        int currentLevel = getPlanLevel(current);
        int targetLevel = getPlanLevel(target);
        return targetLevel < currentLevel;
    }
    
    /**
     * Get numeric level for plan comparison
     */
    private int getPlanLevel(SubscriptionPlan plan) {
        switch (plan) {
            case FREE: return 0;
            case MONTHLY: return 1;
            case YEARLY: return 2;
            case LIFETIME: return 3;
            default: return 0;
        }
    }
    
    @GetMapping("/payment/simulate")
    public String simulatePayment(@RequestParam("subscriptionId") Long subscriptionId, 
                                 Model model,
                                 @AuthenticationPrincipal User user) {
        try {
            // Get subscription details for display
            Subscription subscription = subscriptionService.getSubscriptionById(subscriptionId);
            
            if (subscription == null || !subscription.getUser().getId().equals(user.getId())) {
                throw new RuntimeException("Subscription not found or access denied");
            }
            
            model.addAttribute("subscriptionId", subscriptionId);
            model.addAttribute("planName", subscription.getPlan().getDisplayName());
            model.addAttribute("planPrice", subscription.getPlan().getFormattedPrice());
            model.addAttribute("currentPlan", user.getSubscriptionPlan().getDisplayName());
            
            return "subscription/payment-simulate";
        } catch (Exception e) {
            log.error("Error loading payment simulation page: {}", e.getMessage());
            model.addAttribute("subscriptionId", subscriptionId);
            return "subscription/payment-simulate";
        }
    }
    
    @PostMapping("/payment/callback")
    public String handlePaymentCallback(@RequestParam("subscriptionId") String subscriptionId,
                                      @RequestParam("status") String status,
                                      @RequestParam(value = "paymentId", required = false) String paymentId,
                                      @RequestParam(value = "transactionId", required = false) String transactionId,
                                      RedirectAttributes redirectAttributes) {
        
        if ("success".equals(status)) {
            try {
                subscriptionService.activateSubscription(subscriptionId, paymentId, transactionId);
                redirectAttributes.addFlashAttribute("success", "Subscription upgraded successfully!");
                return "redirect:/dashboard";
            } catch (Exception e) {
                log.error("Payment processing error", e);
                redirectAttributes.addFlashAttribute("error", "Payment processing failed");
                return "redirect:/subscription/plans";
            }
        } else {
            subscriptionService.handleFailedPayment(subscriptionId, "Payment cancelled or failed");
            redirectAttributes.addFlashAttribute("error", "Payment was not completed");
            return "redirect:/subscription/plans";
        }
    }
    
    @GetMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public String showCancelForm(Model model, @AuthenticationPrincipal User user) {
        if (user.getSubscriptionPlan() == SubscriptionPlan.FREE) {
            return "redirect:/subscription/plans";
        }
        
        model.addAttribute("subscription", user.getSubscription());
        model.addAttribute("cancelForm", new CancelSubscriptionRequest());
        return "subscription/cancel";
    }
    
    @PostMapping("/cancel")
    @PreAuthorize("isAuthenticated()")
    public String cancelSubscription(@ModelAttribute("cancelForm") CancelSubscriptionRequest request,
                                   @AuthenticationPrincipal User user,
                                   RedirectAttributes redirectAttributes) {
        
        try {
            String reason = request.getReason() + " - " + request.getFeedback();
            subscriptionService.cancelSubscription(user.getSubscription(), reason);
            redirectAttributes.addFlashAttribute("success", "Subscription cancelled successfully");
            return "redirect:/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to cancel subscription");
            return "redirect:/subscription/cancel";
        }
    }
    
    @PostMapping("/trial/activate")
    @PreAuthorize("isAuthenticated()")
    public String activateTrial(@AuthenticationPrincipal User user,
                               RedirectAttributes redirectAttributes) {
        try {
            // Check if user has already used trial
            if (subscriptionService.hasUsedTrial(user)) {
                redirectAttributes.addFlashAttribute("error", "You have already used your free trial");
                return "redirect:/subscription/plans";
            }
            
            // Check if user is already on a premium plan
            if (user.getSubscriptionPlan().isPremium()) {
                redirectAttributes.addFlashAttribute("info", "You are already on a premium plan");
                return "redirect:/subscription/plans";
            }
            
            subscriptionService.activateTrial(user);
            redirectAttributes.addFlashAttribute("success", 
                "7-day free trial activated successfully! You now have access to all premium features.");
            
            log.info("Trial activated successfully for user: {}", user.getEmail());
            return "redirect:/dashboard";
            
        } catch (TrialAlreadyUsedException e) {
            redirectAttributes.addFlashAttribute("error", "You have already used your free trial");
            log.warn("Trial activation failed - already used for user: {}", user.getEmail());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to activate trial. Please try again.");
            log.error("Failed to activate trial for user: {}", user.getEmail(), e);
        }
        return "redirect:/subscription/plans";
    }
}
