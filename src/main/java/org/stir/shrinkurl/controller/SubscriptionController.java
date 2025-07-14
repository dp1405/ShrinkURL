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
import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.exceptions.TrialAlreadyUsedException;
import org.stir.shrinkurl.service.SubscriptionService;

import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/subscription")
@Slf4j
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @GetMapping("/plans")
    @PreAuthorize("isAuthenticated()")
    public String viewPlans(Model model, @AuthenticationPrincipal User user) {
        model.addAttribute("currentPlan", user.getSubscriptionPlan());
        model.addAttribute("plans", SubscriptionPlan.values());
        model.addAttribute("usage", subscriptionService.getUsageStats(user));
        model.addAttribute("hasUsedTrial", subscriptionService.hasUsedTrial(user));
        return "subscription/plans";
    }
    
    @PostMapping("/upgrade")
    @PreAuthorize("isAuthenticated()")
    public String upgradeSubscription(@RequestParam("plan") SubscriptionPlan plan,
                                    @AuthenticationPrincipal User user,
                                    RedirectAttributes redirectAttributes) {
        
        try {
            Subscription pendingSubscription = subscriptionService.createPendingSubscription(user, plan);
            
            // Simulate payment gateway redirect
            String paymentUrl = "/subscription/payment/simulate?subscriptionId=" + pendingSubscription.getId();
            return "redirect:" + paymentUrl;
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to initiate upgrade");
            return "redirect:/subscription/plans";
        }
    }
    
    @GetMapping("/payment/simulate")
    public String simulatePayment(@RequestParam("subscriptionId") Long subscriptionId, Model model) {
        model.addAttribute("subscriptionId", subscriptionId);
        return "subscription/payment-simulate";
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
            subscriptionService.activateTrial(user);
            user.addRole("PREMIUM");
            redirectAttributes.addFlashAttribute("success", "Trial activated successfully!");
            return "redirect:/dashboard";
        } catch (TrialAlreadyUsedException e) {
            redirectAttributes.addFlashAttribute("error", "You have already used your free trial");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to activate trial");
        }
        return "redirect:/subscription/plans";
    }
}
