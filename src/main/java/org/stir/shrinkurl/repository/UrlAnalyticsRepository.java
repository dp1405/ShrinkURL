package org.stir.shrinkurl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.stir.shrinkurl.entity.UrlAnalytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UrlAnalyticsRepository extends JpaRepository<UrlAnalytics, Long> {
    
    // Find analytics for a specific URL and date
    Optional<UrlAnalytics> findByUrlIdAndClickDate(Long urlId, LocalDate clickDate);
    
    // Get analytics for a URL over a date range
    List<UrlAnalytics> findByUrlIdAndClickDateBetween(Long urlId, LocalDate startDate, LocalDate endDate);
    
    // Get total clicks for a URL
    @Query("SELECT SUM(ua.clickCount) FROM UrlAnalytics ua WHERE ua.urlId = :urlId")
    Long getTotalClicksByUrlId(@Param("urlId") Long urlId);
    
    // Get clicks for last N days
    @Query("SELECT ua FROM UrlAnalytics ua WHERE ua.urlId = :urlId AND ua.clickDate >= :startDate ORDER BY ua.clickDate DESC")
    List<UrlAnalytics> getClicksForLastNDays(@Param("urlId") Long urlId, @Param("startDate") LocalDate startDate);
    
    // Increment click count
    @Modifying
    @Query("UPDATE UrlAnalytics ua SET ua.clickCount = ua.clickCount + 1, ua.updatedAt = CURRENT_TIMESTAMP WHERE ua.urlId = :urlId AND ua.clickDate = :clickDate")
    int incrementClickCount(@Param("urlId") Long urlId, @Param("clickDate") LocalDate clickDate);
    
    // Get total clicks for user URLs
    @Query("SELECT SUM(ua.clickCount) FROM UrlAnalytics ua JOIN Url u ON ua.urlId = u.id WHERE u.userId = :userId")
    Long getTotalClicksByUserId(@Param("userId") Long userId);
    
    // Get clicks for user URLs in date range
    @Query("SELECT SUM(ua.clickCount) FROM UrlAnalytics ua JOIN Url u ON ua.urlId = u.id WHERE u.userId = :userId AND ua.clickDate BETWEEN :startDate AND :endDate")
    Long getClicksByUserIdAndDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    // Get clicks for user in date range
    @Query("SELECT ua FROM UrlAnalytics ua JOIN Url u ON ua.urlId = u.id WHERE u.userId = :userId AND ua.clickDate >= :startDate ORDER BY ua.clickDate DESC")
    List<UrlAnalytics> getClicksForUserInDateRange(@Param("userId") Long userId, @Param("startDate") LocalDate startDate);
    
    // Get analytics for user's URLs
    @Query("SELECT ua FROM UrlAnalytics ua JOIN Url u ON ua.urlId = u.id WHERE u.userId = :userId ORDER BY ua.clickDate DESC")
    List<UrlAnalytics> getAnalyticsByUserId(@Param("userId") Long userId);
}
