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
src/main/java/com/selfblog/
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
| `DB_TYPE` | `sqlite` | No | `sqlite` (default) or `mysql` |
| `DB_URL_SQLITE` | `jdbc:sqlite:campus_blog.db` | No | SQLite file URL; relative paths resolve against the working directory |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USERNAME` | — | MySQL only | MySQL connection details |
| `DB_PASSWORD` | — | MySQL only | MySQL password (min 8 chars) |
| `JWT_SECRET` | — | **Yes** | Random JWT/hash secret (min 32 chars) |
| `JWT_EXPIRATION` | `86400000` | No | Access-token lifetime in ms (24h) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | No | Refresh-token lifetime in ms (7d) |
| `ADMIN_USERNAME` | `admin` | No | Initial administrator username |
| `ADMIN_PASSWORD` | — | **Yes** | Initial administrator password (min 12 chars) |
| `ADMIN_NICKNAME` | `站长` | No | Initial administrator nickname |
| `ADMIN_PATH` | `admin` | No | Initial author route; editable in settings |
| `SERVER_PORT` | `8825` | No | Server port |
| `SITE_BASE_URL` | — | No | Public origin used in feeds/sitemap behind a reverse proxy |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:8825` | No | CORS whitelist; `*` is rejected in production |
| `AVATAR_ALLOWED_DOMAINS` | `localhost,127.0.0.1` | No | Allowed hosts for avatar URLs |
| `UPLOAD_MAX_FILE_SIZE` | `52428800` | No | Max single upload (bytes, 50 MB) |
| `UPLOAD_MAX_REQUEST_SIZE` | `104857600` | No | Max multipart request (bytes, 100 MB) |
| `SWAGGER_ENABLED` | `true` | No | Toggle API docs (disabled under the `prod` profile) |
| `MAIL_*` | — | No | SMTP configuration for password reset (`MAIL_SSL_ENABLED` defaults to `true`) |
| `ENV_VALIDATION_ENABLED` | `true` | No | Startup environment validation |
| `MYBATIS_SQL_LOG` | — | No | Optional SQL logging implementation class |

## Performance

### Database Indexes

Covering high-frequency queries: user post lists, category filtering, hot/latest sorting, comment lists, like/collect/follow checks, notification lists, circle posts, draft lists, report queries, messaging.

### Caffeine Cache

| Cache | Content | TTL |
|-------|---------|-----|
| `hotTagsCache` | Hot tags | 10 min |
| `categoryCache` | Categories | 30 min |
| `userCache` | User info | 10 min |
| `trendingCache` | Trending stats | 5 min |
| `statsCache` | System stats | 15 min |
| `statusCache` | Interaction status | 2 min |
| `siteSettingsCache` | Site settings | 10 min |
| `visitorSummaryCache` | Visitor dashboard summary | 30 sec |

- `@Cacheable` on 12+ query methods
- `@CacheEvict` on 15+ write methods
- TTLs follow `CaffeineCacheConfig.java`; actual performance gains require workload-specific measurement.

## Testing

```bash
mvn test "-Dskip.npm=true" "-Dskip.installnodenpm=true"                 # 463 tests, ~1 min
mvn test "-Dtest=AuthControllerTest" "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

Always pass the two skip flags for test-only runs: without them the `frontend-maven-plugin` runs `npm ci` + `npm run build` in `../frontend` first and mutates the frontend build output. Tests use an in-memory H2 database on a random port, with no impact on production data.

### Real-service follow-up verification (2026-10-16)

The full backend suite was rerun: **463 tests, 0 failures, 0 errors, 0 skipped**.
An additional current-source instance used a fresh temporary SQLite database and
separate upload directories. Real HTTP/browser checks exercised administrator login,
direct publication and public queries, guest likes/comments and moderation, protected
album images/unlock tokens, music upload/download, settings persistence and feeds.
The existing local service and its data were not modified.

The combined audit initially passed **13/15 workflow assertions**, exposing a frontend
unsaved-changes warning after successful publication and tag inputs allowing 40
characters against the server's existing 20-character rule. After frontend fixes,
the audit plus two additional editor review scenarios passes **17/17**. The backend rule was verified to reject a 21-character
value with HTTP 400; no backend API/schema/validation change was needed.
See the [audit report](../docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md)
for exact scope. This does **not** validate a real MySQL deployment, Docker build,
SMTP delivery or external media providers.

## Docker

The container files (`Dockerfile`, `docker-compose.yml`) still target an older multi-module layout and do **not** build the single JAR correctly yet — the expected artifact name and the frontend build context are out of date. Until that setup is reworked, deploy with the single-JAR flow above.

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
