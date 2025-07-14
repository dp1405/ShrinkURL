#!/bin/bash

# Comprehensive Redis and Rate Limiting Verification Script
# This script verifies both Redis storage and rate limiting functionality

BASE_URL="http://localhost:8085"
echo "========================================"
echo "🔍 REDIS & RATE LIMITING VERIFICATION"
echo "========================================"
echo "Base URL: $BASE_URL"
echo ""

# Function to make pretty JSON output
pretty_json() {
    if command -v jq &> /dev/null; then
        echo "$1" | jq .
    else
        echo "$1" | python3 -m json.tool 2>/dev/null || echo "$1"
    fi
}

# Function to check if application is running
check_app_status() {
    echo "🔍 Checking application status..."
    response=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null)
    if [ "$response" = "200" ]; then
        echo "✅ Application is running"
        return 0
    else
        echo "❌ Application is not running. Please start it first."
        return 1
    fi
}

# Function to check Redis connection
check_redis_connection() {
    echo ""
    echo "🔍 Checking Redis connection..."
    response=$(curl -s "$BASE_URL/api/debug/redis/health" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "✅ Redis connection check:"
        pretty_json "$response"
    else
        echo "❌ Failed to check Redis connection"
    fi
}

# Function to check URL mappings in Redis
check_url_mappings() {
    echo ""
    echo "🔍 Checking URL mappings in Redis..."
    response=$(curl -s "$BASE_URL/api/debug/redis/urls" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "✅ URL mappings in Redis:"
        pretty_json "$response"
    else
        echo "❌ Failed to check URL mappings"
    fi
}

# Function to check rate limiting data
check_rate_limiting_data() {
    echo ""
    echo "🔍 Checking rate limiting data in Redis..."
    response=$(curl -s "$BASE_URL/api/debug/redis/rate-limits" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "✅ Rate limiting data in Redis:"
        pretty_json "$response"
    else
        echo "❌ Failed to check rate limiting data"
    fi
}

# Function to test rate limiting by making multiple requests
test_rate_limiting() {
    echo ""
    echo "🔍 Testing rate limiting functionality..."
    echo "Making 10 quick requests to test rate limiting..."
    
    rate_limited=false
    for i in {1..10}; do
        echo -n "Request $i: "
        
        # Make request with timeout
        response=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST \
            -H "Content-Type: application/json" \
            -d '{"originalUrl":"https://example.com/test-'$i'"}' \
            "$BASE_URL/api/demo/test-shortening" 2>/dev/null)
        
        # Extract HTTP status code
        if echo "$response" | grep -q "HTTP_CODE:"; then
            http_code=$(echo "$response" | grep "HTTP_CODE:" | cut -d: -f2)
            response_body=$(echo "$response" | sed '/HTTP_CODE:/d')
            
            if [ "$http_code" = "429" ]; then
                echo "❌ Rate limited! (HTTP $http_code)"
                rate_limited=true
                break
            else
                echo "✅ Success (HTTP $http_code)"
            fi
        else
            echo "❌ No response"
        fi
        
        # Small delay
        sleep 0.1
    done
    
    if [ "$rate_limited" = true ]; then
        echo "✅ Rate limiting is working correctly!"
    else
        echo "⚠️  Rate limiting not triggered in this test"
    fi
}

# Function to test specific URL in Redis
test_specific_url() {
    echo ""
    echo "🔍 Testing specific URL lookup in Redis..."
    
    # First, let's see what URLs are available
    echo "Available short codes in Redis:"
    curl -s "$BASE_URL/api/debug/redis/urls" | grep -o '"[^"]*"' | head -5
    
    # Test a specific short code (we'll use a test one)
    test_code="test123"
    echo ""
    echo "Testing short code: $test_code"
    response=$(curl -s "$BASE_URL/api/debug/redis/url/$test_code" 2>/dev/null)
    if [ $? -eq 0 ]; then
        echo "✅ URL lookup result:"
        pretty_json "$response"
    else
        echo "❌ Failed to lookup URL"
    fi
}

# Function to show test dashboard info
show_dashboard_info() {
    echo ""
    echo "🎯 Interactive Testing Options:"
    echo "1. Test Dashboard: $BASE_URL/test-dashboard"
    echo "2. Rate Limit Error Page: $BASE_URL/error/rate-limit?current=25&limit=20&window=60&retryAfter=45"
    echo "3. Redis Debug URLs:"
    echo "   - Health: $BASE_URL/api/debug/redis/health"
    echo "   - URLs: $BASE_URL/api/debug/redis/urls"
    echo "   - Rate Limits: $BASE_URL/api/debug/redis/rate-limits"
    echo "4. Test Endpoints:"
    echo "   - Rate Limit Test: $BASE_URL/api/test/redis/rate-limit"
    echo "   - URL Mappings: $BASE_URL/api/test/redis/url-mappings/all"
}

# Main execution
main() {
    if ! check_app_status; then
        echo ""
        echo "Please start the application first:"
        echo "cd /home/drpn14/work/SpringBoot/shrinkurl && mvn spring-boot:run"
        exit 1
    fi
    
    check_redis_connection
    check_url_mappings
    check_rate_limiting_data
    test_rate_limiting
    test_specific_url
    show_dashboard_info
    
    echo ""
    echo "========================================"
    echo "✅ VERIFICATION COMPLETE"
    echo "========================================"
}

# Run main function
main
