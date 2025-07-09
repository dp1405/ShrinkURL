package org.stir.shrinkurl.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.stir.shrinkurl.entity.SubscriptionHistory;

@Repository
public interface SubscriptionHistoryRepository extends JpaRepository<SubscriptionHistory, Long> {
    @Query("SELECT CASE WHEN COUNT(h) > 0 THEN true ELSE false END FROM SubscriptionHistory h WHERE h.userId = :userId AND h.hadTrial = true")
    boolean existsByUserAndHadTrial(@Param("userId") Long userId);
    
    List<SubscriptionHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
}
