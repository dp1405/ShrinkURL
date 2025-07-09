package org.stir.shrinkurl.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.stir.shrinkurl.entity.Subscription;
import org.stir.shrinkurl.enums.SubscriptionStatus;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long>{
    @Query("SELECT s FROM Subscription s WHERE s.user.id = :userId AND s.status = 'ACTIVE'")
    Optional<Subscription> findActiveByUserId(@Param("userId") Long userId);
    
    @Query("SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Subscription s WHERE s.user.id = :userId AND s.status = :status")
    boolean existsByUserIdAndStatus(@Param("userId") Long userId, @Param("status") SubscriptionStatus status);
}
