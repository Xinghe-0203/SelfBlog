# Configurable Public Pages Implementation Plan

> **For agentic workers:** Execute inline, task by task, using TDD and file-based progress tracking. The user confirmed the written specification; no further scope choice or global enlargement is needed. Request an independent review before delivery.

**Goal:** Provide safe author-managed public page content/styles, published custom pages and configurable navigation without changing current global proportions.

**Architecture:** A focused PageService owns versioned draft/published documents in the existing site_setting KV table, outside the global public-settings whitelist. A typed frontend store and page-config composable supply the same overrides to real pages and isolated admin previews. Page metadata is lifecycle-owned; server HTML SEO and deployment fixes remain separate batches.

**Tech Stack:** Existing Java21/Spring Boot/MyBatis Plus/Jackson, Vue3/Pinia/TypeScript/Marked/DOMPurify, H2/Vitest/Chrome and isolated SQLite. No schema migration or runtime package additions.

## Global Constraints
- User canceled whole-site110% enlargement; keep current default fonts/navigation/layout.
- Preserve user-owned root CLAUDE.md, existing databases and uploads.
- Preserve direct publication of articles, authentication bootstrap order, private-album authorization, comments and single audio ownership.
- Public page APIs never return drafts; previews stay in the authenticated workspace.
- Body10000 characters; each serialized KV document <=48KiB UTF-8. Unknown fields/modules/styles rejected.
- Every content write checks the expected revision in the database; conflict409, not silent overwrite.
- Built-ins cannot be deleted/unpublished. Published custom slugs immutable; deletion confirms.
- All old345 frontend/463 backend tests remain passing. Run backend tests with both frontend-plugin skip flags.

## File Map and Contracts

Backend:
- `backend/src/main/java/com/selfblog/dto/site/PageModels.java`: typed payload, style/SEO, draft/public snapshots, editor/public manifest/navigation DTOs.
- `backend/src/main/java/com/selfblog/service/site/page/PageCatalog.java`: built-in page field/module registry, validation and defaults.
- `backend/src/main/java/com/selfblog/service/site/PageService.java`: public reads and author writes.
- `backend/src/main/java/com/selfblog/service/site/impl/PageServiceImpl.java`: transactions, separate KV prefixes, revision comparison, slug reservation and navigation filtering.
- `backend/src/main/java/com/selfblog/controller/admin/AdminPageController.java`, `controller/site/PublicPageController.java`: thin authenticated/public endpoints.
- `SiteSettingsServiceImpl.java`, `SecurityConfig.java`, `RateLimitProperties.java`: reserved pages path, explicit GET permission and bounded read/write request prefixes.

Frontend:
- `frontend/src/types/page.ts`, `api/pages.ts`: transport contract matching Java records.
- `frontend/src/stores/pages.ts`: published manifest and per-page reads only; no admin documents in persistent storage.
- `frontend/src/composables/usePageConfig.ts`, `components/pages/PageFrame.vue`, `MarkdownContent.vue`: resolved overrides, scoped tokens, safe body rendering.
- Existing public views: consume page-owned text/module settings while preserving query and media ownership.
- `frontend/src/views/pages/ContentPage.vue`: published custom slug lookup with genuine loading/error/not-found states.
- `frontend/src/views/admin/pages/PagesManagement.vue`, `PageEditor.vue`, `PagePreview.vue`, `NavigationEditor.vue`: lists, transactional author actions, shared preview and button-based navigation sorting.
- `router/modules/admin.ts`, `router/modules/home.ts`, `AdminLayout.vue`, `Navbar.vue`, `main.ts`, `App.vue`: routes, navigation overflow and startup published configuration.
- `useSeoMeta.ts`: explicit meta removal, noindex and JSON-LD cleanup; detail pages retain actual article/tag identity.

### Task 1: Page domain, persistence and validation

**Interfaces:** `PageService.getManifest()`, `getPublished(id)`, `getBySlug(slug)`, `listPages()`, `getEditor(id)`, `createPage(slug,title)`, `saveDraft(id,revision,slug,config)`, `publish(id,revision)`, `unpublish(id,revision)`, `deletePage(id,revision)`, `getNavigation()`, `saveNavigation(revision,items)`.

Document keys are `page.draft.<id>`, `page.published.<id>`, `page.slug.<slug>`, `page.navigation`. Slug reservation inserts enforce uniqueness across simultaneous creations. Page IDs remain stable if an unpublished slug changes. Payload fields are nullable title/description/body plus `text`, `modules`, `style`, `seo`; absent differs from explicit empty text.

- [x] Read existing controller/service/mapper/entity chain and transactional test conventions before edits.
- [x] Add H2 integration tests in `backend/src/test/java/com/selfblog/service/site/impl/PageServiceImplTest.java` covering defaults, isolation and version conflicts:
```java
var draft = pages.createPage("my-projects", "项目");
assertThrows(BusinessException.class, () -> pages.getBySlug("my-projects"));
var published = pages.publish(draft.id(), draft.draft().revision());
assertEquals("项目", pages.getBySlug("my-projects").config().title());
assertThrows(BusinessException.class,
    () -> pages.publish(draft.id(), draft.draft().revision()));
```
- [x] Run JDK21 Maven `test -Dtest=PageServiceImplTest -Dskip.npm=true -Dskip.installnodenpm=true`; observe missing feature RED, then implement records/catalog/service.
- [x] Test all8 built-in module allowlists, explicit empty overrides, invalid styles/URLs/body/UTF-8 size, slug uniqueness and rename-after-publication rejection; reject malformed stored schema instead of leaking raw data.
- [x] Implement conditional updates with bound old serialized state containing revision; `UPDATE ... WHERE setting_key=? AND setting_value=?` increments revision so cross-database collation cannot accept another revision. Missing-row inserts translate duplicate keys to409.
- [x] Run focused tests GREEN and update affected backend docs with exact KV/backup limits.

### Task 2: HTTP authorization and navigation contract

**Interfaces:** GET `/site/pages` manifest, GET `/site/pages/{id}`, GET `/site/pages/by-slug/{slug}`; GET/POST `/admin/pages`, GET `/admin/pages/{id}`, PUT `/{id}/draft`, PUT/DELETE `/{id}/publication`, DELETE `/{id}`. GET/PUT `/admin/page-navigation` uses expected revision and `{pageId,label}` items. Runtime adds `/api` automatically.

- [x] Add controller tests (MockBean JwtUtils plus admin/nonadmin/anonymous identities), including real JSON unknown-field rejection and an unknown draft public404.
```java
mvc.perform(get("/admin/pages")).andExpect(status().isForbidden());
// Each test sets its identity explicitly; public reads never require a token.
```
- [x] Run focused Maven tests RED, add thin controllers and security/rate-limit rules; writes use role admin and service transactions.
- [x] Navigation requires home, no duplicate targets, valid labels and only navigable published targets; public paths are derived, never arbitrary input URLs. Unpublished/deleted targets filtered without revealing draft data.
- [x] Test independent navigation revision conflicts and live filtering after unpublish/delete; run focused tests GREEN.
- [x] Add pages to reserved admin paths; if an existing adminPath=pages is present, retain its route and block custom-page creation with migration guidance, rather than seize it.

### Task 3: Shared frontend configuration and page integration

**Interfaces:** `usePagesStore().loadManifest()`, `.loadPage(id)`, `.loadSlug(slug)`; `usePageConfig(id)` returns `config`, `text(key,fallback)`, `module(key,default=true)`; `PageFrame` takes config and scopes width/background/accent/spacing tokens. `MarkdownContent` receives source and sanitizes before v-html.

- [x] Add Vitest tests for null-vs-empty resolution, modules, CSS isolation, unsafe Markdown, custom404 and request replacement; run with `node node_modules/vitest/vitest.mjs run <test-files>` to observe RED.
```ts
expect(resolveText({ title: '' }, 'title', '默认')).toBe('')
expect(resolveText({}, 'title', '默认')).toBe('默认')
```
- [x] Add typed API/store/composable and pure presentation components; bootstrap published manifest after site settings without interrupting auth restoration/router installation if the optional request fails.
- [x] Integrate Home/About/Archives/Gallery/Search/PostDetail/TagDetail/NotFound field/module contracts from the spec; add view tests before each integration.
- [x] Add `/pages/:slug` route and ContentPage; skip reserved route when existing adminPath=pages. Unknown/unpublished custom pages show not-found, API errors remain retryable, and stale responses cannot replace a new slug.
- [x] Use semantic CSS variables for per-page styles; defaults must not alter current measurements. Verify selected skin, dark and forced-colors behavior without global CSS writes.
- [x] Run focused tests and strict type-check GREEN.

### Task 4: Author page editor and authenticated preview

**Interfaces:** admin list `/pages`, editor `/pages/:id`; PageEditor consumes service editor snapshots and sends complete payload with expected revision. PagePreview provides the override to the same public view components, choosing a real public article/tag when those templates are previewed (empty state if none).

- [x] Add attached real-RouterView tests for draft save vs publish, failed requests, stale revision feedback, in-flight edits/route guards, unpublished preview isolation and custom creation/unpublish/delete confirmation; observe RED for missing components and lifecycle defects before their fixes.
```ts
await saveButton.trigger('click')
expect(pageApi.publish).not.toHaveBeenCalled()
// Publishing uses the saved revision; edits during its request remain dirty.
```
- [x] Implement separated content/style/SEO/preview sections with default-override toggles, character counts, unsaved protection, snapshot-safe responses and no overlapping writes.
- [x] Preview uses injected page configuration, never a public query parameter or global store mutation; it suppresses visitor tracking and head-meta effects. Actual desktop/mobile srcdoc viewports are clipped inside a labeled scroll container.
- [x] Add admin sidebar/routes and navigation editor with label/hide/up/down controls. Reset built-in config to `{}` only in draft, publish separately. Make errors actionable and retain edits.
- [x] Run focused tests and strict type-check GREEN.

### Task 5: Responsive public navigation and page metadata

- [x] Extend Navbar tests with custom published links, hidden draft links, more-menu Escape/focus, and full mobile menu; observe fit-budget/unit regression RED, then verify actual fit with Chrome at seven widths.
- [x] Use measured available space for primary-vs-overflow links, preserving search/link/tool order and44px targets. Keep one menu panel owner and close on route/viewport changes.
- [x] Add SEO lifecycle tests: article → About removes article tags/JSON-LD; empty description clears prior value; search/404/admin/preview cannot become indexable; article/tag overrides retain their actual identity.
- [x] Apply page-level metadata from resolved published config, while detail views supply real identity; admin preview never changes document head. This is runtime metadata only, not server-rendered SEO completion.
- [x] Run all focused tests/type-check and seven-width Chrome checks GREEN.

### Task 6: Full regression, live evidence, review and docs

- [x] Run frontend type-check/test:run/lint:check/build with provisioned Node; backend full tests with JDK21 and both skip flags. No repo-wide formatting.
- [x] Test only in new temporary SQLite/uploads: admin saves About draft, fresh guest cannot see it, publish changes guest page, new custom page/nav/rename-conflict/unpublish/delete work and private albums remain locked.
- [x] Re-run existing17 real-service workflow checks plus seven-width foundation checks. Check 320/390/768/820/960/1024/1440px, dark/reduced-motion/keyboard and no default enlargement.
- [x] Request independent read-only review (attempt interrupted by quota; no independent approval). Complete inline review, reproduce findings with failed regressions before fixes, and rerun gates.
- [x] Update README variants, CHANGELOG and `docs/` author guide with exact feature/endpoint/limit details and evidence. Keep initial-HTML SEO and Docker work explicitly unshipped until their separate batches.
- [ ] Commit only scoped product/spec/plan/docs; preserve root CLAUDE.md and scratch notes. Push only reviewed delivery per the user's earlier request.

## Plan Self-Review

All first-batch spec sections map to tasks1–6. Separate draft/public keys and preview context prevent accidental disclosure. Versioned database writes, UTF-8 limits, builtin defaults, dynamic titles and navigation overflow are explicit. Global enlargement is canceled. Later SEO/server deployment work is intentionally not hidden inside this implementation plan.

## Execution Evidence

- Final frontend41 files/389 tests; type-check, ESLint0 errors/256 warnings, build and touched-file format pass. Includes three request-order regressions added during final review.
- Backend491 tests, no failures/errors/skips; includes real H2 transaction races, schema-version failure and legacy pages admin-path preservation.
- Actual packagedJAR-launched SQLite/Chrome audit31/31, including native bundled routes, retainedDENY headers, UI-confirmed unpublish/delete,390px dark preview and both first-load response orders. Original seven-width/audio checks pass.
- `PagePreviewContent.vue` was separated from the srcdoc viewport presenter so actual media queries work without relaxing frame security. Content route is installed in router/index.ts only after the server admin namespace is known.
- Independent review attempt has no result due quota; inline review findings and exact verification limits are recorded in the linked review document, not described as independent approval.
