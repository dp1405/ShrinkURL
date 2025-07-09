package org.stir.shrinkurl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.stir.shrinkurl.enums.SubscriptionPlan;
import org.stir.shrinkurl.enums.SubscriptionStatus;

@Entity
@Table(name = "subscriptions", indexes = {
    @Index(name = "idx_subscription_user", columnList = "user_id"),
    @Index(name = "idx_subscription_status", columnList = "status"),
    @Index(name = "idx_subscription_end_date", columnList = "end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"user"})
@EqualsAndHashCode(exclude = {"user"})
public class Subscription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan plan;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SubscriptionStatus status = SubscriptionStatus.PENDING;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "trial_end_date")
    private LocalDateTime trialEndDate;

    @Column(nullable = false, precision = 10, scale = 2)
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal amount;

    @Column(length = 3)
    @Builder.Default
    private String currency = "INR";

    @Column(name = "auto_renew", nullable = false)
    @Builder.Default
    private boolean autoRenew = true;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Helper methods
    public boolean isActive() {
        return status == SubscriptionStatus.ACTIVE && 
               LocalDateTime.now().isBefore(endDate);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(endDate);
    }

    public boolean isInTrial() {
        return trialEndDate != null && 
               LocalDateTime.now().isBefore(trialEndDate) &&
               status == SubscriptionStatus.TRIAL;
    }

    public long getDaysRemaining() {
        if (!isActive()) {
            return 0;
        }
        return ChronoUnit.DAYS.between(LocalDateTime.now(), endDate);
    }

    public void cancel(String reason) {
        this.status = SubscriptionStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
        this.autoRenew = false;
    }

    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
    }

    public void expire() {
        this.status = SubscriptionStatus.EXPIRED;
        this.autoRenew = false;
    }

    // Calculate end date based on plan
    public void calculateEndDate() {
        this.endDate = calculateEndDateFromPlan(this.startDate, this.plan);
    }

    private LocalDateTime calculateEndDateFromPlan(LocalDateTime start, SubscriptionPlan plan) {
        switch (plan) {
            case MONTHLY:
                return start.plusMonths(1);
            case YEARLY:
                return start.plusYears(1);
            case LIFETIME:
                return start.plusYears(100); // Effectively lifetime
            default:
                return start; // FREE plan has no end date
        }
    }
}