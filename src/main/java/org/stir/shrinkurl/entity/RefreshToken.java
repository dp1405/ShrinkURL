package org.stir.shrinkurl.entity;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    private String id;

    @Indexed(unique = true)
    private String token;
    
    @Indexed
    private Long userId;
    
    private String email;
    
    @Indexed(expireAfterSeconds = 2592000) // Auto-delete after 30 days
    private Date expiryDate;
    
    private Date createdAt;
}
