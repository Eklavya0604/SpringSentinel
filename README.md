# 🛡️ SpringSentinel

A high-performance Spring Boot microservice acting as a central API gateway and guardrail system for a social media platform. Built to handle massive concurrent bot interactions safely using Redis atomic operations.

---

# 📌 Table of Contents

- [Overview](#overview)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
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

The core challenge solved:

> Preventing race conditions when hundreds of bots interact simultaneously while keeping the backend completely stateless.

---

# Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Core language |
| Spring Boot | 3.x | Application framework |
| Spring Security | 3.x | JWT authentication |
| Spring Data JPA | 3.x | PostgreSQL ORM |
| Spring Data Redis | 3.x | Redis operations |
| PostgreSQL | 15 | Persistent storage |
| Redis | 7 | Guardrails & caching |
| Docker & Compose | Latest | Containerization |
| JJWT | 0.12.6 | JWT token generation |
| Lombok | Latest | Boilerplate reduction |
| Maven | 3.9+ | Build tool |

---

# Architecture

```text
┌─────────────────────────────────────────────────────────┐
│                    Client / Postman                     │
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
│         (Validates Bearer token on every request)       │
└──────────────────────────┬──────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────┐
│              Spring Boot REST Controllers               │
│     PostController | UserController | BotController     │
│              AuthController                             │
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
│   (Permanent Storage)   │      │   (Real-time State)   │
│                         │      │                        │
│ • users                 │      │ • virality scores     │
│ • bots                  │      │ • bot counters        │
│ • posts                 │      │ • cooldown TTLs       │
│ • comments              │      │ • notification queues │
│                         │      │ • rate limit counters │
└─────────────────────────┘      └────────────────────────┘
```

---

# Features

## Phase 1 — Core API

- ✅ User management
- ✅ Bot management
- ✅ Create posts
- ✅ Add comments
- ✅ Like posts
- ✅ JWT authentication

---

## Phase 2 — Redis Virality Engine

### Real-Time Virality Scoring

| Action | Score |
|---|---|
| Bot Reply | +1 |
| Human Like | +20 |
| Human Comment | +50 |

### Redis Guardrails

#### ✅ Horizontal Cap

Maximum 100 bot replies per post.

Implemented using atomic Redis Lua scripting.

#### ✅ Vertical Cap

Maximum comment nesting depth = 20.

#### ✅ Cooldown Cap

Same bot cannot interact with same human twice in 10 minutes.

---

## Phase 3 — Smart Notification Engine

- ✅ 15-minute notification throttle
- ✅ Pending notification queue
- ✅ CRON sweeper every 5 minutes
- ✅ Notification summarization

---

## Extra Features

- ✅ JWT Authentication
- ✅ Global Exception Handling
- ✅ Input Validation
- ✅ Distributed Rate Limiting
- ✅ Virality Score Endpoint
- ✅ Docker Containerization

---

# Project Structure

```text
src/main/java/com/Vaish/SpringSentinel/
│
├── controller/
│   ├── AuthController.java
│   ├── PostController.java
│   ├── UserController.java
│   └── BotController.java
│
├── model/
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
│   ├── JwtFilter.java
│   └── SecurityConfig.java
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

```text
http://localhost:8080
```

---

## Stop Application

```bash
docker-compose stop
```

---

## Rebuild After Changes

```bash
docker-compose down
docker-compose up --build
```

---

# API Endpoints

## Complete API Testing Flow

```text
Register User
↓
Login User
↓
Create Bot
↓
Create Post
↓
Add Bot Comment
↓
Like Post
↓
Get Virality Score
```

All protected endpoints require:

```text
Authorization: Bearer <JWT_TOKEN>
```

---

# Authentication APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/register` | Register user | ❌ |
| POST | `/api/auth/login` | Login user | ❌ |

---

## Register Request

```json
{
  "username": "testuser",
  "password": "test123",
  "isPremium": false
}
```

---

## Register/Login Response

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

# Users APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/users` | Create user | ✅ |
| GET | `/api/users/{id}` | Get user | ✅ |

---

# Bot APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/bots` | Create bot | ✅ |
| GET | `/api/bots/{id}` | Get bot | ✅ |

---

## Create Bot Request

```json
{
  "name": "SentinelBot",
  "personaDescription": "AI moderation bot that prevents spam and abusive behavior."
}
```

---

## Create Bot Response

```json
{
  "id": 1,
  "name": "SentinelBot",
  "personaDescription": "AI moderation bot that prevents spam and abusive behavior."
}
```

---

# Post APIs

| Method | Endpoint | Description | Auth |
|---|---|---|---|
| POST | `/api/posts` | Create post | ✅ |
| POST | `/api/posts/{id}/comments` | Add comment | ✅ |
| POST | `/api/posts/{id}/like` | Like post | ✅ |
| GET | `/api/posts/{id}/virality` | Get virality score | ✅ |

---

## Create Post Request

```json
{
  "authorId": 1,
  "authorType": "USER",
  "content": "Hello SpringSentinel!"
}
```

---

## Add Bot Comment Request

```json
{
  "authorId": 1,
  "authorType": "BOT",
  "content": "Nice post!",
  "depthLevel": 1
}
```

---

## Like Post Request

```http
POST /api/posts/1/like?likerType=USER
```

No request body required.

---

## Get Virality Request

```http
GET /api/posts/1/virality
```

---

## Virality Response

```json
{
  "postId": "1",
  "viralityScore": "21"
}
```

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
Thread A -> 99
Thread B -> 99

Both pass
Final count = 101
```

---

# Redis Lua Script Solution

```lua
local current = tonumber(redis.call('GET', KEYS[1])) or 0

if current < tonumber(ARGV[1]) then
    redis.call('INCR', KEYS[1])
    return 1
else
    return 0
end
```

Redis executes Lua scripts atomically.

No other thread can execute between:

```text
CHECK
AND
INCREMENT
```

This completely eliminates race conditions.

---

# Test Cases

## Authentication Tests

| TC | Test | Expected |
|---|---|---|
| TC-01 | Register user | 200 OK |
| TC-02 | Duplicate username | 409 Conflict |
| TC-03 | Login success | JWT token |
| TC-04 | Wrong password | 401 Unauthorized |

---

## Validation Tests

| TC | Test | Expected |
|---|---|---|
| TC-05 | Empty username | 400 |
| TC-06 | Username too short | 400 |
| TC-07 | Empty post content | 400 |

---

## Redis Guardrail Tests

| TC | Test | Expected |
|---|---|---|
| TC-08 | First bot comment | 200 OK |
| TC-09 | Cooldown violation | 429 Too Many Requests |
| TC-10 | Depth > 20 | 400 Bad Request |
| TC-11 | Like post | 200 OK |

---

## Concurrency Tests

| TC | Test | Expected |
|---|---|---|
| TC-12 | 200 concurrent bot requests | Exactly 100 allowed |

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

---

# Postman Collection

Import:

```text
SpringSentinel.postman_collection.json
```

into Postman to test APIs.

---

## Import Steps

1. Open Postman
2. Click Import
3. Upload `SpringSentinel.postman_collection.json`
4. Start testing APIs



- [ ] Real push notifications
- [ ] AI moderation pipeline
- [ ] Trending feed generation
