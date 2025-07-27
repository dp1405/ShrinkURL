# ShrinkURL - Advanced URL Shortener Service

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A feature-rich, enterprise-grade URL shortening service built with Spring Boot 3.5.3. ShrinkURL provides comprehensive URL management, advanced analytics, subscription-based rate limiting, and a modern responsive web interface.

## 🚀 Features

### Core Functionality
- **URL Shortening**: Transform long URLs into short, memorable links
- **Custom Short Codes**: Create personalized short URLs (Premium feature)
- **QR Code Generation**: Generate QR codes for shortened URLs
- **Bulk URL Operations**: Process multiple URLs simultaneously
- **URL Expiration**: Configurable URL retention based on subscription plans

### Analytics & Monitoring
- **Real-time Analytics**: Track click counts, geographic data, and referrer information
- **Interactive Charts**: Chart.js-powered visualization of click trends
- **Daily Analytics**: Detailed breakdown of URL performance over time
- **User Dashboard**: Comprehensive overview of URL statistics and usage

### Authentication & Security
- **OAuth2 Integration**: Google OAuth2 authentication
- **JWT Token Management**: Secure API access with refresh tokens
- **Form-based Authentication**: Traditional email/password login
- **Role-based Access Control**: User and admin role management
- **Rate Limiting**: Global and endpoint-specific rate limiting with Redis

### Subscription Management
- **Multi-tier Plans**: FREE, MONTHLY, YEARLY, and LIFETIME subscriptions
- **Usage Tracking**: Monitor URL creation, API calls, and storage usage
- **Trial System**: 7-day free trial for premium features
- **Upgrade/Downgrade**: Seamless plan transitions with validation
- **Payment Simulation**: Built-in payment gateway simulation

### Performance & Scalability
- **Redis Caching**: High-performance URL resolution with Redis
- **Database Optimization**: Indexed queries and efficient data structures
- **Async Processing**: Background processing for analytics and notifications
- **Connection Pooling**: Optimized database connections

## 🏗️ Technical Architecture

### Technology Stack
- **Backend**: Spring Boot 3.5.3, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, Bootstrap 5, Chart.js, Vanilla JavaScript
- **Database**: MySQL (primary), MongoDB (analytics), Redis (caching/rate limiting)
- **Authentication**: OAuth2, JWT, Spring Security
- **Build Tool**: Maven
- **Java Version**: 17

### Database Schema
```
users
├── id (Primary Key)
├── email (Unique)
├── password (Nullable for OAuth2)
├── name
├── picture
├── roles (Collection)
├── provider (OAuth2)
├── provider_id
└── subscription (OneToOne)

urls
├── id (Primary Key)
├── original_url
├── short_code (Unique)
├── user_id (Foreign Key)
├── click_count
├── created_at
├── expires_at
└── is_active

subscriptions
├── id (Primary Key)
├── user_id (Foreign Key, Unique)
├── plan (Enum)
├── status (Enum)
├── start_date
├── end_date
├── amount
└── auto_renew

url_analytics
├── id (Primary Key)
├── url_id (Foreign Key)
├── date
├── click_count
├── unique_visitors
└── geographic_data
```

### Redis Structure
- **DB 0**: URL mappings (`url:{shortCode}` → `originalUrl`)
- **DB 1**: Rate limiting counters (`rate_limit:{key}`, `global_rate_limit:{ip}`)

## 📦 Installation & Setup

### Prerequisites
- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- Redis 6.0+
- MongoDB 4.4+ (optional, for enhanced analytics)

### Environment Setup

1. **Clone the repository**
```bash
git clone https://github.com/your-username/shrinkurl.git
cd shrinkurl
```

2. **Database Configuration**
```sql
-- Create MySQL database
CREATE DATABASE ShrinkURL;
CREATE USER 'your_username'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON ShrinkURL.* TO 'your_username'@'localhost';
FLUSH PRIVILEGES;
```

3. **Configuration Files**

Update `src/main/resources/application.yml`:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ShrinkURL?useSSL=false&serverTimezone=UTC
    username: your_username
    password: your_password
  
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: your-google-client-id
            client-secret: your-google-client-secret

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      
  data:
    mongodb:
      uri: mongodb://localhost:27017/shrinkurl_analytics

server:
  port: 8085

url-shortener:
  base-url: http://localhost:8085
  
subscription:
  trial:
    days: 7
```

4. **Google OAuth2 Setup**
- Go to [Google Cloud Console](https://console.cloud.google.com/)
- Create a new project or select existing
- Enable Google+ API
- Create OAuth2 credentials
- Add authorized redirect URI: `http://localhost:8085/login/oauth2/code/google`

### Build & Run

```bash
# Install dependencies and build
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR file
java -jar target/shrinkurl-0.0.1-SNAPSHOT.jar
```

The application will be available at: `http://localhost:8085`

## 🎯 Usage Guide

### Basic URL Shortening
1. **Home Page**: Visit the main page and enter a URL
2. **Dashboard**: Access your personalized dashboard after login
3. **API Access**: Use REST API endpoints for programmatic access

### API Documentation

#### Authentication
```bash
# OAuth2 login (redirects to Google)
GET /oauth2/authorization/google

# Form-based login
POST /login
Content-Type: application/x-www-form-urlencoded
email=user@example.com&password=password
```

#### URL Operations
```bash
# Shorten URL
POST /api/urls/shorten
Authorization: Bearer {jwt_token}
Content-Type: application/json
{
  "originalUrl": "https://example.com/very-long-url"
}

# Get user URLs
GET /api/urls/my-urls
Authorization: Bearer {jwt_token}

# Get URL analytics
GET /api/urls/{id}/analytics?days=30
Authorization: Bearer {jwt_token}
```

#### Subscription Management
```bash
# Get usage statistics
GET /api/analytics/usage
Authorization: Bearer {jwt_token}

# Upgrade subscription
POST /subscription/upgrade
Content-Type: application/x-www-form-urlencoded
plan=MONTHLY
```

### Subscription Plans

| Feature | FREE | MONTHLY ($9.99) | YEARLY ($99.99) | LIFETIME ($299.99) |
|---------|------|----------------|----------------|-------------------|
| URLs | 20 | 50 | 100 | Unlimited |
| API Calls/Day | 100 | 10,000 | 100,000 | Unlimited |
| Storage | 100 MB | 5 GB | 50 GB | 1 TB |
| URL Retention | 30 days | 365 days | No expiration | No expiration |
| Custom Short Codes | ❌ | ✅ | ✅ | ✅ |
| Advanced Analytics | ❌ | ✅ | ✅ | ✅ |
| Priority Support | ❌ | ✅ | ✅ | ✅ |

## 🔧 Configuration

### Rate Limiting
Configure rate limits in controllers using `@RateLimit` annotation:
```java
@RateLimit(value = 50, timeWindow = 60, message = "Too many requests")
```

### Global Rate Limiting
Modify `GlobalRateLimitFilter` for IP-based limiting:
```java
// 1000 requests per hour per IP
private static final int RATE_LIMIT = 1000;
private static final int TIME_WINDOW = 3600;
```

### URL Expiration Policies
Customize in `UrlService.calculateExpirationDate()`:
```java
switch (plan) {
    case FREE: return LocalDateTime.now().plusDays(30);
    case MONTHLY: return LocalDateTime.now().plusDays(365);
    case YEARLY:
    case LIFETIME: return null; // No expiration
}
```

## 🧪 Testing & Monitoring

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=SubscriptionServiceTest

# Run with coverage
mvn test jacoco:report
```

### Health Checks
```bash
# Application health
GET /actuator/health

# Redis verification
GET /api/admin/verify/redis

# Rate limiting test
POST /api/admin/verify/test-rate-limiting
```

### Monitoring Endpoints
- **Health Check**: `/actuator/health`
- **Metrics**: `/actuator/metrics`
- **Redis Stats**: `/api/admin/verify/redis-stats`
- **Test Dashboard**: `/test-dashboard`

## 🚦 Rate Limiting System

### Global Rate Limiting
- **IP-based**: 1000 requests per hour per IP address
- **Redis Storage**: Sliding window counters in Redis DB 1
- **Bypass**: Authenticated users get higher limits

### Endpoint-specific Rate Limiting
- **URL Shortening**: 50 requests per minute
- **Analytics**: 20 requests per minute for detailed analytics
- **User URLs**: 100 requests per minute

### Configuration
```java
@RateLimit(value = 50, timeWindow = 60, message = "Custom error message")
```

## 🔐 Security Features

### Authentication Flow
1. **OAuth2**: Google-based authentication with automatic user creation
2. **JWT Tokens**: Stateless authentication with refresh token support
3. **Session Management**: Secure session handling with Spring Security

### Security Headers
- CSRF protection enabled
- Content Security Policy headers
- Secure cookie configuration
- HTTPS enforcement (production)

### Data Protection
- Password encryption using BCrypt
- SQL injection prevention with parameterized queries
- XSS protection with Thymeleaf escaping
- Rate limiting to prevent abuse

## 📊 Analytics & Reporting

### Real-time Analytics
- **Click Tracking**: Real-time click count updates
- **Geographic Data**: Country and region-based analytics
- **Referrer Tracking**: Source website identification
- **Device Analytics**: Browser and device type tracking

### Dashboard Features
- **Interactive Charts**: Chart.js-powered visualizations
- **Usage Statistics**: URL creation, click trends, storage usage
- **Subscription Insights**: Plan comparison and upgrade recommendations
- **Export Options**: CSV and JSON data export

## 🔄 Deployment

### Production Configuration
```yaml
spring:
  profiles:
    active: production
  datasource:
    url: jdbc:mysql://production-db:3306/ShrinkURL
  redis:
    host: production-redis
    port: 6379
    
server:
  port: 8080
  ssl:
    enabled: true
    
logging:
  level:
    org.stir.shrinkurl: INFO
    
url-shortener:
  base-url: https://shrinkurl.yourdomain.com
```

### Docker Support
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/shrinkurl-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Environment Variables
- `DB_URL`: Database connection URL
- `DB_USERNAME`: Database username
- `DB_PASSWORD`: Database password
- `REDIS_HOST`: Redis server host
- `GOOGLE_CLIENT_ID`: OAuth2 client ID
- `GOOGLE_CLIENT_SECRET`: OAuth2 client secret

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines
- Follow Spring Boot best practices
- Maintain test coverage above 80%
- Use proper logging levels
- Document API changes
- Follow conventional commit messages

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

- **Documentation**: [Wiki](https://github.com/your-username/shrinkurl/wiki)
- **Issues**: [GitHub Issues](https://github.com/your-username/shrinkurl/issues)
- **Email**: support@shrinkurl.com
- **Discord**: [Community Server](https://discord.gg/shrinkurl)

## 🙏 Acknowledgments

- Spring Boot team for the excellent framework
- Chart.js for beautiful visualizations
- Bootstrap team for responsive UI components
- Redis Labs for high-performance caching
- Google for OAuth2 authentication services

---

**ShrinkURL** - Making long URLs short, and short URLs powerful! 🎯
