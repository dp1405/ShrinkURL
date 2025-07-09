package org.stir.shrinkurl.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.stir.shrinkurl.dto.GoogleUserInfo;
import org.stir.shrinkurl.dto.RegisterRequest;
import org.stir.shrinkurl.dto.UpdateProfileRequest;
import org.stir.shrinkurl.dto.UserDTO;
import org.stir.shrinkurl.entity.User;
import org.stir.shrinkurl.exceptions.AccountDisabledException;
import org.stir.shrinkurl.exceptions.EmailAlreadyExistsException;
import org.stir.shrinkurl.exceptions.UserNotFoundException;
import org.stir.shrinkurl.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    // OAuth2 login
    public User processOAuth2User(GoogleUserInfo googleUserInfo) {
        Optional<User> existingUser = userRepository.findByEmail(googleUserInfo.getEmail());
        
        if (existingUser.isPresent()) {
            return updateExistingUser(existingUser.get(), googleUserInfo);
        } else {
            return createNewUser(googleUserInfo);
        }
    }
    
    private User updateExistingUser(User user, GoogleUserInfo googleUserInfo) {
        user.setName(googleUserInfo.getName());
        user.setPicture(googleUserInfo.getPicture());
        user.setLastLoginAt(LocalDateTime.now());
        
        if (user.getProvider() == null) {
            user.setProvider("google");
            user.setProviderId(googleUserInfo.getId());
            user.setEmailVerified(googleUserInfo.isVerifiedEmail());
        }
        
        return userRepository.save(user);
    }
    
    private User createNewUser(GoogleUserInfo googleUserInfo) {
        User user = User.builder()
            .email(googleUserInfo.getEmail())
            .name(googleUserInfo.getName())
            .picture(googleUserInfo.getPicture())
            .provider("google")
            .providerId(googleUserInfo.getId())
            .roles(Arrays.asList("USER"))
            .emailVerified(googleUserInfo.isVerifiedEmail())
            .lastLoginAt(LocalDateTime.now())
            .active(true)
            .build();
        
        user = userRepository.save(user);
        subscriptionService.initializeFreeSubscription(user);
        
        return user;
    }
    
    // For form-based login
    public User authenticateUser(String email, String password) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        
        if (user.getPassword() == null) {
            throw new BadCredentialsException("Please login with " + user.getProvider());
        }
        
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        
        if (!user.isActive()) {
            throw new AccountDisabledException("Account is disabled");
        }
        
        user.setLastLoginAt(LocalDateTime.now());
        return userRepository.save(user);
    }
    
    // For form-based registration
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered");
        }
        
        User user = User.builder()
            .email(request.getEmail())
            .name(request.getName())
            .password(passwordEncoder.encode(request.getPassword()))
            .roles(Arrays.asList("USER"))
            .active(true)
            .emailVerified(false)
            .build();
        
        user = userRepository.save(user);
        subscriptionService.initializeFreeSubscription(user);
        
        return user;
    }
    
    // Get user with subscription info
    @Transactional(readOnly = true)
    public UserDTO getUserWithSubscription(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        
        return UserDTO.from(user);
    }
    
    // Update user profile
    public User updateUserProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found"));
        
        if (request.getName() != null && !request.getName().isEmpty()) {
            user.setName(request.getName());
        }
        
        if (request.getPicture() != null && !request.getPicture().isEmpty()) {
            user.setPicture(request.getPicture());
        }
        
        return userRepository.save(user);
    } 
}
