# SelfBlog - Backend

Spring Boot 3 backend for a single-author personal blog. Public visitors can read and search published posts; author operations require an administrator account.

**Version**: v2.0.29 | **Java**: 21 | **Spring Boot**: 3.3.13

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
| CommonMark | 0.24.0 | Server-side Markdown for crawlable reading HTML |
| Jsoup | 1.18.3 | XSS protection and HTML assembly |
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

Set `DB_TYPE=mysql` and configure all `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` values in `.env`. For an **existing** database, select it and run only `migration-selfblog.sql`; do not run `schema-fresh-install.sql` (formerly `schema.sql`), which is a fresh-install script containing `DROP TABLE` statements. It is only for a new, empty database. CI (`.github/workflows/ci.yml`) initializes a real MySQL 8 with this script and runs the backend test suite against it.

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
| API Docs | http://localhost:8825/doc.html (off by default; set `SWAGGER_ENABLED=true` to enable) |
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
| JWT | Access Token (24h) + Refresh Token (7d) with rotation; both bind to a persistent password-derived credential version |
| Password changes | Changing/resetting a password invalidates all older access/refresh tokens after commit, including after restart; pre-upgrade tokens require login |
| Token blacklist | Process-local for logout/rotation; separate from persistent password-change invalidation |
| Login lock | 5 failures → 15 min lock (atomic update) |
| XSS | HtmlSanitizer (Jsoup) for server-side purification |
| Rate limiting | Segment-boundary matching, longest rule wins; **read and write use separate tables** (`GET`/`HEAD`/`OPTIONS` consult the read table, an equal-or-longer read prefix wins, and counters are bucketed per read/write). Article reads and guest-comment list reads: 60 requests/min per IP; guest-comment creation: 5 requests/min; site-asset uploads: 30 requests/min. The 10/min `/api/post/` rule is a **write** budget — before v2.0.29 it also throttled `GET /api/post/{id}`, so a normal visitor hit 429 on the 11th article |
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
| `SWAGGER_ENABLED` | `false` | No | Toggle API docs. Defaults to **off**; set `true` for local debugging. The `prod` profile additionally pins it off |
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
mvn test "-Dskip.npm=true" "-Dskip.installnodenpm=true"                 # 559 tests, ~1 min
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

## Author-configurable appearance

Nine new keys — `backgroundType`, `backgroundColor`, `backgroundGradientFrom`,
`backgroundGradientTo`, `backgroundImageUrl`, `backgroundOverlay`, `backgroundFixed`,
`surfaceOpacity`, `surfaceBlur` — are registered in `SiteSettingsServiceImpl`'s whitelist
and stored in the `site_setting` KV table. **No schema migration is required.**

`validateValue` is the authority for every key: `backgroundType` is limited to
`solid | gradient | image`, the three colour keys to `#RRGGBB`, `backgroundFixed` to a
boolean, and the numeric keys go through `requireIntegerInRange` (pure decimal integers only,
no decimals or signs). The declared ranges are `backgroundOverlay` 0-90, `surfaceOpacity`
60-100, `surfaceBlur` 0-40. Defaults are chosen so an untouched site renders exactly as
before, which is why the frontend emits **no** CSS variable in that case.

`backgroundImageUrl` is a CSS-injection surface: the value is spliced into `url("…")`
client-side, so whitespace and `\u00A0`, `"`, `'`, `(`, `)`, and `\` are all rejected,
and the value must be `http(s)://` or `/uploads/` prefixed. The frontend re-checks
(`safeBackgroundUrl()`) because it must not assume the data came from this server.

`SiteAssetKind.BACKGROUND` (`"background"`) was added to the upload whitelist: raster
formats only (`.png/.jpg/.jpeg/.webp`) with an **8MB** cap. `.svg` is excluded because an
SVG opened as a top-level document can carry scripts and a background fills the viewport;
`.ico` is favicon-only and too small to scale. The 8MB cap is deliberately above the
2MB used for logos and avatars — a 2560px-wide photo routinely exceeds 2MB, and reusing the
smaller limit would force authors to re-compress until quality degrades. The frontend's
client-side pre-check must stay at the same 8MB, since a stricter check would make the
server's allowance unreachable.

### Typography, layout, presentation and motion (v2.0.29)

Eleven more `site_setting` keys, again with **zero schema migration**:

| Dimension | Keys | Allowed values | Default |
|---|---|---|---|
| Typography | `fontFamily` | `system \| sans \| serif \| mono` | `system` |
| | `baseFontSize` | 14–19 (integer, px) | `16` |
| | `bodyLineHeight` | 1.5–2.2 (decimal) | `1.75` |
| | `readerWidth` | 640–960 (integer, px) | `760` |
| | `paragraphSpacing` | 0–2 (decimal, em) | `1` |
| Layout | `radiusScale` | `none \| sharp \| default \| soft \| round` | `default` |
| | `shadowDepth` | `flat \| default \| deep` | `default` |
| | `layoutDensity` | `compact \| default \| comfortable` | `default` |
| Presentation | `homeHeroStyle` | `default \| compact \| minimal \| off` | `default` |
| | `postCoverStyle` | `default \| wide \| hidden` | `default` |
| Motion | `motionIntensity` | `off \| subtle \| default \| lively` | `default` |

Every default is **the value the frontend previously hard-coded**, so upgrading an existing
site is a pixel-for-pixel no-op; the frontend relies on this to emit no hook attribute/variable
at the default (see `frontend/src/stores/site.ts`'s `applyCustomizations`).

`validateValue` gains one branch per key. The two decimal-valued keys go through
`requireDecimalInRange(String, double, double, String)`, whose grammar is
`-?\d{1,3}(\.\d{1,3})?` — scientific notation, a leading `+` and `.5`-style values with no
integer part are all rejected on purpose: the frontend splices these strings straight into CSS,
so validator and consumer must agree on exactly one grammar. Integer keys keep using
`requireIntegerInRange`.

These keys **are** returned by `GET /api/site/public` (they are cosmetic and public pages need
them); `LLM_SETTING_KEYS` remains the only filtered group.

## Page management

`PageService` stores separate versioned draft/published documents and slug reservations
in the existing `site_setting` table, outside the global public-settings whitelist.
No schema migration is required. Database conditional updates enforce expected revisions
and return409 on conflict; public manifests exclude bodies and never contain drafts.

Author endpoints are under `/api/admin/pages` and `/api/admin/page-navigation`;
published reads are under `/api/site/pages`. Both capability whitelists and strict
request decoding reject unknown fields. Body limit10000 characters; one serialized
document limit48KiB UTF-8. Back up the entire database, not only the site-settings API.
See [the page-management guide](../docs/page-management.md) and
[the storage decision](../docs/decisions/001-versioned-pages-in-existing-kv.md).

The final full suite with this feature passes **491 tests / 0 failures / 0 errors / 0 skipped**,
including two real independent-transaction H2 concurrency tests. A disposable SQLite/browser
audit passes **31/31**, including direct access to the packaged single-JAR frontend and
safe authenticated preview while retaining DENY frame headers. See [the verification record](../docs/reviews/2026-10-16-page-management-verification.md)
for final evidence and limits; this does not verify MySQL or Docker.

## Crawlable reading HTML and discovery documents

History routes (`/`, `/archives`, `/about`, `/gallery`, `/post/{id}`, `/tag/{id}`, `/pages/{slug}`)
now return server-rendered public reading HTML instead of an empty SPA shell. `SpaController`
delegates to `ReadingPageService` + `ReadingHtmlRenderer`:

- the built `static/index.html` is cloned per request so Vite's hashed assets, icons and feed
  links stay intact; `#app` carries a semantic reading DOM that Vue replaces on mount;
- head metadata (title, description, robots, canonical, Open Graph, Twitter, BlogPosting
  JSON-LD) is rebuilt per request, with `<` escaped as `\u003c` inside JSON-LD;
- only published content is exposed: `getPublishedPostDetail`/`getPublishedMetadata` live in
  the owning `PostQueryService`, drafts stay 404 even for an authenticated admin, and reading
  requests never increment view counts or visitor events;
- `/index.html` answers `308` to `/`, unknown paths return a real 404, and data/render
  failures return safe 5xx + `noindex` (missing build template → 503);
- admin paths and legacy account routes keep the app shell with `noindex,follow`.

`SiteOrigin` is the single canonical-origin resolver. Set `SITE_BASE_URL` to the public origin
(e.g. `https://blog.example.com`) in production; once configured, spoofed `Host`/
`X-Forwarded-*` headers cannot change canonical URLs, feeds or sitemap links. Leave it empty
only for local development, where the direct request origin is used.

The author can also set the same origin in the admin UI (**网站设置 → 站点资料 → 网站域名**,
stored as the `siteBaseUrl` setting). Priority: admin setting → `SITE_BASE_URL` env → direct
request origin. Saving takes effect on the next request (the settings cache is evicted);
clearing falls back to the env value. Invalid stored values are logged and ignored instead of
breaking the site. The public settings response includes `siteBaseUrl` because the canonical
domain is public by definition.

Discovery endpoints (root path, no `/api` prefix):

| Endpoint | Content |
| --- | --- |
| `/feed.xml`, `/atom.xml` | Published posts, newest first, capped at 100 items |
| `/sitemap.xml` | Sitemap index referencing `sitemap-site.xml` and `sitemap-posts-N.xml`, `sitemap-tags-N.xml`, `sitemap-pages-N.xml` |
| `/sitemap-site.xml` | Home/archives/about/gallery, honoring each published page's `indexable` |
| `/sitemap-posts-N.xml` etc. | Shards of at most 1000 URLs, published-only, `lastmod` from real timestamps |
| `/robots.txt` | Dynamic `Disallow` for the configured admin path, `/api/` and `/search`, plus the sitemap URL |

The sitemap no longer shares the feed's 100-item query, so every published post/tag/page is
discoverable; it never lists drafts, deleted posts, `/search` or index-disabled pages. An
overflow beyond 50000 shards fails explicitly instead of truncating. Canonical origin and page
`indexable` semantics remain consistent between the server response and the Vue runtime
(`selfblog-site-origin` is read by `useSeoMeta`).

### Running behind a reverse proxy or Vercel

The JAR owns HTML, hashed assets, API, uploads and discovery documents; a CDN/proxy should
forward every path unchanged (including `/uploads/**`) to that single origin. Vercel support in
this repository only *proxies* to the JAR (no second static frontend), see
[frontend/vercel.json](../frontend/vercel.json) and `frontend/scripts/vercel-proxy.mjs`.

**Scope note:** returning crawlable HTML establishes the technical conditions for indexing;
it does not prove that search engines have indexed the site, and no ranking is promised.

The final full suite with this feature passes **559 tests / 0 failures / 0 errors / 0 skipped**.
See [the verification record](../docs/reviews/2026-10-16-seo-deployment-verification.md) for the
live evidence and its limits (real MySQL, Docker and a real Vercel deployment are not covered
by the automated run).

## Docker status

`Dockerfile` and `docker-compose.yml` now build the real single JAR from a **repository-root**
build context (Maven + JDK 21 image, non-root JRE runtime, `curl` healthcheck), and
`docker-compose.sqlite.yml` provides an explicit SQLite alternative. Keep the JAR+`.env` flow
above when you do not need containers. Container images were not built or run in this
environment (no Docker daemon); treat image execution as unverified.

## API Modules

> Backend community/circle/topic endpoints are retained (admin-only), but their frontend
> circle/topic pages were removed on 2026-10-17 as unreachable dead code; see CHANGELOG.

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
| Trending | 3 | Admin SEO | 9 |
| Admin AI | 9 | Admin | 16+ |

Total: **120+ endpoints**. Authenticated endpoints require `Authorization: Bearer <access_token>`.

## LLM-assisted SEO (2026-10-22, extended 2026-10-22)

Per-post SEO metadata is generated by any **OpenAI-compatible** chat completions endpoint, configured from the admin UI (`llmEnabled`, `llmBaseUrl`, `llmApiKey`, `llmModel`, `llmTimeoutSeconds`, `llmAutoOnPublish` — six `site_setting` keys, no schema migration needed).

v2.0.28 adds five more `site_setting` keys for DeepSeek-native behaviour: `llmProvider` (`deepseek|custom`), `llmThinking` (`enabled|disabled`), `llmReasoningEffort` (`low|high|max`), `llmMaxTokens` (`0`..`131072`, `0` = omit the field), `llmCommentModeration` (`true|false`). All eleven `llm*` keys are filtered out of `GET /api/site/public`.

| Endpoint | Purpose |
|----------|---------|
| `GET /api/admin/seo/overview` | Coverage + issue counts; `llmConfigured` is derived server-side (key **and** base URL present) |
| `GET /api/admin/seo/posts` | Paged audit list (`keyword`, `issue` filters); never returns post bodies |
| `GET /api/admin/seo/posts/{postId}` | Detail for the editor; **404 means "no SEO record yet"**, which the UI renders as an empty form |
| `PUT /api/admin/seo/posts/{postId}` | Save manual metadata (`source=manual`); blank strings are stored as `NULL` |
| `POST /api/admin/seo/posts/{postId}/generate` | Generate via the model (`source=llm`); 400 when unconfigured, 502 when the upstream fails |
| `POST /api/admin/seo/batch` | Bounded serial batch (default 10, hard cap 50); failures are isolated per post |
| `POST /api/admin/seo/test-connection` | Always HTTP 200 with `ok:false` on failure; message is truncated and redacts the key |
| `GET /api/admin/seo/models` | Probes upstream `/models`; always 200 — on failure returns built-in candidates with `source="fallback"` and a `reason` |
| `DELETE /api/admin/seo/posts/{postId}` | Idempotent; the reader page falls back to derived metadata |

Notes that matter when changing this area:

- `post_seo` is a **separate table**, not new `blog_post` columns: this repo has no column-migration mechanism (`migration-selfblog.sql` is `CREATE TABLE IF NOT EXISTS` only, and `ALTER TABLE` appears nowhere), so a new column would silently never exist on an existing database.
- `SeoFallback` is the single source of truth for effective title/description/image. `ReadingPageService` (public HTML) and `SeoAuditService` (admin preview) must both call it, or the preview drifts from the real page. `og:image` precedence: `post_seo.og_image_url` → `blog_post.cover_url` → site logo.
- Outbound HTTP uses the JDK `java.net.http.HttpClient`. The repo has no `RestTemplate`/`WebClient`/`RestClient` and no outbound HTTP dependency — do not add one.
- Auto-generation on publish is fire-and-forget: `SeoAutoGenerationTrigger` is a separate bean (so `@Async` is not self-invoked) and dispatches from `afterCommit` so the worker never reads an uncommitted post. Every layer swallows exceptions, so SEO can never fail a post save.
- Neither `SeoGenerationService` nor `ReadingPageService` has a class-level `@Transactional`; a model call must never hold a DB connection, and the reader path must keep its 404-becomes-500 guarantee.
- **Sampling parameters are provider-aware** (`OpenAiCompatibleLlmClient.applySampling`): DeepSeek + thinking mode sends `thinking:{type:"enabled"}` and `reasoning_effort` and **never sends `temperature`** (the upstream silently ignores temperature/presence_penalty/frequency_penalty in thinking mode, so sending it only fakes control). Otherwise `temperature: 0.4` is sent. `max_tokens` is only sent when `llmMaxTokens > 0`.
- **`response_format: json_object` requires the literal word `json` in the prompt.** DeepSeek enforces this outside its public docs and returns 400 `Prompt must contain the word 'json' in some form...` otherwise. `complete()` therefore runs `withJsonHint(...)` first: if no message contains `json` (case-insensitive) it appends `请以 JSON 对象的形式输出结果。` to the last message. The 400-retry branch still sends the original messages so the hint is never appended twice.
- Upstream error mapping: `402` → balance exhausted, `422` → parameters rejected by the model; 401/403/429/404/5xx keep the generic safe Chinese messages. Keys are never echoed.
- Tests never touch the network: they start a `com.sun.net.httpserver.HttpServer` stub (`StubLlmServer`).

## AI features beyond SEO (v2.0.28)

Nine `POST /api/admin/ai/**` endpoints behind `hasRole('admin')`, all returning `Result<T>`. **None of them writes a business table** except comment moderation; generated text is always a suggestion the author must apply.

| Endpoint | Returns |
|----------|---------|
| `POST /api/admin/ai/writing/summary` | `AiSummaryVO` — 80–200 char Chinese summary |
| `POST /api/admin/ai/writing/tags` | `AiTagsVO` — 3–6 tags **picked only from the existing tag pool** (top 60 by usage); out-of-pool names are discarded, all discarded → 502, empty pool → 400 |
| `POST /api/admin/ai/writing/titles` | `AiTitlesVO` — exactly 3 distinct ≤60-char titles |
| `POST /api/admin/ai/moderation/check` | `AiModerationVO` — `verdict ∈ pass|flag|block`, `reason`, `categories` |
| `POST /api/admin/ai/moderation/scan` | `AiModerationScanVO` — scans only `status=1` guest comments; `limit` default 20, cap 50 |
| `POST /api/admin/ai/proofread/{postId}` | `AiProofreadVO` — ≤20 issues `{type,excerpt≤120,suggestion≤200,severity}`; body >6000 chars truncated with `truncated=true` |
| `POST /api/admin/ai/proofread/batch` | `AiProofreadBatchVO` — serial, per-post failure isolation (`limit` default 5, cap 10) |
| `POST /api/admin/ai/images/alt` | `AiImageAltVO` — `alt` (≤120) + `caption` (≤80) from a cover or album photo, image inlined as base64 |
| `POST /api/admin/ai/health/report` | `AiHealthReportVO` — site-wide `score` 0–100 + 3–8 `findings`; one single call over metadata only (no post bodies) |

Notes that matter when changing this area:

- `AiCommentModerationTrigger` is a **separate bean** with `@Lazy self`, dispatching from `afterCommit` into `@Async("taskExecutor")`. Self-invoking `@Async` bypasses the proxy and would make a guest comment submission wait for the model. A verdict other than `pass` flips the row to `status=0` (pending). When `llmCommentModeration` is off or the model is unconfigured, nothing is registered at all.
- `AiModelCaller` has **no `@Transactional`**: a model call must never hold a DB connection. On invalid model output it re-prompts once with the parse error; a second failure becomes `BusinessException(502, "模型返回的内容不符合要求，请重试或手动填写")`. Upstream errors are never retried.
- **Image capability must be predicted from `/models`, not from the upstream response.** A real DeepSeek text-only model (`deepseek-v4-pro`) answers HTTP 200 with `{"error": "无法读取图片..."}` inside `content` rather than an HTTP error. So: `listModels` failure → fail-**open** (still call `completeVision`, a self-hosted gateway may lack `/models`); `listModels` success and the selected model has `supportsImage=false` → fail-**closed** 502 without spending an upstream call.
- `AiImageLoader` caps inline images at **4 MiB** (400 「图片过大，请先压缩后再试」) and only accepts site-relative `/uploads/...` paths or absolute http(s) URLs. Album photo paths must resolve inside `galleryDir`.
- `album_photo.caption` had **no write path at all** before v2.0.28 (the column and the VO existed, upload only wrote file_path/mime_type/sort_order). It is now written by `PUT /api/admin/albums/{albumId}/photos/{photoId}` (`PhotoCaptionForm`, cap 500 matching `caption VARCHAR(500)`; `null`/blank clears).
