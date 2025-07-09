package org.stir.shrinkurl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UrlShortenRequest {
    
    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.*", message = "Please enter a valid URL starting with http:// or https://")
    private String originalUrl;
}