package org.stir.shrinkurl.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.stir.shrinkurl.entity.User;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {
    
    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public void sendWelcomeEmail(User user) {
        log.info("Sending welcome email to: {}", user.getEmail());
        // Email implementation would go here
    }
    
    public void sendVerificationEmail(User user) {
        String verificationToken = generateVerificationToken();
        String verificationLink = baseUrl + "/verify-email?token=" + verificationToken;
        
        log.info("Sending verification email to: {} with link: {}", user.getEmail(), verificationLink);
        // Save token to database and send email
    }
    
    private String generateVerificationToken() {
        return UUID.randomUUID().toString();
    }
}
