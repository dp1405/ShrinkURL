package org.stir.shrinkurl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelSubscriptionRequest {
    
    private String reason;
    
    @Size(max = 1000, message = "Feedback must not exceed 1000 characters")
    private String feedback;
}