package org.stir.shrinkurl.service;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.stir.shrinkurl.entity.RefreshToken;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.repository.RefreshTokenRepository;
import org.stir.shrinkurl.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class RefreshTokenService {
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private UserRepository userRepository; // Your SQL repository
    
    public Optional<RefreshToken> validateRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByToken(token);
        
        if (refreshToken.isPresent()) {
            RefreshToken tokenEntity = refreshToken.get();
            
            // Check if token is expired (though MongoDB will auto-delete)
            if (tokenEntity.getExpiryDate().before(new Date())) {
                refreshTokenRepository.delete(tokenEntity);
                return Optional.empty();
            }
            
            // Verify user still exists in SQL database
            if (!userRepository.existsById(tokenEntity.getUserId())) {
                refreshTokenRepository.delete(tokenEntity);
                return Optional.empty();
            }
            
            return refreshToken;
        }
        
        return Optional.empty();
    }
    
    public RefreshToken createRefreshToken(User user) {
        // Delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUserId(user.getId());
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (30L * 24 * 60 * 60 * 1000)); // 30 days
        
        RefreshToken refreshToken = RefreshToken.builder()
            .token(UUID.randomUUID().toString())
            .userId(user.getId())
            .email(user.getEmail())
            .expiryDate(expiryDate)
            .createdAt(now)
            .build();
        
        return refreshTokenRepository.save(refreshToken);
    }
    
    public Optional<User> getUserFromRefreshToken(String token) {
        Optional<RefreshToken> refreshToken = validateRefreshToken(token);
        
        if (refreshToken.isPresent()) {
            Long userId = refreshToken.get().getUserId();
            return userRepository.findById(userId);
        }
        
        return Optional.empty();
    }
    
    public void deleteByUserId(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }
    
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }
}
