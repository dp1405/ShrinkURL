package org.stir.shrinkurl.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private Long userId;
    private String email;
    private String name;
    private List<String> roles;
    private String accessToken;
    private String refreshToken;
}
