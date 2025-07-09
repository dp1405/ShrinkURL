package org.stir.shrinkurl.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.stir.shrinkurl.entity.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String email;
    private String name;
    private String picture;
    private List<String> roles;
    private boolean emailVerified;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private SubscriptionDTO subscription;
    private String provider;
    
    public static UserDTO from(User user) {
        UserDTO dto = UserDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .name(user.getName())
            .picture(user.getPicture())
            .roles(user.getRoles())
            .emailVerified(user.isEmailVerified())
            .active(user.isActive())
            .createdAt(user.getCreatedAt())
            .lastLoginAt(user.getLastLoginAt())
            .provider(user.getProvider())
            .build();
        
        if (user.getSubscription() != null) {
            dto.setSubscription(SubscriptionDTO.from(user.getSubscription()));
        }
        
        return dto;
    }
}
