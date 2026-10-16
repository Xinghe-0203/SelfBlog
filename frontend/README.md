# SelfBlog - Frontend

Vue 3 frontend for a single-author personal blog. Public pages focus on reading, search, archives, about, and tags; writing and content management are protected by administrator-only routes.

**Version**: v2.0.25 | **Vue**: 3.5.44 | **Vite**: 6.4.4

## Tech Stack

| Component | Version | Purpose |
|-----------|---------|---------|
| Vue | 3.5.44 | UI framework (Composition API) |
| Vite | 6.4.4 | Build tool |
| Vue Router | 4.3.0 | Routing |
| Pinia | 2.1.7 | State management |
| Axios | 1.7.4 | HTTP client |
| Marked | 12.0.1 | Markdown rendering |
| DOMPurify | 3.0.9 | XSS protection |
| TypeScript | 6.0 | Type safety |
| Vitest | 4.1.8 | Unit tests (jsdom + @vue/test-utils) |

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
├── api/                # Axios modules + interceptors (index, site, admin, post, media, …)
├── components/
│   ├── common/         # Reusable UI: Button, Card, Input, Icon, Modal, Toast, ImagePreview, BackToTop, …
│   ├── layout/         # Navbar, Footer, MusicPlayer
│   ├── home/           # PostCardList, HomeSidebar
│   ├── post/           # MobileTocSheet (article reading extras)
│   ├── circle/         # Campus circle widgets
│   └── admin/          # SiteAssetField (brand asset uploads)
├── composables/        # useConfirm, useScrollState, useMarkdownEnhance, useSeoMeta, usePagination, useScrollReveal
├── router/             # Entry, guards, lazy helpers, per-domain modules
├── stores/             # Pinia: site, theme, user
├── styles/             # Tokens + base + components + dark mode + a11y (entry: main.css)
├── types/              # TypeScript type definitions
├── utils/              # crypto, logger, toast, visitor, reading
├── views/              # Home / About / Archives + admin, auth, post, circle, user, gallery, search, tag, topic, trending, common
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
themeStore.isDark        // Dark mode status
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

Clean tech-blue ("清爽科技蓝") design system: CSS-variable tokens for color, spacing,
radius and shadows, a first-class dark mode, and five selectable light/dark skins.
Surfaces are solid by default — the legacy glass tokens (`--glass-*`) are kept for
compatibility but resolve to solid colors with `--glass-blur: none`.

### CSS Variables

```css
:root {
  --accent: #165DFF;
  --radius: 12px;
  --surface-primary: #FFFFFF;
}
html.dark-mode { /* Dark mode overrides live in dark-mode.css */ }
```

### Legacy glass classes

The `.glass*` / `.water-drops` utilities remain for backward compatibility but
resolve to solid surfaces (`--glass-blur: none`); new UI uses the `--surface-*`
tokens instead of glass effects.

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
