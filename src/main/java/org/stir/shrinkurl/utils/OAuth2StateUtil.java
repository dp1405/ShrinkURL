package org.stir.shrinkurl.utils;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class OAuth2StateUtil {
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder base64Encoder = Base64.getUrlEncoder();

    public String generateState(){
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        return base64Encoder.encodeToString(randomBytes);
    }
}
