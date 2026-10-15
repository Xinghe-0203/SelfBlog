# SelfBlog - Backend

Spring Boot 3 backend for a single-author personal blog. Public visitors can read and search published posts; author operations require an administrator account.

**Version**: v2.0.25 | **Java**: 21 | **Spring Boot**: 3.3.13

## Tech Stack

| Component | Version | Purpose |
|----------|---------|---------|
| Java | 21 | LTS |
| Spring Boot | 3.3.13 | Web framework |
| MyBatis Plus | 3.5.8 | ORM |
| Spring Security | 6.x | Authentication |
| JWT | 0.12.6 | JWT |
| Caffeine | 3.2.0 | Local cache |
| Knife4j | 4.5.0 | API documentation |
| Jsoup | 1.18.3 | XSS protection |
| Hutool | 5.8.40 | Utility library |
| Lombok | 1.18.42 | Code simplification |

## Quick Start: Single JAR (SQLite default)

The production JAR embeds the Vue frontend and serves both the site and `/api`. Build requires JDK 21, Maven 3.8+, and network access on the first build; the Maven frontend plugin installs Node/npm and compiles Vue automatically.

```bash
cp .env.example .env
# Set JWT_SECRET (random, >=32 chars) and ADMIN_PASSWORD (>=12 chars)
mvn package -DskipTests
```

The executable artifact is `target/selfblog.jar`. Copy it and the configured `.env` to the same runtime directory, then run `java -jar selfblog.jar`.

SQLite defaults to `./campus_blog.db` in the working directory, matching the legacy default database file. Keep/copy the existing `campus_blog.db` to the JAR's working directory (or point `DB_URL_SQLITE` to its current location). New feature tables are created with `CREATE TABLE IF NOT EXISTS`, preserving existing article tables/data. Persist the SQLite database, `uploads/`, and `gallery-media/` when deploying.

### Existing MySQL deployments

Set `DB_TYPE=mysql` and configure all `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` values in `.env`. For an **existing** database, select it and run only `migration-selfblog.sql`; do not run `schema.sql`, which is a legacy fresh-install script containing `DROP TABLE` statements. `schema.sql` is only for a new, empty database.

### Local development

```bash
mvn spring-boot:run             # Spring Boot API (loads .env; SQLite by default)
cd ../frontend
npm ci
npm run dev                     # Vue dev server at http://localhost:3000
```

| Endpoint | URL |
|----------|-----|
| Blog | http://localhost:8825/ |
| Default author entry | http://localhost:8825/admin |
| API | http://localhost:8825/api |
| API Docs | http://localhost:8825/doc.html |
| Health | http://localhost:8825/actuator/health |

The first startup creates the author account from `ADMIN_USERNAME` (default `admin`) and `ADMIN_PASSWORD`; there is no fixed default password. The author can change the admin path in site settings.

## Project Structure

```
src/main/java/com/example/selfblog/
├── controller/
│   ├── auth/         # AuthController, PasswordController
│   ├── user/         # SysUserController
│   ├── post/         # BlogPost, BlogComment, BlogLike, BlogCollect, BlogTag
│   ├── circle/       # CircleController
│   ├── social/       # Follow, Message, Notification, Report
│   ├── content/      # Topic, Trending, Media, Share, Statistics
│   ├── admin/        # Author management APIs
│   └── site/         # Settings, visitors, guest comments, and gallery APIs
├── service/
│   ├── auth/         # SysUserService, EmailService
│   ├── post/         # BlogPostService, BlogTagService, PostInteractionService, PostQueryService
│   ├── circle/       # CircleService, CircleQueryService, CircleInteractionService
│   ├── social/       # BlogCollect/Comment/Like, Follow, Message, Notification, Report
│   └── content/      # Media, Share, Statistics, Topic, Trending
├── mapper/           # MyBatis Plus mappers
├── entity/           # Database entity classes
├── dto/              # Request DTOs (domain-grouped)
├── vo/               # Response VOs (domain-grouped)
├── config/
│   ├── security/     # SecurityConfig, JwtAuthenticationFilter, XssFilter
│   ├── web/          # WebMvcConfig, RateLimitInterceptor
│   ├── cache/        # CaffeineCacheConfig
│   └── db/           # DatabaseConfig, MybatisPlusConfig
├── common/           # Result, BusinessException, PageResult
├── annotation/       # Custom annotations
├── aspect/           # AOP aspects
├── event/            # Application events
├── listener/         # Event listeners
└── util/             # Utility classes
```

## Architecture

```
Controller → Service → Mapper → Entity
```

### Conventions

| Convention | Implementation |
|-----------|---------------|
| Unified response | `Result<T>` - `Result.success(data)` / `Result.error(code, msg)` |
| Mapper inheritance | `BaseMapper<T>` (MyBatis Plus) |
| Soft delete | `isDeleted` + `@TableLogic` (0=active, 1=deleted) |
| Write transactions | `@Transactional(rollbackFor = Exception.class)` |
| Read transactions | `@Transactional(readOnly = true)` |
| Business exceptions | `throw new BusinessException(code, message)` |
| Current user | `SecurityUtils.getCurrentUserIdOrNull()` |

### Response Format

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1716000000000
}
```

Paginated responses include `records`, `total`, `size`, `current`, `pages` in `data`.

## Security

The site runs in single-author mode: registration is disabled and password authentication is limited to administrator accounts. Published articles and tags are public; anonymous likes, guest comments, visitor tracking, and gallery reading have dedicated public endpoints. Content writes and management endpoints require the administrator role. Existing article data is retained.

| Feature | Implementation |
|---------|---------------|
| Password | BCrypt (strength 12), never returned in API |
| JWT | Access Token (24h) + Refresh Token (7d) with rotation |
| Token blacklist | Added on logout, prevents replay |
| Login lock | 5 failures → 15 min lock (atomic update) |
| XSS | HtmlSanitizer (Jsoup) for server-side purification |
| Rate limiting | Path-prefix limits via `RateLimitInterceptor`; site-asset uploads: 30 requests/min |
| IP protection | Format validation against IP spoofing |
| Validation | Spring Validation (`@Valid`, `@NotBlank`) |
| CORS | Configurable whitelist, `*` forbidden in production |

## Environment Variables

| Variable | Default | Required | Description |
|----------|---------|----------|-------------|
| `DB_TYPE` | sqlite | No | `sqlite` (default) or `mysql` |
| `DB_URL_SQLITE` | `jdbc:sqlite:./data/selfblog.db` | No | SQLite file URL |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` | — | MySQL only | MySQL connection details |
| `DB_PASSWORD` | — | MySQL only | MySQL password (min 8 chars) |
| `JWT_SECRET` | — | **Yes** | Random JWT/hash secret (min 32 chars) |
| `ADMIN_USERNAME` | `admin` | No | Initial administrator username |
| `ADMIN_PASSWORD` | — | **Yes** | Initial administrator password (min 12 chars) |
| `ADMIN_PATH` | `admin` | No | Initial author route; editable in settings |
| `SERVER_PORT` | 8825 | No | Server port |
| `CORS_ALLOWED_ORIGINS` | localhost:3000,localhost:8825 | No | CORS whitelist |
| `MAIL_*` | — | No | SMTP configuration |

## Performance

### Database Indexes

Covering high-frequency queries: user post lists, category filtering, hot/latest sorting, comment lists, like/collect/follow checks, notification lists, circle posts, draft lists, report queries, messaging.

### Caffeine Cache

| Cache | Content | TTL |
|-------|---------|-----|
| `hotTagsCache` | Hot tags | 30 min |
| `categoryCache` | Categories | 1 hour |
| `userCache` | User info | 30 min |
| `trendingCache` | Trending stats | 10 min |
| `statsCache` | System stats | 5 min |
| `siteSettingsCache` | Site settings | 10 min |
| `visitorSummaryCache` | Visitor dashboard summary | 30 sec |

- `@Cacheable` on 12+ query methods
- `@CacheEvict` on 15+ write methods
- Expected 15-300x query performance improvement

## Testing

```bash
mvn test                            # All tests
mvn test -Dtest=AuthControllerTest  # Single test class
```

Tests use H2 in-memory database (`application-test.yml`), random port, no impact on production data.

## Docker

```bash
cp .env.example .env        # Configure DB_PASSWORD and JWT_SECRET
docker-compose up -d        # Start MySQL + App
docker-compose logs -f      # View logs
docker-compose down         # Stop
```

Multi-stage build: JDK 21 (build) → JRE 21 Alpine (runtime). Runs as non-root user `appuser` (UID 1001).

## API Modules

| Group | Endpoints | Group | Endpoints |
|-------|-----------|-------|-----------|
| Auth | 5 | Follow | 5 |
| User | 8 | Notification | 5 |
| Post | 12 | Message | 5 |
| Comment | 6 | Circle | 8 |
| Like | 4 | Topic | 5 |
| Collect | 4 | Report | 4 |
| Tag | 5 | Share | 3 |
| Media | 4 | Statistics | 4 |
| Trending | 3 | Admin | 16+ |

Total: **120+ endpoints**. Authenticated endpoints require `Authorization: Bearer <access_token>`.
