// UpdateProfileRequest.java
package org.stir.shrinkurl.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    @Size(max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    @Size(max = 500, message = "Picture URL must not exceed 500 characters")
    private String picture;
}