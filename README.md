# 🔗 URL Shortener — System Design Project

A production-ready URL shortening service built with **Spring Boot 4**, **MySQL**, and **Redis**. Designed with system design principles including Base62 encoding, rate limiting, caching, and comprehensive error handling.

---

## 📐 System Design

### High-Level Architecture

```
┌──────────────┐       ┌──────────────────────────────────────────────────┐
│   Client     │       │                Spring Boot Application            │
│  (Postman/   │       │                                                    │
│   Browser)   │       │  ┌────────────┐   ┌────────────┐   ┌──────────┐  │
│              │──────▶│  │   Rate     │──▶│ Controller │──▶│ Service  │  │
│              │       │  │  Limiter   │   │            │   │          │  │
│              │       │  │(Interceptor)│   └────────────┘   └────┬─────┘  │
│              │◀──────│  └────────────┘                          │        │
└──────────────┘       │         │                                │        │
                       │         ▼                                ▼        │
                       │  ┌────────────┐                   ┌──────────┐   │
                       │  │   Redis    │                   │  MySQL   │   │
                       │  │  (Cache +  │                   │  (URLs)  │   │
                       │  │ Rate Limit)│                   └──────────┘   │
                       │  └────────────┘                                   │
                       └──────────────────────────────────────────────────┘
```

### Request Flow

```
HTTP Request
     │
     ▼
┌─────────────────────┐
│ RateLimitInterceptor │──── Exceeds limit? ──▶ 429 Too Many Requests
│   (preHandle)        │
└──────────┬──────────┘
           │ Allowed
           ▼
┌─────────────────────┐
│   UrlController      │──── @Valid fails? ──▶ 400 Bad Request
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│    UrlService        │──── Invalid URL? ──▶ 400 InvalidUrlException
│                      │──── Expired?     ──▶ 410 UrlExpiredException
│                      │──── Not found?   ──▶ 404 UrlNotFoundException
│                      │──── Duplicate?   ──▶ 409 ShortCodeAlreadyExists
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  MySQL (JPA/DAO)     │ ◀── @Cacheable (Redis)
└─────────────────────┘
```

---

## 🧠 Algorithm Choices

### 1. Base62 Encoding

**Why Base62?**
- Characters: `0-9`, `A-Z`, `a-z` = 62 characters
- URL-safe — no special characters that need encoding
- Compact — 6 characters can represent **62⁶ = 56.8 billion** unique URLs

**How it works:**
```
Input:  DB Auto-Increment ID (e.g., 12345)
Output: Base62 string (e.g., "3D7")

Algorithm: Repeatedly divide by 62, map remainder to character set
```

| DB ID | Base62 Code | Total Combinations |
|-------|-------------|-------------------|
| 1 | `1` | — |
| 62 | `10` | — |
| 1,000 | `G8` | — |
| 999,999 | `4c91` | — |
| 56.8B | `zzzzzz` | 6-char max |

**Trade-offs:**
| Approach | Pros | Cons |
|----------|------|------|
| Base62 (chosen) | Deterministic, no collisions, compact | Sequential — short codes are predictable |
| MD5/SHA hash | Non-sequential | Collisions possible, longer output |
| Random string | Non-sequential | Collision check needed every time |
| UUID | Universally unique | Too long (36 chars) for short URLs |

### 2. Rate Limiting — Fixed Window Counter

**Why Fixed Window?**
- Simple to implement with Redis `INCR` + `EXPIRE`
- O(1) per request — extremely fast
- Minimal memory per client (single key per IP)

**Algorithm:**
```
Key:    "rate_limit:{clientIp}"
Action: INCR key → if count == 1, set TTL 60s
Check:  count <= MAX_REQUESTS_PER_MINUTE (10)
```

**Trade-offs:**
| Algorithm | Pros | Cons |
|-----------|------|------|
| Fixed Window (chosen) | Simple, fast, low memory | Burst at window boundaries |
| Sliding Window Log | Accurate | Higher memory (stores timestamps) |
| Sliding Window Counter | Good balance | More complex logic |
| Token Bucket | Smooth rate | Requires refill scheduling |

### 3. Caching — Redis with `@Cacheable`

- **Cache key:** Short code
- **Cache value:** Original URL
- **Benefit:** Avoids DB lookup on repeated access → ~100x faster reads
- **Eviction:** Managed by Redis TTL policies

---

## 🏗️ Project Architecture

```
src/main/java/com/systemdesign/url_shortener/
├── UrlShortenerApplication.java          # Entry point
├── config/
│   └── WebConfig.java                    # Registers interceptors
├── constant/
│   └── Constant.java                     # BASE_URL, limits, expiry days
├── controller/
│   └── UrlController.java               # REST API endpoints
├── dao/
│   └── UrlDao.java                       # JPA repository
├── dto/
│   ├── ShortenUrlRequestDto.java         # Request body with validation
│   └── UrlStatsDto.java                  # Analytics response
├── entity/
│   └── Url.java                          # JPA entity (MySQL table)
├── exception/
│   ├── GlobalExceptionHandler.java       # @RestControllerAdvice
│   ├── InvalidUrlException.java          # 400
│   ├── ShortCodeAlreadyExistsException.java  # 409
│   ├── UrlExpiredException.java          # 410
│   └── UrlNotFoundException.java         # 404
├── ratelimiter/
│   └── RateLimitInterceptor.java         # preHandle — checks Redis counter
├── service/
│   ├── RateLimiterService.java           # Redis INCR + EXPIRE logic
│   └── UrlService.java                   # Core business logic
└── util/
    └── Base62Encoder.java                # Encode/decode ID ↔ short code
```

---

## 📡 API Documentation

### Base URL: `http://localhost:8080/api/url`

---

### 1. Shorten URL

```
POST /api/url/shorten
Content-Type: application/json
```

**Request Body:**
```json
{
  "longUrl": "https://www.cricbuzz.com/live-cricket-scores/152240/srh-vs-rcb",
  "expirationDays": 7,
  "customCode": "srhrcb"
}
```

| Field | Type | Required | Validation |
|-------|------|----------|------------|
| `longUrl` | String | ✅ | Must be a valid URL, max 2048 chars |
| `expirationDays` | Integer | ❌ | Min 1 (default: 30) |
| `customCode` | String | ❌ | 4-10 alphanumeric chars, must be unique |

**Responses:**

| Status | Body | Condition |
|--------|------|-----------|
| `200 OK` | `http://short.url/srhrcb` | Success |
| `400 Bad Request` | `{"message": "Invalid URL format: ..."}` | Malformed URL |
| `400 Bad Request` | `{"fieldErrors": {"longUrl": "must not be blank"}}` | Validation failure |
| `409 Conflict` | `{"message": "Short code already exists: srhrcb"}` | Duplicate custom code |
| `429 Too Many Requests` | — | Rate limit exceeded |

---

### 2. Resolve Short URL

```
GET /api/url/{shortCode}
```

**Example:** `GET /api/url/srhrcb`

**Responses:**

| Status | Body | Condition |
|--------|------|-----------|
| `200 OK` | `https://www.cricbuzz.com/...` | Found & valid |
| `404 Not Found` | `{"message": "No URL found for short code: xyz"}` | Not in DB |
| `410 Gone` | `{"message": "URL has expired for short code: abc"}` | Expired |
| `429 Too Many Requests` | — | Rate limit exceeded |

---

### 3. URL Analytics

```
GET /api/url/analytics/{shortCode}
```

**Example:** `GET /api/url/analytics/srhrcb`

**Responses:**

| Status | Body | Condition |
|--------|------|-----------|
| `200 OK` | `Original URL: ..., Created At: 2026-05-23, Expires At: 2026-05-30, Access Count: 42` | Found |
| `404 Not Found` | `{"message": "No URL found for short code: xyz"}` | Not in DB |

---

## ⚡ Performance Metrics

| Operation | Without Cache | With Redis Cache | Improvement |
|-----------|--------------|------------------|-------------|
| Resolve URL (read) | ~5-10ms (DB hit) | ~0.5-1ms (cache hit) | **~10x faster** |
| Shorten URL (write) | ~10-15ms (2 DB writes) | ~10-15ms (no cache benefit) | — |
| Rate limit check | — | ~0.3ms (Redis INCR) | Negligible overhead |

### Scalability Estimates

| Metric | Value |
|--------|-------|
| Max unique short codes (6 chars) | 56.8 billion |
| Rate limit per IP | 10 requests/minute |
| URL expiration | Configurable (default 30 days) |
| Cache hit ratio (expected) | 80-95% for popular URLs |
| DB storage per URL | ~200 bytes |
| 1M URLs storage | ~200 MB |

---

## 🛠️ Tech Stack

| Component | Technology |
|-----------|-----------|
| Framework | Spring Boot 4.0.6 |
| Language | Java 21 |
| Database | MySQL 8.x |
| Cache & Rate Limiting | Redis |
| ORM | Spring Data JPA / Hibernate |
| Build Tool | Maven |
| Validation | Jakarta Bean Validation |

---

## 🚀 Getting Started

### Prerequisites
- Java 21+
- MySQL running on `localhost:3306`
- Redis running on `localhost:6379`

### Setup

```bash
# 1. Create MySQL database
mysql -u root -p -e "CREATE DATABASE url_shortener_db;"

# 2. Clone & run
cd url-shortener
./mvnw spring-boot:run
```

### Configuration (`application.properties`)

| Property | Default |
|----------|---------|
| `server.port` | 8080 |
| `spring.datasource.url` | `jdbc:mysql://localhost:3306/url_shortener_db` |
| `spring.data.redis.host` | localhost |
| `spring.data.redis.port` | 6379 |

---

## 📸 Screenshots

> Add screenshots of Postman API calls here:

### Shorten URL
<!-- ![POST /api/url/shorten](screenshots/shorten.png) -->

### Resolve URL
<!-- ![GET /api/url/{shortCode}](screenshots/resolve.png) -->

### Analytics
<!-- ![GET /api/url/analytics/{shortCode}](screenshots/analytics.png) -->

### Rate Limiting (429)
<!-- ![429 Too Many Requests](screenshots/rate-limit.png) -->

### Validation Error (400)
<!-- ![400 Bad Request](screenshots/validation-error.png) -->

---

## 📝 Future Enhancements

- [ ] Sliding window rate limiter for smoother throttling
- [ ] User authentication & per-user URL management
- [ ] QR code generation for short URLs
- [ ] Dashboard UI with click analytics charts
- [ ] Geo-location tracking on URL access
- [ ] Bulk URL shortening API
- [ ] Custom domain support

---

## 📄 License

This project is for educational/system design learning purposes.

