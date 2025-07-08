package org.stir.shrinkurl.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JWTUtil {

    @Value("$(jwt-token.secret)")
    private String secret_key;

    private SecretKey getSigningKey(){
        return Keys.hmacShaKeyFor(secret_key.getBytes());
    }

    private Claims extractAllClaims(String token){
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }

    private Date extractExpiration(String token){
        return extractAllClaims(token).getExpiration();
    }

    private boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public boolean validateToken(String token){
        return !isTokenExpired(token);
    }

    public String getUsername(String token){
        return extractAllClaims(token).getSubject();
    }

    public String generateToken(String subject, Map<String, Object> claims){
        return Jwts
            .builder()
            .claims(claims)
            .subject(subject)
            .header()
            .empty()
            .add("typ", "JWT")
            .and()
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60)).signWith(getSigningKey()).compact();
    }

    public String generateToken(String subject){
        Map<String, Object> claims = new HashMap<>();

        return generateToken(subject, claims);
    }
}
