# SelfBlog — agent notes

Entrypoint for agents. Two independent projects: `backend/` (Spring Boot 3.3 + MyBatis Plus + Java 21) and `frontend/` (Vue 3.5 + Vite 6 + Pinia). There is no root build manifest or npm workspace; run commands inside the relevant project.

## Read the detailed notes first

- Touching `backend/` → read **`docs/agents/backend.md`**: verified Maven Wrapper commands, H2/MySQL/SQLite schema rules, change boundaries, reader HTML/discovery/JSON-LD rules, post-publishing and unique-constraint fixes.
- Touching `frontend/` → read **`docs/agents/frontend.md`**: provisioned-Node setup, all gates, jsdom/`?raw` testing traps, dark-mode and GFM contracts.
- Preserve the rules in root `CLAUDE.md` and `frontend/CLAUDE.md`: before backend edits, read the complete related Controller → Service → Mapper → Entity chain; update affected README files and `CHANGELOG.md` for code changes. Their version/setup details can be stale; use manifests and configuration as authority.

## Fast path

```powershell
# Backend tests (repository Maven Wrapper; quote the -D args in PowerShell)
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
backend\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"

# Frontend gates (prefix the Node provided by the backend build to satisfy engine-strict)
$env:PATH = 'C:\…\SelfBlog\backend\target\frontend\node;' + $env:PATH
cd frontend
npm run lint:check; npm run format:check; npm run type-check; npm run test:run; npm run test:deployment; npm run build
```

- **Always pass `-Dskip.npm=true -Dskip.installnodenpm=true` on backend runs.** Otherwise `frontend-maven-plugin` runs `npm ci` + `npm run build` in `../frontend` first, and a plain `mvn test` mutates frontend build output. Never run it while frontend files are mid-edit.
- Never leave the suites below baseline: backend **1004 tests / 0 failures**, frontend **47 files / 477 tests** plus `lint:check`, `format:check`, `type-check`, `test:deployment` (27/27) and `build` green (2026-10-22).

## CI (`.github/workflows/ci.yml`)

Four jobs, all real executions — keep them green:

- `backend`: full suite on H2 (Temurin 21, `./mvnw`, the two skip flags).
- `frontend`: `npm ci` (Node 24.15.0) + lint/format/type/test/deployment/build.
- `mysql`: initializes real MySQL 8 from `backend/schema-fresh-install.sql`, then runs the full suite against it (`DB_TYPE=mysql`, `SPRING_SQL_INIT_MODE=never`). This is the only verified real-MySQL run available to this repo — the local machine has no MySQL/Docker daemon.
- `docker`: builds `backend/Dockerfile` with the repository-root context, starts the container on SQLite, waits for `/actuator/health`, and greps the server-rendered home HTML for `seo-reading`.

## Project-wide records

- `frontend/vercel.json` is a **proxy generator**, not a static frontend (`framework: null` + `scripts/build-vercel-proxy.mjs` → Build Output v3 forwarding to one origin; invalid/self-referencing origins fail the build). `npm run test:deployment` covers both adapters.
- MySQL fresh installs use `backend/schema-fresh-install.sql` (renamed from `schema.sql` on 2026-10-17 because it contains `DROP TABLE`); existing databases only run `migration-selfblog.sql`. Docker Compose mounts the renamed file. `backend/entrypoint.sh` and the unused `backend/.dockerignore` were deleted (build context is the repository root).
- Verification evidence lives in `docs/reviews/` (page-management, SEO/deployment) and `docs/decisions/` (ADRs). Known unverified limits: a real Vercel deployment and search-engine indexing have never been exercised; treat runtime metadata as metadata, not as proof either happens.
- Dead-code history and the 2026-10-17 frontend legacy cleanup (47 files across two waves) are recorded in `docs/agents/frontend.md`; backend community endpoints/tables are intentionally retained as an admin-only API surface.
