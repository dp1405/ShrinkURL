package org.stir.shrinkurl.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.stir.shrinkurl.enums.SubscriptionPlan;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_provider", columnList = "provider,provider_id")
})
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(exclude = {"subscription"}) // Prevent circular reference
@EqualsAndHashCode(exclude = {"subscription"})
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @Column(length = 100)
    private String password; // Null for OAuth2 users

    @Column(nullable = false, length = 100)
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;

    @Column(length = 500)
    private String picture;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role")
    @Builder.Default
    private List<String> roles = new ArrayList<>();

    @Column(length = 50)
    private String provider; // google, facebook, github, etc.

    @Column(name = "provider_id", length = 100)
    private String providerId; // ID from OAuth2 provider

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Subscription subscription;

    // Helper methods
    public boolean hasActiveSubscription() {
        return subscription != null && subscription.isActive();
    }

    public boolean isPremiumUser() {
        return hasActiveSubscription() && 
               subscription.getPlan() != SubscriptionPlan.FREE;
    }

    public SubscriptionPlan getSubscriptionPlan() {
        if (subscription == null || !subscription.isActive()) {
            return SubscriptionPlan.FREE;
        }
        return subscription.getPlan();
    }

    public void addRole(String role) {
        if (this.roles == null) {
            this.roles = new ArrayList<>();
        }
        if (!this.roles.contains(role)) {
            this.roles.add(role);
        }
    }

    public void removeRole(String role) {
        if (this.roles != null) {
            this.roles.remove(role);
        }
    }
}