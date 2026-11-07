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
- Test baseline as of 2026-11-07: `npx vitest run` = **58 files / 609 tests**, all passing (~25s). `lint:check` (0 errors / 42 warnings), `format:check`, `type-check`, `test:deployment` (27/27) and `build` are all green at the same commit; never leave any of them below that.
- **jsdom/sandbox trap on this machine**: vitest and `npm run test:deployment` (node:test) `mkdtemp`/`mkdir` under the OS temp dir can fail with `EPERM` when run from a sandboxed shell. Point `TMP`/`TEMP`/`TMPDIR` at a workspace-local directory (e.g. `frontend/.tmp-vitest`, removed afterwards) and re-run — it is environmental, not a test failure.

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
- `index.html` now has **two** pre-paint inline scripts, and their order is load-bearing: `[0]` theme bootstrap (`edu_theme`), `[1]` author-appearance forwarder (`edu_site_appearance`). The forwarder is a **zero-logic whitelist relay** — it only `setProperty`s values already composed by `stores/site.ts`'s `buildBackgroundVars()`, never composes CSS itself. Its whitelist and storage key are pinned to the `PREPAINT_APPEARANCE_VARS` / `APPEARANCE_STORAGE_KEY` exports, so renaming on either side fails the suite instead of silently breaking first paint. `bootScript()` matches the first script only; `appearanceScript()` indexes `inlineScripts()[1]` on purpose so an ordering regression fails loudly.
- **Do not run `prettier --write` on `frontend/index.html`.** `format:check` globs only `src/**/*.{ts,tsx,vue,css,json}`, so `index.html` is not covered by the gate — and it was already not Prettier-clean at HEAD. Formatting it produces a large unrelated reflow diff.
- Markdown rendering must stay at GFM parity with the backend (`SafeMarkdown`): the shared fixture `docs/fixtures/gfm-sample.md` is asserted from both `src/components/pages/__tests__/markdownGfm.test.ts` and `backend`'s `SafeMarkdownTest`. DOMPurify forbids `input`, so task-list checkboxes disappear on the browser side while item text remains.

## 2026-11-07 batch (v2.0.30)

- **`marked.setOptions({ gfm: true, breaks: true })` lives at module top-level of `src/utils/markdown.ts` now; `main.ts` no longer imports `marked`.** Consequence: the 66 KB markdown chunk is NOT modulepreloaded in `dist/index.html` — Home/Search/Archives/Tag never parse it on first paint. Do not move the config back into an eagerly-loaded module or the chunk rejoins the critical path.
- Appearance keys grew 11 → **16**: `readingProgress` (`on|off`, PostDetail renders the progress bar via a reactive `v-if` reading `siteStore.settings` — deliberately NOT in `App.vue`'s watch array and NOT a `data-ap-*` attr), plus four pure-CSS enums (`blockquoteStyle`, `postRowStyle`, `contentImageStyle`, `tableStyle`) that ARE in the watch array (now 18 entries) as `apBlockquote`/`apPostRow`/`apContentImage`/`apTable`. All five stay out of `PREPAINT_APPEARANCE_VARS`; default tier has zero CSS rules. `blockquoteStyle=accent` deliberately out-specifies PostDetail's `:deep` blockquote override to unify both markdown rendering sites.
- PostEdit's AI card: selection-writing actions (polish/continue/shorten) work on **unsaved** drafts and capture the textarea selection synchronously before the `await` (with a changed-content guard that refuses to splice after edits during the request); internal-link suggestions (saved posts only) insert `[anchor](/post/{id})` and disable after use. New `llmApi.inlineWriting`/`suggestLinks` follow the `_noRetry` + 130 s convention.
- Both `recordVisitor()` call sites in `App.vue` skip when `visitorTrackingEnabled === 'false'` (server enforces the same setting; the client check only saves a round trip).
- `ArticleList` renders read minutes from a `computed` Map instead of calling `estimateReadMinutes` in the template per row per render; Navbar's resize listener is rAF-coalesced with the frame cancelled in `onBeforeUnmount` (Navbar itself remains a do-not-restyle zone — JS only).

## 2026-11-07 batch 2 (v2.0.30)

- **Archives is a single `getArchives()` call** against the compact 6-column backend projection (`types/post.ts` `PostArchiveItem`) — the old five-100-item crawl is gone; grouping (`createTime.slice(0,7)`, `undated` fallback, key-descending) and visuals unchanged. Pinned by `PublicPages.test.ts` ("getArchives called exactly once, getPostList never").
- **PostDetail renders a 相关阅读 section** (`api/post.ts getRelated`) between the markdown body and the adjacent nav — non-empty only, skipped in preview, silent failure. Rows are `router-link`s to `/post/{id}`; the markdown-body click interception does NOT cover this section.
- PostEdit's selection row gained **翻译** (`runInlineWriting('translate')`), same replace-selection flow/guards as polish/shorten; `InlineWritingAction` is now a 4-value union.
- Appearance keys 16 → **18**: `homeSidebarOrder` (Home wraps the three sidebar blocks in a `<template v-for>` over the order split; default `author-tags-archive` renders byte-identical DOM; per-block `showModule` conditions unchanged) and `tocStyle` (`hidden` hides aside + MobileTocSheet + FAB and reuses the **existing** `reader-layout--no-toc` grid modifier — zero new CSS). Both are template/logic-driven: not in `applyCustomizations`, the watch array or `PREPAINT_APPEARANCE_VARS`.

## 2026-11-07 batch 3 (v2.0.30)

- **PostDetail 「摘要」 callout** (`summaryCallout === 'on'` + non-blank summary + not preview): plain-text interpolation between cover and body — never v-html; default `off` renders no node.
- **Search typeahead**: `api/post.ts getSearchSuggestions` + a 300 ms trailing debounce in `Search.vue` feeding a `role="listbox"` dropdown; ↓/↑ wrap-around, Enter reuses `submitSearch()` (no duplicated search logic), Esc/blur close with `mousedown.prevent` options. The backend suggest route has a dedicated 120/min read budget (`/api/post/search`) — do not fold it back into the shared `/api/post` bucket, typeahead exhausts 60/min in ~18 s.
- **`linkStyle`** (`data-ap-link`, section 13 of customize.css): `dotted` = always-on underline goes dotted; `flat` = no rest-state underline, restored on hover, `.heading-anchor` excluded so heading `#` marks keep their no-underline design. App.vue watch array is 19 entries (`summaryCallout` deliberately absent — PostDetail v-if).
- **4 new theme presets** (墨韵/杏暖/青苔/暮色) — presets panel shows 8 cards; each preset writes only existing keys. Appearance panel: **20 keys**.

## 2026-11-07 batch 4 (v2.0.30, borrowed from Hexo/Ghost)

- **`highlight.js@11.12.0` is the repo's only new frontend dependency.** It lives in `useMarkdownEnhance` (`highlight.js/lib/common` — never the full build), gated on `hljs.getLanguage` (NO auto-detect), applied **before** collapse measurement, keyed off `textContent` for copy, and registered with no disposer (v-html replacement discards hljs classes). It ships in its own async chunk (~56 KB gzip; entry unchanged — verify with a build before/after touching the import graph). The github-dark theme provides token colors ONLY: `.markdown-body pre code.hljs` overrides the theme background to keep the `--code-bg/--code-text` always-dark surface (typography.css's `pre` meanwhile switched from the wrong `--bg-secondary` surface to the code tokens). Client-only enhancement: `utils/markdown.ts` and the SafeMarkdown parity fixture are untouched.
- **`postLicense`** (site setting, ≤200 chars server-side, empty = off): PostDetail renders a license footer + self-permalink between `.markdown-body` and the related section; the permalink origin comes from the exported `canonicalOrigin()` in `useSeoMeta.ts`.
- **Ctrl/Cmd+K / `/` → `/search`**: judgment in the pure `utils/searchShortcut.ts::shouldOpenSearch` (editable targets, `event.repeat`, modifier+`/` all filtered); App.vue only wires the listener + one `router.push`. Search.vue focuses its input on mount.
- Feeds are full-text server-side — no frontend involvement; see `docs/agents/backend.md`.

## Dead code removed (2026-10-17)

- Deleted the never-routed frontend circle/topic trees and other unreferenced files (**22 files**): `router/modules/circle.ts`, `views/circle/*`, `components/circle/*`, `views/admin/{CircleManagement,TopicsManagement}.vue`, `views/{topic/TopicDetail,trending/TrendingPage,auth/PasswordReset,auth/Register}.vue`, `components/home/*`, `components/common/Card.vue`, `composables/usePagination.ts`, `types/vo.ts`, `api/topic.ts`, `types/topic.ts`, plus the now-dead circle/topic API helpers and types in `api/admin.ts`/`types/index.ts`. Backend community endpoints and tables are intentionally retained (admin-only API surface).
- Deleted the second, legacy community UI set (**25 files**): `api/{circle,collect,comment,follow,like,report}.ts`, `types/{circle,follow}.ts`, `components/common/{CollectButton,FollowButton,LikeButton}.vue`, `views/admin/{Reports,Statistics,Users}.vue`, `views/common/Report.vue`, the ten `views/user/*` pages except `PasswordChange.vue` (which is kept and linked from the admin settings page). `api/user.ts`, `stores/user.ts` and `types/comment.ts` stay.
- Both deletion waves are frontend-only; no runtime route or API changed. Root `CLAUDE.md`'s "known issues" line about the unregistered circle route is obsolete but the file carries user-owned edits and was left untouched (except the necessary `schema.sql` rename).
