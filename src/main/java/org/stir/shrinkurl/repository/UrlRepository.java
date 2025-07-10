package org.stir.shrinkurl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.stir.shrinkurl.entity.Url;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {
    
    // Find by short code (most important for redirection)
    Optional<Url> findByShortCode(String shortCode);
    
    // Find by short code and active status
    Optional<Url> findByShortCodeAndIsActive(String shortCode, Boolean isActive);
    
    // Find all URLs by user ID
    Page<Url> findByUserId(Long userId, Pageable pageable);
    
    // Find active URLs by user ID
    Page<Url> findByUserIdAndIsActive(Long userId, Boolean isActive, Pageable pageable);
    
    // Find URLs by user ID ordered by creation date
    List<Url> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    // Find active URLs by user ID ordered by creation date
    List<Url> findByUserIdAndIsActiveOrderByCreatedAtDesc(Long userId, Boolean isActive);
    
    // Count total URLs by user
    long countByUserId(Long userId);
    
    // Count active URLs by user
    long countByUserIdAndIsActive(Long userId, Boolean isActive);
    
    // Find expired URLs
    @Query("SELECT u FROM Url u WHERE u.expiresAt <= :currentTime AND u.isActive = true")
    List<Url> findExpiredUrls(@Param("currentTime") LocalDateTime currentTime);
    
    // Update click count
    @Modifying
    @Query("UPDATE Url u SET u.clickCount = u.clickCount + 1, u.lastClickedAt = :clickTime WHERE u.id = :id")
    void incrementClickCount(@Param("id") Long id, @Param("clickTime") LocalDateTime clickTime);
    
    // Check if short code exists
    boolean existsByShortCode(String shortCode);
    
    // Get top clicked URLs by user
    @Query("SELECT u FROM Url u WHERE u.userId = :userId AND u.isActive = true ORDER BY u.clickCount DESC")
    Page<Url> findTopClickedByUser(@Param("userId") Long userId, Pageable pageable);
    
    // Get recent URLs by user
    @Query("SELECT u FROM Url u WHERE u.userId = :userId ORDER BY u.createdAt DESC")
    Page<Url> findRecentByUser(@Param("userId") Long userId, Pageable pageable);
    
    // Find URL by ID and user ID
    Optional<Url> findByIdAndUserId(Long id, Long userId);
}