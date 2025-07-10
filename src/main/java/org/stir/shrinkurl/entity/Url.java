package org.stir.shrinkurl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "urls")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Url {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "original_url", nullable = false, length = 2000)
    private String originalUrl;
    
    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "click_count", nullable = false)
    private Integer clickCount = 0;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @Column(name = "last_clicked_at")
    private LocalDateTime lastClickedAt;
}