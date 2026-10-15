# SelfBlog — agent notes

## Scope and existing rules

- Two independent projects: `backend/` (Spring Boot/MyBatis Plus) and `frontend/` (Vue/Vite). There is no root build manifest or npm workspace; run commands inside the relevant project.
- Preserve the rules in root `CLAUDE.md` and `frontend/CLAUDE.md`: before backend edits, read the complete related Controller → Service → Mapper → Entity chain; update affected README files and `CHANGELOG.md` for code changes. Their version/setup details can be stale; use manifests and configuration as authority.

## Backend: run and verify

Requires JDK 21; there is no Maven wrapper in the repo, and on this machine `mvn` is **not on `PATH`** (`MAVEN_HOME` points at an empty `D:\Javawebexe`, and `JAVA_HOME` points at a JDK 25 the project does not target). Commands below run from `backend/`. Verified working invocation:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$mvn = 'C:\Users\Zora\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd'
& $mvn test -Dskip.npm=true -Dskip.installnodenpm=true
```

- **Always pass `-Dskip.npm=true -Dskip.installnodenpm=true` for backend test runs.** `pom.xml` wires `frontend-maven-plugin`, which otherwise runs `npm ci` + `npm run build` in `../frontend` on every build (bound to `initialize`/`generate-resources`). With the skips a single test class finishes in ~8s; without them the whole frontend is rebuilt first.
- **Consequence: a plain `mvn test` mutates frontend build output.** Never run it while frontend files are mid-edit.
- Baseline as of 2026-10-15: `mvn test` = **386 tests, 0 failures, BUILD SUCCESS**. Never leave the suite below that.
- The plugin provisions its own Node **v24.15.0** at `backend/target/frontend/node/` (`node.exe`, `npm.cmd`, `npx.cmd`, npm 11.12.1). This is the Node that satisfies `engine-strict`; see the frontend section.

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
- **Testing an admin controller with `@WebMvcTest` needs `@MockBean JwtUtils` plus an authenticated user** (both verified empirically). The slice pulls in `SecurityConfig`, whose `jwtAuthenticationFilter` requires `JwtUtils` — without the mock the context fails with `NoSuchBeanDefinitionException`. Method-level `@PreAuthorize` *is* enforced in the slice: role `admin` → 200, any other user → 403, anonymous → 403. The working recipe is `@MockBean JwtUtils` + a per-test `@WithMockUser(roles = "admin")`; do **not** reach for `@AutoConfigureMockMvc(addFilters = false)`, which was measured to return 401 rather than bypassing authorization.

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

- **A satisfying Node already exists in the repo tree.** The backend's `frontend-maven-plugin` provisions Node **v24.15.0** + npm **11.12.1** at `backend/target/frontend/node/` (`node.exe`, `npm.cmd`, `npx.cmd`). Prefix `PATH` with that directory to run any npm script under a Node that clears `engine-strict` — e.g. `$env:PATH = "C:\…\backend\target\frontend\node;$env:PATH"; npm install --dry-run` reports `up to date` with **no** `EBADENGINE`, while the machine's own Node 24.14.0 fails the gate. Prefer this over `--engine-strict=false`.
- `node_modules` in this checkout was originally installed on Linux, so Windows lacked the `.bin/*.cmd` shims and win32 native packages; that has since been repaired in place (`@rollup/rollup-win32-x64-msvc`, `@esbuild/win32-x64`), and `package.json`/`package-lock.json` were left untouched. `npm run` scripts work.

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
