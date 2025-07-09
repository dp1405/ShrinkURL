package org.stir.shrinkurl.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    private String password;
    
    private boolean rememberMe = false;
}
