package org.stir.shrinkurl.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {
    
    /**
     * Number of requests allowed per time window
     */
    int value() default 100;
    
    /**
     * Time window in seconds
     */
    int timeWindow() default 60;
    
    /**
     * Rate limit key expression (SpEL)
     * Default uses user ID if available, otherwise IP address
     */
    String key() default "";
    
    /**
     * Message to return when rate limit is exceeded
     */
    String message() default "Rate limit exceeded";
    
    /**
     * Whether to apply rate limiting per user or globally
     */
    boolean perUser() default true;
}
