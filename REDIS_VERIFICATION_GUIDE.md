# Redis Storage & Rate Limiting Verification Guide

## 🔍 Overview
This guide provides comprehensive methods to verify that:
1. **URL mappings are properly stored in Redis**
2. **Rate limiting is functioning correctly**
3. **Error pages are displayed appropriately**

## 🛠️ Verification Methods

### Method 1: Automated Verification Script
```bash
# Run the comprehensive verification script
./verify-redis-rate-limiting.sh
```

### Method 2: Interactive Test Dashboard
Visit: `http://localhost:8085/test-dashboard`

Features:
- Real-time Redis connection testing
- URL mapping verification
- Rate limiting simulation
- Interactive debugging tools

### Method 3: API Endpoints for Verification

#### A. Redis Storage Verification
```bash
# Check all URL mappings in Redis
curl -s "http://localhost:8085/api/debug/redis/urls" | jq

# Check specific URL by short code
curl -s "http://localhost:8085/api/debug/redis/url/abc123" | jq

# Test URL storage
curl -X POST "http://localhost:8085/api/admin/verify/test-url-storage?testUrl=https://example.com"
```

#### B. Rate Limiting Verification
```bash
# Check current rate limiting data
curl -s "http://localhost:8085/api/debug/redis/rate-limits" | jq

# Test rate limiting functionality
curl -X POST "http://localhost:8085/api/admin/verify/test-rate-limiting?testKey=mytest&limit=3&windowSeconds=60"

# Check specific rate limit key
curl -s "http://localhost:8085/api/debug/redis/rate-limit/test_user" | jq
```

#### C. Comprehensive Verification
```bash
# Run all verification tests
curl -s "http://localhost:8085/api/admin/verify/all" | jq

# Get Redis statistics
curl -s "http://localhost:8085/api/admin/verify/redis-stats" | jq
```

### Method 4: Manual Testing

#### Testing URL Storage:
1. Create a shortened URL through the web interface
2. Check if it's stored in Redis:
   ```bash
   curl -s "http://localhost:8085/api/debug/redis/urls"
   ```
3. Verify the URL resolves correctly

#### Testing Rate Limiting:
1. Make multiple rapid requests to any rate-limited endpoint
2. Observe the 429 response after hitting the limit
3. Check rate limiting data:
   ```bash
   curl -s "http://localhost:8085/api/debug/redis/rate-limits"
   ```

## 🎯 Rate Limiting Error Pages

### Standard Error Page
- URL: `/error/rate-limit`
- Features: Basic countdown timer, rate limit info

### Enhanced Error Page
- URL: `/error/rate-limit-enhanced`
- Features: Progress bar, better UX, helpful tips

### Testing Error Pages
```bash
# View standard error page
curl "http://localhost:8085/error/rate-limit?current=25&limit=20&window=60&retryAfter=45"

# View enhanced error page
curl "http://localhost:8085/error/rate-limit-enhanced?current=25&limit=20&window=60&retryAfter=45"
```

## 📊 Understanding the Data

### Redis URL Storage Structure:
```json
{
  "status": "success",
  "redis_connection": "OK",
  "total_urls": 15,
  "sample_urls": {
    "url:abc123": {
      "original_url": "https://example.com",
      "ttl_seconds": 86400,
      "expires_in": "1 day"
    }
  }
}
```

### Rate Limiting Data Structure:
```json
{
  "status": "success",
  "total_rate_limit_keys": 8,
  "total_global_rate_limit_keys": 3,
  "rate_limits": {
    "rate_limit:user:123": {
      "current_count": "5",
      "ttl_seconds": 45,
      "resets_in": "45 seconds"
    }
  }
}
```

## 🔧 Troubleshooting

### Common Issues:

#### 1. Redis Connection Problems
```bash
# Check Redis status
curl -s "http://localhost:8085/api/debug/redis/health"

# Verify Redis is running
redis-cli ping
```

#### 2. Rate Limiting Not Working
```bash
# Test rate limiting functionality
curl -X POST "http://localhost:8085/api/admin/verify/test-rate-limiting"

# Check rate limiting configuration
curl -s "http://localhost:8085/api/debug/redis/rate-limits"
```

#### 3. URL Storage Issues
```bash
# Test URL storage directly
curl -X POST "http://localhost:8085/api/admin/verify/test-url-storage?testUrl=https://test.com"

# Check URL mappings
curl -s "http://localhost:8085/api/debug/redis/urls"
```

## 🚀 Advanced Testing

### Load Testing Rate Limiting
```bash
# Use the provided script for load testing
./test-rate-limiting.sh
```

### Custom Rate Limiting Tests
```bash
# Test with custom parameters
curl -X POST "http://localhost:8085/api/test/redis/rate-limit" \
  -d "testKey=custom_test&limit=5&windowSeconds=30"
```

### Monitoring Redis in Real-time
```bash
# Monitor Redis commands
redis-cli monitor

# Check Redis memory usage
redis-cli info memory
```

## 📈 Performance Metrics

### Expected Performance:
- **Redis lookup time**: < 1ms
- **URL redirection**: < 50ms
- **Rate limiting check**: < 5ms
- **Error page load**: < 100ms

### Monitoring Commands:
```bash
# Check application metrics
curl -s "http://localhost:8085/actuator/metrics/redis.commands.total"

# Monitor rate limiting
curl -s "http://localhost:8085/api/admin/verify/redis-stats"
```

## 🎨 Error Page Customization

### Configuration Options:
- Countdown timer duration
- Progress bar animations
- Custom messages
- Retry button behavior
- Auto-redirect settings

### Testing Different Scenarios:
```bash
# Long wait time
curl "http://localhost:8085/error/rate-limit-enhanced?retryAfter=300"

# High traffic scenario
curl "http://localhost:8085/error/rate-limit-enhanced?current=150&limit=100"
```

## 📝 Best Practices

1. **Regular Verification**: Run verification scripts daily
2. **Monitor Redis Memory**: Check Redis usage regularly
3. **Test Rate Limits**: Verify limits match business requirements
4. **User Experience**: Ensure error pages are helpful and informative
5. **Logging**: Monitor rate limiting events for abuse detection

## 🔗 Useful Links

- Test Dashboard: `http://localhost:8085/test-dashboard`
- Enhanced Error Page: `http://localhost:8085/error/rate-limit-enhanced`
- Redis Health Check: `http://localhost:8085/api/debug/redis/health`
- Comprehensive Verification: `http://localhost:8085/api/admin/verify/all`

---

**Note**: Make sure Redis is running and the application is started before running any verification tests.
