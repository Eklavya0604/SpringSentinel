# 🛡️ SpringSentinel

A high-performance Spring Boot microservice acting as a central API gateway and guardrail system for a social media platform. Built to handle massive concurrent bot interactions safely using Redis atomic operations.

---

# 📌 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Role System](#role-system)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [Swagger UI Testing](#swagger-ui-testing)
- [API Endpoints](#api-endpoints)
- [Comment Threading](#comment-threading)
- [Redis Key Design](#redis-key-design)
- [Thread Safety — How Atomic Locks Work](#thread-safety--how-atomic-locks-work)
- [Test Cases](#test-cases)
- [Race Condition Results](#race-condition-results)
- [Postman Collection](#postman-collection)
- [Future Improvements](#-future-improvements)

---

# Overview

SpringSentinel is a production-grade REST API backend that simulates a social media platform where both real users and AI bots can interact.

The system uses a dual-database architecture:

- PostgreSQL → Permanent source of truth
- Redis → Real-time guardrail engine

The core challenges solved:

> 1. Preventing race conditions when hundreds of bots interact simultaneously while keeping the backend completely stateless.
> 2. Secure multi-user interaction where identity is always resolved from JWT — never trusted from the request body.
> 3. Role-based access control separating USER and ADMIN capabilities.

---

# Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| Spring Boot | 3.3.0 | Application framework |
| Spring Security | 3.x | JWT authentication + RBAC |
| Spring Data JPA | 3.x | PostgreSQL ORM |
| Spring Data Redis | 3.x | Redis operations |
| PostgreSQL | 15 | Persistent storage |
| Redis | 7 | Guardrails & caching |
| Docker & Compose | Latest | Containerization |
| JJWT | 0.12.6 | JWT token generation |
| Lombok | Latest | Boilerplate reduction |
| Maven | 3.9+ | Build tool |
| SpringDoc OpenAPI | 2.5.0 | Swagger UI |

---

# Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                 Client / Swagger UI / Postman           │
└──────────────────────────┬──────────────────────────────┘
                           │ HTTP Requests
                           ▼
┌─────────────────────────────────────────────────────────┐
│                  Rate Limit Filter                      │
│           (100 requests/min per IP via Redis)           │
└──────────────────────────┬──────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                   JWT Filter                            │
│   (Validates Bearer token + resolves Role on every      │
│    request — ROLE_USER or ROLE_ADMIN)                   │
└──────────────────────────┬──────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot REST Controllers               │
│     PostController | UserController | BotController     │
│              AuthController                             │
│                                                         │
│  Identity always resolved from JWT — never from body    │
└──────────────────────────┬──────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│                    Service Layer                        │
│                                                         │
│  ┌─────────────────────┐   ┌──────────────────────────┐ │
│  │    Redis Guardrails │   │   Notification Service   │ │
│  │                     │   │                          │ │
│  │ ✅ Horizontal Cap   │   │ ✅ 15-min throttle      │ │
│  │    (max 100 bots)   │   │ ✅ Pending queue        │ │
│  │ ✅ Vertical Cap     │   │ ✅ CRON sweeper         │ │
│  │    (max depth 20)   │   │                          │ │
│  │ ✅ Cooldown Cap     │   └──────────────────────────┘ │
│  │    (10 min TTL)     │                                │
│  └──────────┬──────────┘                                │
│             │ APPROVED                                  │
└─────────────┼───────────────────────────────────────────┘
              │
              ▼
┌─────────────────────────┐      ┌────────────────────────┐
│       PostgreSQL        │      │         Redis          │
│   (Permanent Storage)   │      │   (Real-time State)    │
│                         │      │                        │
│ • users (with roles)    │      │ • virality scores      │
│ • bots                  │      │ • bot counters         │
│ • posts                 │      │ • cooldown TTLs        │
│ • comments              │      │ • notification queues  │
│                         │      │ • rate limit counters  │
└─────────────────────────┘      └────────────────────────┘
```

---

# Features

## Phase 1 — Core API

- ✅ User registration and login
- ✅ Role-based access control (USER / ADMIN)
- ✅ Admin-seeded on startup
- ✅ Admin promotion endpoint
- ✅ Bot management (ADMIN only)
- ✅ Create posts
- ✅ Add comments with nested threading
- ✅ Like posts (humans only)
- ✅ JWT authentication

---

## Phase 2 — Redis Virality Engine

### Real-Time Virality Scoring

| Action | Score |
|---|---|
| Bot Comment | +1 |
| Human Like | +20 |
| Human Comment | +50 |

> Note: Only humans can like posts. Bots do not have like capability.

### Redis Guardrails

#### ✅ Horizontal Cap

Maximum 100 bot replies per post.

Implemented using atomic Redis Lua scripting — race condition proof.

#### ✅ Vertical Cap

Maximum comment nesting depth = 20.

Depth is calculated server-side automatically from `parentCommentId` — client never sends `depthLevel`.

#### ✅ Cooldown Cap

Same bot cannot interact with the same human's post more than once per 10 minutes.

---

## Phase 3 — Smart Notification Engine

- ✅ 15-minute notification throttle
- ✅ Pending notification queue
- ✅ CRON sweeper every 5 minutes
- ✅ Notification summarization

---

## Extra Features

- ✅ JWT Authentication with Role extraction
- ✅ Global Exception Handling
- ✅ Input Validation
- ✅ Distributed Rate Limiting (100 req/min per IP)
- ✅ Virality Score Endpoint
- ✅ Docker Containerization
- ✅ Swagger UI at `/swagger-ui.html`

---

# Role System

SpringSentinel uses a two-role system stored in the `users` table. There is no separate admin table.

```
users table
─────────────────────────────────────────────────────
id | username  | password | is_premium | role
1  | admin     | $2a$...  | true       | ADMIN   ← seeded on startup
2  | testuser  | $2a$...  | false      | USER
3  | otheruser | $2a$...  | false      | USER
```

## Roles and Permissions

| Action | USER | ADMIN |
|---|---|---|
| Register / Login | ✅ | ✅ |
| Create post | ✅ | ✅ |
| Like post | ✅ | ✅ |
| Add comment | ✅ | ✅ |
| View virality score | ✅ | ✅ |
| View user profile | ✅ | ✅ |
| Create bot | ❌ 403 | ✅ |
| View bots | ❌ 403 | ✅ |
| Promote user to ADMIN | ❌ 403 | ✅ |

## How Role is Assigned

At registration, `isPremium` field determines the role:

```json
{ "isPremium": false }  →  ROLE_USER
{ "isPremium": true }   →  ROLE_ADMIN
```

> ⚠️ Note: The `isPremium` → ADMIN mapping is a temporary approach. A secure access-code-based system will replace this in a future update.

## Hardcoded Admin

One admin is seeded automatically on every startup:

```
username: admin
password: admin123
```

## Promoting a User to Admin

Only an existing ADMIN can promote another user:

```http
POST /api/users/{id}/promote
Authorization: Bearer <admin_token>
```

After promotion the user must **login again** to receive a new token with ADMIN role.

---

# Project Structure

```text
src/main/java/com/Vaish/SpringSentinel/
│
├── config/
│   ├── SwaggerConfig.java
│   └── AdminSeeder.java          ← seeds admin on startup
│
├── controller/
│   ├── AuthController.java
│   ├── PostController.java
│   ├── UserController.java
│   └── BotController.java
│
├── model/
│   ├── Role.java                 ← USER, ADMIN enum
│   ├── User.java
│   ├── Bot.java
│   ├── Post.java
│   └── Comment.java
│
├── repository/
│   ├── UserRepository.java
│   ├── BotRepository.java
│   ├── PostRepository.java
│   └── CommentRepository.java
│
├── service/
│   ├── PostService.java
│   ├── RedisService.java
│   ├── NotificationService.java
│   └── RateLimiterService.java
│
├── security/
│   ├── JwtService.java
│   ├── JwtFilter.java            ← resolves role from DB on every request
│   └── SecurityConfig.java       ← RBAC rules + @EnableMethodSecurity
│
├── filter/
│   └── RateLimitFilter.java
│
├── exception/
│   └── GlobalExceptionHandler.java
│
├── scheduler/
│   └── NotificationScheduler.java
│
└── SpringSentinelApplication.java
```

---

# Getting Started

## Prerequisites

Install:

- Docker Desktop

---

## Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/SpringSentinel.git
cd SpringSentinel
```

---

## Start Application

```bash
docker-compose up --build
```

API available at:

```
http://localhost:8080
```

Swagger UI available at:

```
http://localhost:8080/swagger-ui.html
```

---

## Stop Application

```bash
docker-compose stop
```

---

## Rebuild After Changes

```bash
docker-compose down -v
docker-compose up --build
```

> Use `-v` to wipe the Postgres volume when schema changes are involved (e.g. new columns).

---

# Swagger UI Testing

Swagger UI is the easiest way to test all endpoints directly from the browser.

## Step 1 — Open Swagger

```
http://localhost:8080/swagger-ui.html
```

## Step 2 — Register or Login

Call `POST /api/auth/login` with admin credentials:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Copy the `token` from the response.

## Step 3 — Authorize

Click the **Authorize 🔒** button at the top right of Swagger UI.

Paste the token and click **Authorize**.

```
Bearer eyJhbGciOiJIUzI1NiJ9...
```

## Step 4 — Test APIs

All protected endpoints will now use your token automatically.

## Swagger Sections

| Section | Description |
|---|---|
| 1. Authentication | Register and login |
| 2. Bots | Create and view bots (ADMIN token required) |
| 3. Posts | Create posts, comments, likes, virality |
| 4. Users | Get user profile, promote to admin |

---

# API Endpoints

## Complete Testing Flow

```text
Login Admin
↓
Create Bot (admin token)
↓
Register User A → Login → Create Post
↓
Register User B → Login → Like Post → Add Comment
↓
Admin triggers Bot Comment
↓
Get Virality Score
```

All protected endpoints require:

```
Authorization: Bearer <JWT_TOKEN>
```

---

## Authentication APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register user | ❌ |
| POST | `/api/auth/login` | Login user | ❌ |

### Register Request

```json
{
  "username": "testuser",
  "password": "test123",
  "isPremium": false
}
```

### Register / Login Response

```json
{
  "message": "User registered successfully",
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "role": "USER"
}
```

---

## User APIs

| Method | Endpoint | Description | Auth | Role |
|---|---|---|---|---|
| GET | `/api/users/{id}` | Get user profile | ✅ | Any |
| POST | `/api/users/{id}/promote` | Promote to ADMIN | ✅ | ADMIN only |

---

## Bot APIs

| Method | Endpoint | Description | Auth | Role |
|---|---|---|---|---|
| POST | `/api/bots` | Create bot | ✅ | ADMIN only |
| GET | `/api/bots` | Get all bots | ✅ | ADMIN only |
| GET | `/api/bots/{id}` | Get bot by ID | ✅ | ADMIN only |

### Create Bot Request

```json
{
  "name": "SentinelBot",
  "personaDescription": "AI moderation bot that prevents spam and abusive behavior."
}
```

### Create Bot Response

```json
{
  "id": 1,
  "name": "SentinelBot",
  "personaDescription": "AI moderation bot that prevents spam and abusive behavior."
}
```

---

## Post APIs

| Method | Endpoint | Description | Auth | Role |
|---|---|---|---|---|
| POST | `/api/posts` | Create post | ✅ | Any |
| GET | `/api/posts` | Get all posts | ✅ | Any |
| POST | `/api/posts/{id}/comments` | Add comment | ✅ | Any |
| GET | `/api/posts/{id}/comments` | Get all comments | ✅ | Any |
| POST | `/api/posts/{id}/like` | Like post | ✅ | Any (humans only) |
| GET | `/api/posts/{id}/virality` | Get virality score | ✅ | Any |

### Create Post Request

```json
{
  "content": "Hello SpringSentinel!"
}
```

> ⚠️ Do NOT send `authorId` or `authorType` — these are set automatically from the JWT token on the server.

### Create Post Response

```json
{
  "id": 1,
  "authorId": 2,
  "authorType": "USER",
  "content": "Hello SpringSentinel!",
  "createdAt": "2026-05-09T12:00:00"
}
```

---

### Add Bot Comment Request

Bot comments must be triggered by an ADMIN token. `authorId` here is the **bot's ID**.

```json
{
  "authorId": 1,
  "authorType": "BOT",
  "content": "Nice post!",
  "parentCommentId": null
}
```

### Add Human Comment Request

For human comments `authorId` is set from JWT automatically — do not send it.

```json
{
  "authorType": "USER",
  "content": "Amazing platform!",
  "parentCommentId": null
}
```

### Comment Response

```json
{
  "id": 1,
  "postId": 1,
  "authorId": 2,
  "authorType": "USER",
  "content": "Amazing platform!",
  "parentCommentId": null,
  "depthLevel": 0,
  "createdAt": "2026-05-09T12:01:00"
}
```

### Cooldown Violation Response

```json
{
  "message": "Bot cooldown active for this user",
  "status": 429,
  "timestamp": "2026-05-09T15:01:30.815513659"
}
```

---

### Like Post Request

```http
POST /api/posts/1/like
Authorization: Bearer <user_token>
```

No body required. Identity resolved from JWT.

Response:

```
Like registered
```

> Only humans can like posts. Bots have no like capability.

---

### Get Virality Score

```http
GET /api/posts/1/virality
```

Response:

```json
{
  "postId": "1",
  "viralityScore": "91"
}
```

---

# Comment Threading

Comments support nested replies up to 20 levels deep using `parentCommentId`.

## How It Works

```text
Post
 └── Comment A  (id=1, depthLevel=0)      parentCommentId: null
      └── Reply B  (id=2, depthLevel=1)   parentCommentId: 1
           └── Reply C  (id=3, depthLevel=2)  parentCommentId: 2
                └── Reply D  (id=4, depthLevel=3)  parentCommentId: 3
```

## Rules

| Rule | Details |
|---|---|
| Top level comment | Send `parentCommentId: null` |
| Reply to a comment | Send `parentCommentId: <comment_id>` |
| depthLevel | Calculated server-side — never send this |
| Max depth | 20 levels — returns 400 if exceeded |

## Why depthLevel Is Server-Side

Previously clients sent `depthLevel` manually in the request body. This was a security flaw — any client could send `"depthLevel": 1` for every comment regardless of actual nesting, bypassing the vertical cap entirely.

The fix: clients send only `parentCommentId`. The server looks up the parent comment's depth and calculates the new depth as `parent.depthLevel + 1`. The client can never manipulate this value — it is marked `@JsonProperty(access = READ_ONLY)` so it is ignored on input.

---

# Redis Key Design

| Key Pattern | Purpose | TTL |
|---|---|---|
| `post:{id}:virality_score` | Real-time virality score | Permanent |
| `post:{id}:bot_count` | Bot reply counter | Permanent |
| `cooldown:bot_{id}:human_{id}` | Bot-human cooldown | 10 minutes |
| `notif_cooldown:user_{id}` | Notification throttle | 15 minutes |
| `user:{id}:pending_notifs` | Notification queue | Cleared by CRON |
| `rate_limit:{ip}` | IP request counter | 1 minute |

---

# Thread Safety — How Atomic Locks Work

## The Problem — Race Condition

Naive implementation:

```java
Long count = redisTemplate.increment(key);

if (count > 100) {
    reject();
}
```

Race condition:

```text
Thread A reads → 99
Thread B reads → 99

Both pass the check
Both increment
Final count = 101  ← data corruption
```

---

## Redis Lua Script Solution

```lua
local current = tonumber(redis.call('GET', KEYS[1])) or 0

if current < tonumber(ARGV[1]) then
    redis.call('INCR', KEYS[1])
    return 1
else
    return 0
end
```

Redis executes Lua scripts atomically — no other command can execute between the CHECK and INCREMENT. Out of 200 concurrent requests, exactly 100 will pass.

## Rollback on Failure

If Redis guardrails pass but PostgreSQL save fails, both Redis keys are rolled back:

```text
1. Redis horizontal cap check    ← gatekeeper
2. Redis cooldown check          ← gatekeeper
3. PostgreSQL save               ← only if both pass
   └── on failure:
       decrementBotCount()       ← rollback
       removeCooldown()          ← rollback
4. Redis virality update         ← only on success
```

This ensures Redis and PostgreSQL never go out of sync.

---

# Test Cases

## Authentication Tests

| TC | Test | Expected |
|---|---|---|
| TC-01 | Register user (isPremium: false) | 200 OK, role: USER |
| TC-02 | Register user (isPremium: true) | 200 OK, role: ADMIN |
| TC-03 | Duplicate username | 409 Conflict |
| TC-04 | Login success | JWT token + role |
| TC-05 | Wrong password | 401 Unauthorized |

---

## Role & Authorization Tests

| TC | Test | Expected |
|---|---|---|
| TC-06 | Create bot with USER token | 403 Forbidden |
| TC-07 | Create bot with ADMIN token | 200 OK |
| TC-08 | View bots with USER token | 403 Forbidden |
| TC-09 | Promote user with USER token | 403 Forbidden |
| TC-10 | Promote user with ADMIN token | 200 OK |

---

## Validation Tests

| TC | Test | Expected |
|---|---|---|
| TC-11 | Empty username | 400 |
| TC-12 | Username too short (< 3 chars) | 400 |
| TC-13 | Empty post content | 400 |
| TC-14 | Empty comment content | 400 |

---

## Multi-User Interaction Tests

| TC | Test | Expected |
|---|---|---|
| TC-15 | User A creates post | authorId set from JWT |
| TC-16 | User B likes User A's post | +20 virality |
| TC-17 | User B comments on User A's post | +50 virality, authorId from JWT |
| TC-18 | User B replies to own comment | depthLevel = 1, auto-calculated |

---

## Redis Guardrail Tests

| TC | Test | Expected |
|---|---|---|
| TC-19 | First bot comment | 200 OK |
| TC-20 | Bot cooldown violation (within 10 min) | 429 Too Many Requests |
| TC-21 | Comment depth > 20 | 400 Bad Request |
| TC-22 | Like post | 200 OK, +20 virality |
| TC-23 | Virality score after interactions | Correct sum |

---

## Concurrency Tests

| TC | Test | Expected |
|---|---|---|
| TC-24 | 200 concurrent bot requests | Exactly 100 allowed, 100 → 429 |

---

# Race Condition Results

Stress test using Newman CLI:

```bash
newman run stress-test.json -n 200
```

| Check | Result |
|---|---|
| Redis bot_count | ✅ Exactly 100 |
| PostgreSQL comments | ✅ Exactly 100 |
| Over-limit requests | ✅ Returned 429 |
| Race conditions | ✅ Zero |

### Verify Manually

Check Redis:

```bash
docker exec -it springsentinel_redis redis-cli
GET post:1:bot_count
```

Check PostgreSQL:

```bash
docker exec -it springsentinel_postgres psql -U postgres -d springsentinel
SELECT COUNT(*) FROM comments WHERE post_id = 1 AND author_type = 'BOT';
```

Both should return exactly `100`.

---

# Postman Collection

Import `SpringSentinel.postman_collection.json` into Postman.

## Import Steps

1. Open Postman
2. Click **Import**
3. Upload `SpringSentinel.postman_collection.json`
4. Tokens are auto-saved via test scripts — no manual copy-paste needed

## Collection Variables

| Variable | Saved by | Used by |
|---|---|---|
| `user_token` | Register/Login User | All user requests |
| `admin_token` | Login Admin | All admin requests |
| `other_token` | Register/Login Second User | Multi-user tests |

---

# 🔮 Future Improvements

- [ ] Replace `isPremium` ADMIN assignment with secure access-code system
- [ ] Real push notifications (WebSocket / FCM)
- [ ] AI moderation pipeline
- [ ] Trending feed generation
- [ ] Bot like capability with configurable virality weight
- [ ] Refresh token support
