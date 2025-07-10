package org.stir.shrinkurl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "url_analytics", indexes = {
    @Index(name = "idx_analytics_url", columnList = "url_id"),
    @Index(name = "idx_analytics_date", columnList = "click_date"),
    @Index(name = "idx_analytics_url_date", columnList = "url_id, click_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UrlAnalytics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "url_id", nullable = false)
    private Long urlId;
    
    @Column(name = "click_date", nullable = false)
    private LocalDate clickDate;
    
    @Column(name = "click_count", nullable = false)
    @Builder.Default
    private Integer clickCount = 0;
    
    @Column(name = "created_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    @CreationTimestamp
    private LocalDateTime updatedAt;
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
