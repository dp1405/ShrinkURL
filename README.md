# URL Shortener Implementation

This is a comprehensive URL shortening service implementation with the following features:

## Features

### 1. URL Shortening with CRC32 Hashing
- Uses CRC32 hash of the original URL converted to Base62
- Handles collision detection with salt-based retry mechanism
- Minimum 6-character short codes for better uniqueness

### 2. Redis Caching with Database Fallback
- **Redis DB 0**: URL mappings for fast resolution
- **Redis DB 1**: Rate limiting data
- 1-second timeout for Redis operations with automatic database fallback
- TTL-based expiration for cached URLs

### 3. Analytics System
- Tracks total clicks, daily clicks, and monthly clicks
- Asynchronous click tracking to avoid blocking redirections
- Premium users get detailed analytics

### 4. Subscription-based Features
- **FREE**: 10 URLs, 30-day expiration, basic analytics
- **MONTHLY**: 1000 URLs, 365-day expiration, custom URLs, detailed analytics
- **YEARLY**: 10,000 URLs, no expiration, custom URLs, detailed analytics
- **LIFETIME**: Unlimited URLs, no expiration, all premium features

### 5. Two-Level Rate Limiting
- **Global Rate Limiting**: Applied via filter to all requests (IP-based)
- **Endpoint-Specific Rate Limiting**: Applied via `@RateLimit` annotation with AOP

### 6. Premium Features
- Custom short codes (alphanumeric, 3-20 characters)
- Longer URL expiration periods
- Detailed analytics with click tracking
- Higher rate limits

## Architecture

### Key Components

1. **UrlService**: Core URL shortening logic
2. **SubscriptionService**: Manages user subscriptions and usage limits
3. **RateLimitService**: Handles both global and endpoint-specific rate limiting
4. **UrlShortenerUtil**: CRC32 hashing and Base62 encoding
5. **GlobalRateLimitFilter**: Global rate limiting filter
6. **RateLimitAspect**: AOP-based endpoint rate limiting

### Database Schema

- `users`: User information with subscription relationship
- `urls`: URL mappings with click counts and expiration
- `url_analytics`: Daily click analytics per URL
- `subscriptions`: User subscription details

### Redis Usage

- **DB 0**: URL mappings (`url:{shortCode}` → `originalUrl`)
- **DB 1**: Rate limiting counters

## API Endpoints

### URL Management
- `POST /api/urls/shorten` - Create shortened URL
- `GET /api/urls/my-urls` - Get user's URLs
- `GET /api/urls/{id}` - Get specific URL details
- `GET /api/urls/{id}/analytics` - Get URL analytics

### Redirection
- `GET /{shortCode}` - Redirect to original URL
- `GET /api/resolve/{shortCode}` - Get original URL without redirecting

### Analytics
- `GET /api/analytics/usage` - Get usage statistics
- `GET /api/analytics/detailed` - Get detailed analytics (premium)

## Rate Limiting

### Global Rate Limiting
- Applied to all requests via `GlobalRateLimitFilter`
- Configurable per IP address
- Default: 1000 requests per minute

### Endpoint-Specific Rate Limiting
- Applied via `@RateLimit` annotation
- Configurable per endpoint
- Can be per-user or per-IP

## Configuration

Key application properties:
```properties
# Redis Configuration
spring.redis.host=localhost
spring.redis.port=6379
spring.redis.timeout=1000ms

# Rate Limiting
rate-limit.global.enabled=true
rate-limit.global.requests=1000
rate-limit.global.window=60

# Subscription
subscription.trial.days=7
```

## Error Handling

- Redis timeout handling with database fallback
- Collision detection and retry mechanism
- Subscription limit enforcement
- Rate limit exceeded responses with proper headers

## Security Features

- JWT-based authentication
- Role-based access control
- Rate limiting to prevent abuse
- Input validation and sanitization

## Future Enhancements

- Batch URL creation
- URL expiration notifications
- Geographic click analytics
- A/B testing for short codes
- API key management for enterprise users
