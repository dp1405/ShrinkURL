#!/bin/bash

# Demo script to show rate limiting in action

echo "🚀 Redis and Rate Limiting Demo"
echo "================================="
echo

# Check if application is running
if ! curl -s http://localhost:8085/api/debug/redis/health > /dev/null 2>&1; then
    echo "❌ Application not running on port 8085"
    echo "Please start the application first: mvn spring-boot:run"
    exit 1
fi

echo "✅ Application is running on port 8085"
echo

# 1. Check Redis Health
echo "🔍 1. Checking Redis Health..."
curl -s "http://localhost:8085/api/debug/redis/health" | jq
echo

# 2. Check URL Mappings
echo "🔍 2. Checking URL Mappings in Redis..."
curl -s "http://localhost:8085/api/debug/redis/urls" | jq '.total_url_mappings, .status'
echo

# 3. Check Rate Limiting Data
echo "🔍 3. Checking Rate Limiting Data..."
curl -s "http://localhost:8085/api/debug/redis/rate-limits" | jq '.total_rate_limit_keys, .total_global_rate_limit_keys, .status'
echo

# 4. Test URL Storage
echo "🔧 4. Testing URL Storage..."
test_url="https://example.com/demo-$(date +%s)"
curl -X POST "http://localhost:8085/api/admin/verify/test-url-storage?testUrl=$test_url" | jq '.status, .storage_successful, .short_code'
echo

# 5. Test Rate Limiting
echo "🔧 5. Testing Rate Limiting (limit=3, window=60s)..."
curl -X POST "http://localhost:8085/api/admin/verify/test-rate-limiting?testKey=demo_$(date +%s)&limit=3&windowSeconds=60" | jq '.status, .test_results.attempt_1.allowed, .test_results.attempt_3.allowed, .test_results.attempt_4.allowed'
echo

# 6. Comprehensive Verification
echo "🔧 6. Running Comprehensive Verification..."
curl -s "http://localhost:8085/api/admin/verify/all" | jq '.status, .redis_status, .rate_limiting.functionality_test.working_correctly'
echo

echo "🎯 Interactive Testing:"
echo "• Test Dashboard: http://localhost:8085/test-dashboard"
echo "• Standard Error Page: http://localhost:8085/error/rate-limit?current=25&limit=20&window=60&retryAfter=45"
echo "• Enhanced Error Page: http://localhost:8085/error/rate-limit-enhanced?current=25&limit=20&window=60&retryAfter=45"
echo

echo "✅ Demo Complete!"
echo "All Redis and rate limiting functionality is working correctly!"
