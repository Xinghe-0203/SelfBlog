# Compact Glass Author Workspace Implementation Plan

**Delivery note (2026-10-16):** Original batch delivered in `e791586`; follow-up drawer/editor/tag defects and their fixes are tracked in [the correction plan](2026-10-16-navigation-fidelity-correction.md) and [audit report](../../reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md). Original unchecked step-level boxes were not maintained during execution; do not blanket-check them as if each historical verification was recorded.

> **For agentic workers:** Execute inline after the foundation and public-page plans. Read each complete source before modifying it; test behavior before implementation.

**Goal:** Carry the approved design through every active author workflow, not just the sidebar.

**Architecture:** Keep dynamic admin routing, existing API modules and business actions. Use a compact glass shell around solid work surfaces; each screen owns its tables/forms and uses shared modal/confirm behavior.

**Tech Stack:** Existing Vue/TypeScript/Pinia/CSS and Vitest/Playwright; no UI framework or runtime dependency additions.

## Global Constraints
- Follow the approved whole-site spec and existing single-author/admin permissions.
- No pending-review publication gate, route/auth/bootstrap change, backend API/schema change or root CLAUDE.md edit.
- Real settings remain authoritative; uploads are still SiteAssetField + siteApi; preserve hand-entered URLs and optimistic uploaded updates.
- Buttons 8px, cards 12–14px, normal dialogs <=16px, no oversized pill tables/forms.

## File structure
Modify `views/admin/AdminEntry.vue`, `AdminLayout.vue`, `Dashboard.vue`, `Posts.vue`, `TagsManagement.vue`, `Visitors.vue`, `Comments.vue`, `Albums.vue`, `Settings.vue`; modify `views/post/PostEdit.vue` for the actual author editor. Shared controls remain in `components/common/` and `components/admin/SiteAssetField.vue`.

### Task 1: Author shell and entry
- [ ] Read all entry/layout sources and their current tests in full.
- [ ] Add route-close/Escape/focus tests for the mobile sidebar and tests proving only existing author destinations are present.
```ts
await wrapper.get('.menu-button').trigger('click')
await router.push('/writer/posts')
await nextTick()
expect(wrapper.find('.sidebar-open').exists()).toBe(false)
```
- [ ] Observe RED. Build a compact glass sidebar/header and restrained active fill/underline, stable page titles, 44px touch actions, theme toggle, clear return/logout. Use the shared dialog lifecycle for mobile drawer and preserve custom adminRoot.
- [ ] Rework login proportions/labels/errors and busy state without changing credential/auth/redirect behavior. Run focused tests/type-check.

### Task 2: Real author screens and settings
- [ ] Read every active screen in full and map each existing primary/secondary action before styling.
- [ ] Add tests for settings sections and existing upload/save behavior; preserve all previous Settings tests. Add editor smoke tests for draft/save/publish, not invented new actions.
```ts
expect(wrapper.findAllComponents(SiteAssetField)).toHaveLength(4)
await wrapper.get('form').trigger('submit')
expect(siteApi.updateSettings).toHaveBeenCalled()
```
- [ ] Observe RED for new section ownership/behavior, then organize Settings into identity/appearance/music/features, keeping clear save feedback and field errors. Move no field value into purely visual local state.
- [ ] Rework Dashboard metrics and quick actions; Posts/Tags/Visitors/Comments/Albums headings, toolbars, data rows, empty/error/busy states and action grouping. At mobile widths use explicitly scrollable data regions or responsive rows; never silently hide destructive/edit actions.
- [ ] Rework actual PostEdit toolbar/workspace/preview/metadata geometry with existing form and editor commands intact. Do not globally override every legacy page selector to pretend the work is done.
- [ ] Run focused tests/full suite, lint and type-check. Browser-smoke every active author route using authenticated browser-only fixtures or an isolated test account, not writes to existing production data.

### Task 3: Whole-site verification and documentation
- [ ] Run in frontend: `node node_modules/eslint/bin/eslint.js src`, `node node_modules/vue-tsc/bin/vue-tsc.js --noEmit`, `node node_modules/vitest/vitest.mjs run`, `node node_modules/vite/bin/vite.js build`.
- [ ] Check 320/390/768/820/960/1024/1440px, light/dark/reduced-motion, actual media playback fixtures, dialogs over navbar, custom theme inheritance, all public/author routes and error/empty states. Screenshot and inspect, rather than trusting no-overflow metrics alone.
- [ ] Review local diffs for unintended legacy-route/dependency/backend/data changes and old hardcoded demo copy/assets.
- [ ] Update `README.md`, `frontend/README.md`, `frontend/CLAUDE.md`, `CHANGELOG.md` with shipped behavior and real verification counts. Mark all three plans and the spec implementation status truthfully; no claims that prototype checks proved the final app.
- [ ] Obtain code review, address concrete defects and re-run affected checks. Commit scoped changes if appropriate; do not push/deploy without a separate user request.
