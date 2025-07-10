package org.stir.shrinkurl.utils;

import java.util.zip.CRC32;
import org.springframework.stereotype.Component;

@Component
public class UrlShortenerUtil {
    
    private static final String BASE62_CHARS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;
    private static final int MIN_LENGTH = 6;
    
    /**
     * Generate short code using CRC32 hash and Base62 encoding
     */
    public String generateShortCode(String originalUrl) {
        // Generate CRC32 hash
        CRC32 crc32 = new CRC32();
        crc32.update(originalUrl.getBytes());
        long hash = crc32.getValue();
        
        // Convert to positive value and encode to Base62
        String shortCode = encodeBase62(Math.abs(hash));
        
        // Ensure minimum length
        if (shortCode.length() < MIN_LENGTH) {
            shortCode = String.format("%0" + MIN_LENGTH + "d", 0) + shortCode;
            shortCode = shortCode.substring(shortCode.length() - MIN_LENGTH);
        }
        
        return shortCode;
    }
    
    /**
     * Generate custom short code with collision handling
     */
    public String generateShortCodeWithSalt(String originalUrl, int attempt) {
        String saltedUrl = originalUrl + "_" + attempt;
        return generateShortCode(saltedUrl);
    }
    
    /**
     * Encode number to Base62
     */
    private String encodeBase62(long num) {
        if (num == 0) return "0";
        
        StringBuilder sb = new StringBuilder();
        while (num > 0) {
            sb.insert(0, BASE62_CHARS.charAt((int) (num % BASE)));
            num /= BASE;
        }
        return sb.toString();
    }
    
    /**
     * Validate custom short code (for premium users)
     */
    public boolean isValidCustomCode(String customCode) {
        if (customCode == null || customCode.length() < 3 || customCode.length() > 20) {
            return false;
        }
        
        // Check if contains only alphanumeric characters
        return customCode.matches("^[a-zA-Z0-9]+$");
    }
}
