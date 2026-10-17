# Frontend agent notes

Applies to `frontend/` (Vue 3.5 + Vite 6 + Pinia + Vitest). `AGENTS.md` is the entrypoint; this file holds the verified commands and frontend-specific rules. Commands run from `frontend/`.

## Run and verify

`.npmrc` sets `engine-strict=true`, and the locked tree requires Node `^22.22.2 || ^24.15.0 || >=26.0.0` (from `abbrev@5.0.0`, reached via `@vue/test-utils` → `js-beautify` → `nopt`). Anything below that — including Node 24.0–24.14 — hard-fails `npm ci`/`npm install` with `EBADENGINE`; `npm ci --engine-strict=false` bypasses the gate for local work.

- **A satisfying Node exists in the repo tree (after a Maven build).** The backend's `frontend-maven-plugin` provisions Node **v24.15.0** + npm **11.12.1** at `backend/target/frontend/node/` (`node.exe`, `npm.cmd`, `npx.cmd`). Prefix `PATH` with that directory to run any npm script under a Node that clears `engine-strict`:

  ```powershell
  $env:PATH = 'C:\…\SelfBlog\backend\target\frontend\node;' + $env:PATH
  npm run test:run
  ```

  If that directory is missing (fresh `target/` cleanup), run a Maven build once to re-provision it, or call `node_modules/.bin/<tool>` directly.
- On this machine a bare `npm run <script>` fails on the default PATH (verified 2026-10-16): the system PATH contains an unmatched quote (`C:\Program Files\Java\jdk-25\bin"`), which derails `cmd.exe`'s PATH parsing so the `.bin` shims cannot resolve bare `node`. Prefix the provisioned Node directory as above (what all verified commands do), or call the shims directly.
- `node_modules` was reinstalled on Windows with the provisioned Node (`npm ci`), so the `.bin/*.cmd` shims and win32 native packages (`@rollup/rollup-win32-x64-msvc`, `@esbuild/win32-x64`) are present.
- Test baseline as of 2026-10-17: `npx vitest run` = **43 files / 400 tests**, all passing (~19s). `lint:check` (0 errors / 43 warnings), `format:check`, `type-check`, `test:deployment` (27/27) and `build` are all green at the same commit; never leave any of them below that.

```sh
npm ci
npm run dev
npm run lint:check
npm run format:check
npm run type-check
npm run test:run
npm run test:run -- src/utils/__tests__/crypto.test.ts
npm run test:deployment
npm run build
```

- `lint` auto-fixes all `src`; `format` rewrites files. Use the `*:check` scripts for non-mutating verification. `format:check` **is a passing gate now** (2026-10-17): `.prettierrc` sets `endOfLine: "auto"` so CRLF working trees pass, and the drift was rewritten once. `build` runs `vue-tsc --noEmit` before Vite; `type-check` is the focused check.
- `tsconfig.json` enables `strict` **and `noUncheckedIndexedAccess`**: indexed access yields `T | undefined`, so `arr[0].foo` fails `type-check`. Use `arr[0]?.foo`, or drop the index (e.g. `url.replace(/[?#].*$/, '')` instead of `url.split('?')[0]`). `type-check` is a required gate, not advisory.
- `tsconfig.json` sets `"types": ["vite/client"]` and there is no `@types/node` in the dependency tree: `import … from 'node:fs'` fails `type-check` with TS2591. To read a file in a test, use a Vite `?raw` import instead (declared by `vite/client`), e.g. `import html from '../../index.html?raw'`.
- `import.meta.url` inside jsdom tests is not a `file:` URL (Vitest rewrites it to the jsdom origin), so `fileURLToPath(new URL(…, import.meta.url))` throws `The URL must be of scheme file`. This is the second reason to prefer `?raw`.
- Files outside `frontend/` (e.g. the shared fixture `docs/fixtures/gfm-sample.md`) need `server.fs.allow: ['..']` in `vite.config.ts` before a `?raw` import of them works; the entry is already present.
- `npm test` starts watch mode; `test:run` exits. Vitest configuration lives in `vite.config.ts`, uses jsdom, and loads `src/test/setup.ts` (router/matchMedia mocks). Coverage/UI scripts exist, but `@vitest/coverage-v8` and `@vitest/ui` are not installed in the manifest/lockfile.
- jsdom testing conventions: jsdom has no real `requestAnimationFrame`, so stub it with a queue you flush explicitly — a synchronous rAF stub cannot observe per-frame coalescing, and `Object.defineProperty(window, 'scrollY', …)` is required before dispatching `scroll`. Any test mounting a component that registers global listeners must call `enableAutoUnmount(afterEach)` from `@vue/test-utils`, because `src/test/setup.ts` does not. Prefer a real `createMemoryHistory()` router over `stubs: { RouterLink: true }`: the stub renders `<router-link-stub>` and drops slot content unless `renderStubDefaultSlot` is on, and it cannot express real active-route behavior.
- Dev runs on port 3000. `VITE_API_TARGET` controls `/api` and `/uploads` proxying; both paths are forwarded unchanged (no `/uploads` → `/api/uploads` rewrite). Axios hardcodes `/api` in `src/api/index.ts`; the advertised `VITE_API_BASE_URL` is not consumed there. Production proxying is configured separately in `vercel.json`/the reverse proxy.
- The SPA renders from `src/stores/site.ts` defaults with no backend running (public API calls just log 500s), so `npm run dev` alone is enough to verify layout/CSS in a browser. Playwright is installed globally (`npm root -g`); launch it with `chromium.launch({ channel: 'chrome' })` — the `ms-playwright` browser cache predates the global Playwright version and `chromium.launch()` alone fails with "Executable doesn't exist".

## Integration traps

- Use `src/api/` modules: the shared Axios interceptor returns the backend response envelope, not `AxiosResponse`; `response.data` is the business payload. It also owns queued token refresh and auth-failure cleanup of Pinia/storage.
- `src/main.ts` restores auth state before installing the router; preserve this order. Routes are assembled from `src/router/modules/`, with guards in `src/router/guards.ts`.
- `src/styles/main.css` is only the import entrypoint; design tokens live in `variables.css`, dark overrides in `dark-mode.css`. Confirmations use `src/composables/useConfirm.ts`; render its returned `ConfirmDialog` component as well as calling `confirm()`.
- Dark mode is keyed on `html.dark-mode`. `frontend/index.html` runs a tiny inline bootstrap script **before** Vue mounts (and before first paint) that reads `localStorage['edu_theme']` and adds the class; the key/values must stay in sync with `src/stores/theme.ts` (`light | dark | system`, default `light`). `src/__tests__/indexHtml.test.ts` executes the real script from `index.html?raw` and pins that contract. Keep the script inline (no CSP header blocks it) and keep the storage access inside `try/catch`.
- Markdown rendering must stay at GFM parity with the backend (`SafeMarkdown`): the shared fixture `docs/fixtures/gfm-sample.md` is asserted from both `src/components/pages/__tests__/markdownGfm.test.ts` and `backend`'s `SafeMarkdownTest`. DOMPurify forbids `input`, so task-list checkboxes disappear on the browser side while item text remains.

## Dead code removed (2026-10-17)

- Deleted the never-routed frontend circle/topic trees and other unreferenced files (**22 files**): `router/modules/circle.ts`, `views/circle/*`, `components/circle/*`, `views/admin/{CircleManagement,TopicsManagement}.vue`, `views/{topic/TopicDetail,trending/TrendingPage,auth/PasswordReset,auth/Register}.vue`, `components/home/*`, `components/common/Card.vue`, `composables/usePagination.ts`, `types/vo.ts`, `api/topic.ts`, `types/topic.ts`, plus the now-dead circle/topic API helpers and types in `api/admin.ts`/`types/index.ts`. Backend community endpoints and tables are intentionally retained (admin-only API surface).
- Deleted the second, legacy community UI set (**25 files**): `api/{circle,collect,comment,follow,like,report}.ts`, `types/{circle,follow}.ts`, `components/common/{CollectButton,FollowButton,LikeButton}.vue`, `views/admin/{Reports,Statistics,Users}.vue`, `views/common/Report.vue`, the ten `views/user/*` pages except `PasswordChange.vue` (which is kept and linked from the admin settings page). `api/user.ts`, `stores/user.ts` and `types/comment.ts` stay.
- Both deletion waves are frontend-only; no runtime route or API changed. Root `CLAUDE.md`'s "known issues" line about the unregistered circle route is obsolete but the file carries user-owned edits and was left untouched (except the necessary `schema.sql` rename).
