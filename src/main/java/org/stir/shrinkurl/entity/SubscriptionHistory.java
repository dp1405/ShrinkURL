package org.stir.shrinkurl.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.stir.shrinkurl.enums.SubscriptionPlan;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(nullable = false)
    private String action; // CREATED, UPGRADED, DOWNGRADED, CANCELLED, EXPIRED, RENEWED
    
    @Column(name = "old_plan")
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan oldPlan;
    
    @Column(name = "new_plan")
    @Enumerated(EnumType.STRING)
    private SubscriptionPlan newPlan;
    
    @Column(name = "had_trial")
    private boolean hadTrial;
    
    private BigDecimal amount;
    
    private String currency;
    
    @Column(length = 500)
    private String notes;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
