package org.stir.shrinkurl.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.stir.shrinkurl.dto.TokenPair;
import org.stir.shrinkurl.entity.RefreshToken;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.repository.RefreshTokenRepository;
import org.stir.shrinkurl.service.RefreshTokenService;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-validity:3600}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:3600}")
    private long refreshTokenValidity;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    private Algorithm algorithm;
    private JWTVerifier verifier;

    @PostConstruct
    public void init(){
        algorithm = Algorithm.HMAC256(jwtSecret);
        verifier = JWT.require(algorithm).build();
    }

    public TokenPair generateTokens(User user) {
        // Generate access token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("roles", user.getRoles()); // Assuming you have roles
        
        String accessToken = generateAccessToken(user.getEmail(), claims);
        
        // Create refresh token in MongoDB
        RefreshToken refreshTokenEntity = refreshTokenService.createRefreshToken(user);
        
        return new TokenPair(accessToken, refreshTokenEntity.getToken());
    }

    private String generateAccessToken(String email, Map<String, Object> claims){
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenValidity * 1000);

        JWTCreator.Builder tokenBuilder = JWT.create()
            .withSubject(email)
            .withIssuedAt(now)
            .withExpiresAt(expiryDate);

        if (claims != null) {
            for (Map.Entry<String, Object> entry : claims.entrySet()) {
                Object value = entry.getValue();
                String key = entry.getKey();
                
                if (value instanceof String) {
                    tokenBuilder.withClaim(key, (String) value);
                } else if (value instanceof Integer) {
                    tokenBuilder.withClaim(key, (Integer) value);
                } else if (value instanceof Long) {
                    tokenBuilder.withClaim(key, (Long) value);
                } else if (value instanceof Boolean) {
                    tokenBuilder.withClaim(key, (Boolean) value);
                } else if (value instanceof Date) {
                    tokenBuilder.withClaim(key, (Date) value);
                } else if (value instanceof List) {
                    tokenBuilder.withClaim(key, (List<?>) value);
                } else if (value instanceof Map) {
                    tokenBuilder.withClaim(key, (Map<String, ?>) value);
                }
            }
        }

        return tokenBuilder.sign(algorithm);
    }

    public Optional<DecodedJWT> validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                return Optional.empty();
            }
            
            DecodedJWT decodedJWT = verifier.verify(token);
                
            return Optional.of(decodedJWT);
        } catch (TokenExpiredException e) {
            log.debug("Token expired: {}", e.getMessage());
            return Optional.empty();
        } catch (JWTVerificationException e) {
            log.error("Token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
    
    public User getUserFromToken(String token) {
        return validateToken(token)
            .map(jwt -> User.builder()
                .id(Long.parseLong(jwt.getSubject()))
                .email(jwt.getClaim("email").asString())
                .name(jwt.getClaim("name").asString())
                .roles(jwt.getClaim("roles").asList(String.class))
                .build())
            .orElse(null);
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        return refreshTokenRepository.findByToken(token)
            .filter(rt -> rt.getExpiryDate().after(new Date()));
    }
}
