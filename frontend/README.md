# SelfBlog - Frontend

Vue 3 frontend for a single-author personal blog. Public pages focus on reading, search, archives, about, and tags; writing and content management are protected by administrator-only routes.

**Version**: v2.0.26 | **Vue**: 3.5.44 | **Vite**: 6.4.4

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Vue | 3.5.44 | UI framework (Composition API) |
| Vite | 6.4.4 | Build tool |
| Vue Router | 4.6.4 | Routing |
| Pinia | 2.3.1 | State management |
| Axios | 1.20.0 | HTTP client |
| Marked | 12.0.2 | Markdown rendering |
| DOMPurify | 3.4.17 | XSS protection |
| TypeScript | 6.0 | Type safety |
| Vitest | 4.1.11 | Unit tests (jsdom + @vue/test-utils) |

Versions above reflect `package-lock.json`, not the lower bounds of manifest ranges.

## Quick Start

```bash
npm ci              # Install locked dependencies (Node 22.22+ / 24.15+ / 26+; engine-strict)
npm run dev         # Dev server (http://localhost:3000)
npm run build       # Production build (→ dist/)
npm run preview     # Preview production build
npm run lint:check  # Non-mutating ESLint check
npm run test:run    # Unit tests (Vitest, single run)
```

Dev server proxies `/api` requests to `http://localhost:8825`.
For production, Maven builds the Vue assets and embeds them into `backend/target/selfblog.jar`; serving the site does not require a separate Node.js process.

## Project Structure

```
src/
├── api/                # Axios modules + interceptors (index, site, admin, post, media, pages, gallery, …)
├── components/
│   ├── common/         # Reusable UI: Button, Input, Icon, Modal, Toast, ImagePreview, BackToTop, …
│   ├── layout/         # Navbar, Footer, headless MusicPlayer, NavbarMusicControl
│   ├── post/           # ArticleList + MobileTocSheet (shared rows and reading extras)
│   ├── pages/          # PageFrame, MarkdownContent, ContentPageView (page config + safe body)
│   └── admin/          # SiteAssetField (brand asset uploads)
├── composables/        # useDialog, useConfirm, useScrollState, useMarkdownEnhance, useSeoMeta, usePageConfig, usePageSeo, useScrollReveal
├── router/             # Entry, guards, lazy helpers, per-domain modules
├── stores/             # Pinia: site, theme, user, music, pages
├── styles/             # Tokens + base + components + dark mode + a11y (entry: main.css)
├── types/              # TypeScript type definitions
├── utils/              # crypto, logger, toast, visitor, reading
├── views/              # Home / About / Archives + admin(+pages), auth, post, pages, user, gallery, search, tag, common
├── App.vue
└── main.ts
```

## Public and author pages

| Module | Routes |
|--------|--------|
| Public | `/`, `/archives`, `/about`, `/gallery`, `/search`, `/tag/:id`, `/post/:id` |
| Author | `/{adminPath}` (login), `/{adminPath}/write`, `/{adminPath}/edit/:id` |
| Admin | `/{adminPath}/dashboard`, `/{adminPath}/posts`, `/{adminPath}/tags`, plus visitors/comments/albums/settings |

`adminPath` defaults to `/admin` and is loaded from public site settings before the router starts; the author can change it in the protected settings page. Public pages have no login/register links. Registration and legacy community routes are not exposed; anonymous likes and guest comments are available on article pages.

## State Management (Pinia)

### `user` store

```ts
const userStore = useUserStore()
userStore.token          // JWT access token
userStore.isLoggedIn     // Auth status
userStore.login(credentials)
userStore.logout()
userStore.fetchUser()
```

### `theme` store

```ts
const themeStore = useThemeStore()
themeStore.isDarkMode    // Dark mode status
themeStore.toggleTheme() // Toggle light/dark
```

### `site` store

```ts
const siteStore = useSiteStore()
siteStore.settings        // Public site settings (title, logo, favicon, music…)
siteStore.adminPath       // Configurable admin entry
siteStore.applySettings(partial)
await siteStore.loadPublicSettings()
```

## Design System

Compact Glass design foundation: neutral gray-white canvas, system sans typography,
8px controls, 12–14px content groups and 16px panels. The author's primary color and
five selectable skins remain authoritative; soft accent colors derive from that color.
Theme changes settle foreground/background together rather than interpolating unreadable palettes.

Content surfaces and legacy `--glass-*` tokens remain solid. Only functional chrome
opts into `.functional-glass`; unsupported blur, reduced transparency and high contrast
fall back to solid surfaces. Reduced motion removes panel displacement and looping sound bars.

The [approved whole-site redesign](../docs/superpowers/specs/2026-10-16-selfblog-ios-glass-redesign-design.md)
has three batches, all implemented: foundation/navigation/music, the public reading
pages (shared `ArticleList`, rebuilt home/archives/about/404/search/tag/reader/gallery)
and the author workspace (compact admin shell with a focus-managed drawer, unified
page hierarchy, shared album dialog, login card and editor shell).

### CSS Variables

```css
:root {
  --accent: #165DFF;
  --radius: 8px;
  --surface-primary: #FFFFFF;
}
html.dark-mode { /* Dark mode overrides live in dark-mode.css */ }
```

### Legacy glass classes

The `.glass*` / `.water-drops` utilities remain for backward compatibility and do not
enable translucent content cards. New solid content uses `--surface-*`; new functional
glass uses `--functional-glass-*` with a solid fallback.

### Navigation, music and overlays

- Navbar stays 52px high (50px on mobile); scrolling never hides/resizes it. Search, menu and music panels are mutually exclusive and close on Escape/outside input/route changes.
- `App.vue` mounts exactly one headless `MusicPlayer` outside route transitions, including admin routes. `stores/music.ts` owns transport state; `NavbarMusicControl` only presents controls.
- Reuse `musicEnabled/musicUrl/musicTitle`; never autoplay or restore playback intent. Public navigation preserves audio; admin entry pauses. A source change/disable invalidates pending playback and detaches old listeners.
- Only volume/loop preferences are stored. Unknown duration disables seeking; playback failures stay inline; unsupported volume adjustment shows a device-volume hint.
- `#overlay-root` is inside `#selfblog` and outside page transforms. `useDialog` provides a shared stack, top-only Escape, Tab containment, focus restoration and owned scroll locking for Modal/ImagePreview/MobileTocSheet. Notifications also portal there to inherit skins.

### Verification (whole-site redesign, 2026-10-16)

Vitest: **30 files / 328 tests**. Type-check passes; ESLint has no errors (existing warnings only); Vite production build passes. Chrome checked 320/390/768/820/960/1024/1440px, 44px navigation actions, stable heights, Escape/focus, dark mode, reduced motion, overlay layers and real generated-WAV playback/public continuity/admin pause/source replacement. Batches 2–3 were additionally checked at 1440/1024/820/768/390px across eight public routes plus login → seven author screens, the 390px admin drawer, table scrolling and the editor. API data was browser-fixtured; no backend or existing user data was changed.

### Follow-up correction (2026-10-16)

Matched-data Chrome comparison identified and corrected navigation/music drift:
links follow search, the active fill is a 34px capsule within a 44px click target,
and the idle music entry uses static bars plus "听一会儿". Music sliders keep real
values and system-color rails/thumbs in forced-colors mode. Saved brand/theme and
music availability still control presentation.

The mobile sidebar is inert while closed; desktop resizing releases its modal
focus/scroll ownership. Editor dirty state compares complete submitted-content
snapshots (including summary/tags), so success does not cause a false leave warning
and in-flight edits/failed requests remain protected. Draft saves cannot overlap
publication. Tag creation/editing follows the backend's 20-character limit.

Current gates: **31 files / 345 tests**, type-check, ESLint **0 errors / 255 warnings**,
production build. The separate real Spring Boot/SQLite audit (temporary data/uploads,
no API fixtures) now passes **17/17** assertions, including the previously failing
publication warning and tag limit, plus resumed-draft identity and canceled-confirmation
focus regressions from independent review. See the
[full report](../docs/reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md)
for tested workflows and untested deployment environments.

### Configurable public pages

- Author route `/<adminPath>/pages` manages8 built-in public-page/template configurations
  plus published custom pages at `/pages/:slug`. About has a standalone Markdown body.
- `stores/pages.ts` holds published data only; optional manifest loading does not block
  auth restoration or router installation. Preview overrides are injected, never written
  into shared public settings or browser storage.
- `PageFrame` scopes safe accent/background/width/density settings. Defaults preserve the
  existing global proportions; the user canceled whole-site110% scaling.
- Draft editor saves only submitted snapshots, serializes writes and protects new in-flight
  edits. Publication requires a saved revision; stale revisions remain editable after409.
- Preview mounts the real sanitized components in a blank same-origin srcdoc viewport,
  so mobile CSS media queries are evaluated at390px. No public preview URL or weakened
  frame-security headers are needed.
- Navbar measures available width for primary/More links, preserving search/link/tool
  order and44px targets. The mobile menu contains the complete published navigation.
- Runtime metadata clears old article/JSON-LD state and keeps missing/search/404/admin
  pages noindex. Since the crawler batch, the server also returns readable HTML for these
  routes; `useSeoMeta` reads the `selfblog-site-origin` meta to keep canonical/OG URLs on
  the configured public origin instead of the proxy request host.

### Crawlable server HTML and the Vercel proxy (2026-10-16)

- The JAR (not the SPA) now owns the first response for history routes: article/About/custom
  bodies are readable without JavaScript, `#app` carries a semantic reading DOM that Vue
  replaces on mount, and head metadata/status codes are rebuilt per request.
- `useSeoMeta`/`usePageSeo` prefer the server's canonical origin marker; with no marker
  (Vite dev) they fall back to the browser origin, and malformed markers are ignored.
- `vercel.json` **no longer deploys a second static frontend**: `framework: null` plus
  `scripts/build-vercel-proxy.mjs` generate Build Output API v3 routes that forward every
  path/query/method to one backend origin with `no-store` and security headers. Preview
  builds add `X-Robots-Tag: noindex,nofollow`, and invalid/self-referencing origins fail the
  build instead of shipping an empty site.
- `npm run test:deployment` covers the proxy generator and the Docker/Compose source
  contracts with `node:test` (27 assertions). Running the Vite build still produces `dist/`
  for the JAR; nothing about normal development or deployment changed.

Gates for this batch: **41 files / 394 tests**, strict type-check, ESLint **0 errors /
256 warnings**, touched-file format check and production build pass; `npm run test:deployment`
passes 27/27. Real-service SQLite/Chrome evidence (JS-disabled reading, sitemap completeness,
local proxy transport) is recorded in
[the verification record](../docs/reviews/2026-10-16-seo-deployment-verification.md) — it does
not cover a real Vercel deployment or Docker daemon.

### Shared layout conventions

- `components/post/ArticleList.vue` is the single article-row language shared by home, search and tag; it never reserves a cover slot and drops thumbnails that fail to load.
- Home builds its introduction from real site settings; the decorative image is the first usable cover from the first successful response and stays fixed for the visit.
- Reader pages render title/metadata before the optional cover; the side TOC and the mobile sheet share a 1020px breakpoint.
- Admin pages share one heading/hierarchy pattern; `AdminLayout` owns the drawer's focus, Escape and scroll lock, and the album manager reuses the shared `Modal` instead of its own overlay.

## Security

- **XSS**: All user HTML sanitized via DOMPurify before rendering
- **Markdown**: `marked.parse()` → `DOMPurify.sanitize()` pipeline
- **Token**: Access + refresh tokens stored encoded in localStorage/sessionStorage; the Axios layer queues requests and auto-refreshes on 401, clearing auth state when refresh fails
- **Single-author mode**: guests can read/search; registration is disabled; only an administrator can access writing and management routes

## Router Guards

- `requiresAuth` / `requiresAdmin` — unauthenticated visitors go to the configurable admin login (`/<adminPath>`); expired tokens are cleared first
- `requiresAdmin` — non-admin accounts are redirected home with a toast
- `guest` — logged-in users are redirected home

## Build Output

| Asset | Size (gzip) |
|-------|-------------|
| JS | ~150-250 KB |
| CSS | ~30-50 KB |

Optimizations: route-based code splitting, tree shaking, auto-compression, small asset inlining.

## Conventions

1. Vue 3 Composition API (`<script setup>`)
2. PascalCase component names
3. API calls via `src/api/` modules
4. State management via Pinia
5. Scoped styles, global styles in `main.css`
