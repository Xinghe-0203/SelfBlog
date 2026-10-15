# SelfBlog — agent notes

## Scope and existing rules

- Two independent projects: `backend/` (Spring Boot/MyBatis Plus) and `frontend/` (Vue/Vite). There is no root build manifest or npm workspace; run commands inside the relevant project.
- Preserve the rules in root `CLAUDE.md` and `frontend/CLAUDE.md`: before backend edits, read the complete related Controller → Service → Mapper → Entity chain; update affected README files and `CHANGELOG.md` for code changes. Their version/setup details can be stale; use manifests and configuration as authority.

## Backend: run and verify

Requires JDK 21 and installed Maven; there is no Maven wrapper. Commands below run from `backend/`.

```sh
mvn spring-boot:run
mvn test
mvn test "-Dtest=AuthControllerTest"
mvn test "-Dtest=AuthControllerTest#sendRegisterCode_Success_Returns200"
mvn clean package -DskipTests
```

- For local runtime, copy `.env.example` to `.env`, set `JWT_SECRET` (at least 32 characters) and `ADMIN_PASSWORD` (at least 12 characters), and retain the supplied JWT expiration values. The template selects `DB_TYPE=sqlite`, not MySQL. SQLite initializes from `src/main/resources/schema-sqlite.sql` and defaults to `campus_blog.db` in the working directory.
- `config/DotenvConfig.java` parses `.env` with Java `Properties`, not shell dotenv: use unquoted values. It searches the working directory, then the user's home directory. File values become JVM properties, outranking OS environment variables; existing JVM `-D` properties win.
- MySQL requires `DB_TYPE=mysql`, all `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD` values, and startup validation requires a password of at least 8 characters. Use `schema.sql` only for a new empty database; migrate an existing database with `backend/migration-selfblog.sql`. Docker Compose explicitly sets `DB_TYPE=mysql`.
- Tests use H2 via `src/test/resources/application.yml`, optional `application-test.yml`, and test `schema.sql`; test request paths omit `/api`. Keep `DB_TYPE` unset for H2: a runtime database selection activates `DatabaseConfig` and replaces the test datasource, even with the `test` profile.
- Runtime API controllers are prefixed with `/api` by MVC; the SPA is served at `/`, Knife4j is `/doc.html`, and Actuator health is `/actuator/health` (API docs disabled under `prod`). Tests clear the MVC API prefix and use unprefixed request paths.

## Backend: change boundaries

Java source root: `backend/src/main/java/com/selfblog/`.

- Post/circle CRUD services delegate queries and interactions to `PostQueryService`/`PostInteractionService` and `CircleQueryService`/`CircleInteractionService`; modify the owning service rather than duplicating logic in the facade.
- Use `common.result.Result<T>` and `common.exception.BusinessException`; obtain identity through `utils.SecurityUtils` (`getCurrentUserId()` requires login; `getCurrentUserIdOrNull()` allows guests). Public endpoints also need an explicit rule in `config/security/SecurityConfig.java`.
- User articles and circle posts publish directly with `status=1`; do not introduce a pending-review gate despite the existing moderation APIs/status fields.
- Preserve service transaction conventions: writes use `@Transactional(rollbackFor = Exception.class)`, reads use `@Transactional(readOnly = true)`.
- Database changes must account for MySQL `backend/schema.sql`, SQLite `src/main/resources/schema-sqlite.sql`, and H2 `src/test/resources/schema.sql`. SQLite initialization uses `CREATE TABLE IF NOT EXISTS`, so schema edits do not migrate existing files.
- Mapper XML supports `databaseId="mysql"`/`"sqlite"`; H2 has no mapped database ID and uses untagged fallback statements. Do not put MySQL-only SQL in shared statements. Custom SQL must handle soft deletion explicitly (`is_deleted=0`); generated MyBatis Plus queries use `isDeleted`/`@TableLogic`.

## Frontend: run and verify

Commands below run from `frontend/`. `.npmrc` sets `engine-strict=true`, and the locked tree requires Node `^22.22.2 || ^24.15.0 || >=26.0.0` (from `abbrev@5.0.0`, reached via `@vue/test-utils` → `js-beautify` → `nopt`). Anything below that — including Node 24.0–24.14 — hard-fails `npm ci`/`npm install` with `EBADENGINE`; `npm ci --engine-strict=false` bypasses the gate for local work.

```sh
npm ci
npm run dev
npm run lint:check
npm run format:check   # NOT a passing gate: already fails on ~96 files at baseline
npm run type-check
npm run test:run
npm run test:run -- src/utils/__tests__/crypto.test.ts
npm run build
```

- `lint` auto-fixes all `src`; `format` rewrites files. Use the `*:check` scripts for non-mutating verification **except `format:check`**, which already fails on ~96 files at baseline (`core.autocrlf=true` writes CRLF into the working tree while `.prettierrc` sets `endOfLine: "lf"`, plus pre-existing drift) — treat a non-zero exit as noise, not as your bug. To check only what you touched, run `npx prettier --check --end-of-line auto <files>`; never run the repo-wide `format` script, which rewrites ~96 unrelated files. `build` runs `vue-tsc --noEmit` before Vite; `type-check` is the focused check.
- `tsconfig.json` enables `strict` **and `noUncheckedIndexedAccess`**: indexed access yields `T | undefined`, so `arr[0].foo` fails `type-check`. Use `arr[0]?.foo`, or drop the index (e.g. `url.replace(/[?#].*$/, '')` instead of `url.split('?')[0]`). `type-check` is a required gate, not advisory.
- `npm test` starts watch mode; `test:run` exits. Vitest configuration lives in `vite.config.ts`, uses jsdom, and loads `src/test/setup.ts` (router/matchMedia mocks). Coverage/UI scripts exist, but `@vitest/coverage-v8` and `@vitest/ui` are not installed in the manifest/lockfile.
- jsdom testing conventions: jsdom has no real `requestAnimationFrame`, so stub it with a queue you flush explicitly — a synchronous rAF stub cannot observe per-frame coalescing, and `Object.defineProperty(window, 'scrollY', …)` is required before dispatching `scroll`. Any test mounting a component that registers global listeners must call `enableAutoUnmount(afterEach)` from `@vue/test-utils`, because `src/test/setup.ts` does not. Prefer a real `createMemoryHistory()` router over `stubs: { RouterLink: true }`: the stub renders `<router-link-stub>` and drops slot content unless `renderStubDefaultSlot` is on, and it cannot express real active-route behavior.
- Dev runs on port 3000. `VITE_API_TARGET` controls `/api` proxying and `/uploads` → `/api/uploads` rewriting. Axios hardcodes `/api` in `src/api/index.ts`; the advertised `VITE_API_BASE_URL` is not consumed there. Production proxying is configured separately in `vercel.json`/the reverse proxy.
- The SPA renders from `src/stores/site.ts` defaults with no backend running (public API calls just log 500s), so `npm run dev` alone is enough to verify layout/CSS in a browser. Playwright is installed globally (`npm root -g`); launch it with `chromium.launch({ channel: 'chrome' })` — the `ms-playwright` browser cache predates the global Playwright version and `chromium.launch()` alone fails with "Executable doesn't exist".

## Frontend: integration traps

- Use `src/api/` modules: the shared Axios interceptor returns the backend response envelope, not `AxiosResponse`; `response.data` is the business payload. It also owns queued token refresh and auth-failure cleanup of Pinia/storage.
- `src/main.ts` restores auth state before installing the router; preserve this order. Routes are assembled from `src/router/modules/`, with guards in `src/router/guards.ts`.
- `src/styles/main.css` is only the import entrypoint; design tokens live in `variables.css`, dark overrides in `dark-mode.css`. Confirmations use `src/composables/useConfirm.ts`; render its returned `ConfirmDialog` component as well as calling `confirm()`.
