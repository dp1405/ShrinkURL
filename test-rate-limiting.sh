#!/bin/bash

# Rate Limiting Test Script
# This script tests the rate limiting functionality by making multiple requests

BASE_URL="http://localhost:8085"
TEST_URL="https://example.com/test-$(date +%s)"

echo "=== URL Shortener Rate Limiting Test ==="
echo "Base URL: $BASE_URL"
echo "Test URL: $TEST_URL"
echo ""

# Function to make a request and check response
make_request() {
    local request_num=$1
    echo "Request #$request_num:"
    
    # Make the request and capture response
    response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" -X POST \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "originalUrl=$TEST_URL" \
        "$BASE_URL/api/urls/shorten")
    
    # Extract HTTP status code
    http_status=$(echo "$response" | grep "HTTP_STATUS" | cut -d: -f2)
    response_body=$(echo "$response" | grep -v "HTTP_STATUS")
    
    echo "  Status: $http_status"
    echo "  Response: $response_body"
    echo ""
    
    # Check if rate limited
    if [ "$http_status" == "429" ]; then
        echo "  *** RATE LIMITED! ***"
        return 1
    fi
    
    return 0
}

# Test rate limiting by making multiple requests quickly
echo "Making multiple requests to test rate limiting..."
echo ""

for i in {1..15}; do
    if ! make_request $i; then
        echo "Rate limit hit at request #$i"
        break
    fi
    
    # Small delay to avoid overwhelming the server
    sleep 0.1
done

echo ""
echo "=== Testing Redis Debug Endpoints ==="

# Test Redis health
echo "Checking Redis health..."
curl -s "$BASE_URL/api/debug/redis/health" | python3 -m json.tool || echo "Health check failed"
echo ""

# Test Redis URL mappings
echo "Checking Redis URL mappings..."
curl -s "$BASE_URL/api/debug/redis/urls" | python3 -m json.tool || echo "URL mappings check failed"
echo ""

# Test Redis rate limits
echo "Checking Redis rate limits..."
curl -s "$BASE_URL/api/debug/redis/rate-limits" | python3 -m json.tool || echo "Rate limits check failed"
echo ""

echo "=== Test Complete ==="
echo "Visit $BASE_URL/test-dashboard for interactive testing"
echo "Visit $BASE_URL/error/rate-limit for error page demo"
