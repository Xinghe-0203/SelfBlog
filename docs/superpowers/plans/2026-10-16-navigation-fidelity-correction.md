# Navigation Fidelity Correction Implementation Plan

> **For agentic workers:** Execute inline as requested by the user's continuation. Follow TDD and the approved correction spec; no additional agent execution is needed.

**Goal:** Restore the approved navigation/music composition and fix the independently reproduced author-workflow defects.

**Architecture:** Keep Vue/Pinia ownership unchanged. Navbar and NavbarMusicControl only change presentation. AdminLayout derives modal ownership from viewport plus drawer state. PostEdit compares current content with successful request snapshots; server APIs and validation remain authoritative.

**Tech Stack:** Existing Vue/TypeScript/CSS/Vitest/Chrome; current Spring Boot/SQLite for isolated integration checks.

## Global Constraints
- No runtime dependency, backend API/schema, publication gate or author-setting changes.
- Preserve root CLAUDE.md user edits; don't mutate existing databases/uploads.
- Keep 52/50px nav and 44px tools; real duration/disabled seeking and reduced-motion behavior stay intact.

### Task 1: Author lifecycle regressions

Files: `frontend/src/views/admin/AdminLayout.vue`, `__tests__/AdminLayout.test.ts`, `frontend/src/views/admin/TagsManagement.vue`, `__tests__/TagsManagement.test.ts`, `frontend/src/views/post/PostEdit.vue`, `__tests__/PostEdit.test.ts`.

- [x] Add failing drawer resize/inert/aria target tests; use `window.innerWidth=390`, open, resize to 1440, assert expanded false and body unlocked.
- [x] Run `node node_modules/vitest/vitest.mjs run src/views/admin/__tests__/AdminLayout.test.ts`; observe RED.
- [x] Derive drawer modal state from mobile viewport, add resize cleanup and `id="admin-sidebar"`/inert; verify desktop navigation remains reachable.
- [x] Add failing tag create/edit length and rejected-save feedback tests. Change maxlength to 20 and guard trimmed values before API calls.
- [x] Mount PostEdit through real RouterView. Add tests for successful save/publish without leave prompt, failed save preserving prompt, in-flight edits preserving prompt, and auto-save not overlapping publish.
- [x] Replace dirty boolean assignments with successful-content snapshots; serialize draft saves, prevent publication during draft-save, and protect edited content during in-flight requests. Preserve numeric-ID responses and existing media/tag flows.
- [x] Run focused tests and type-check; keep failed-write protection tests passing as regression guards.

Acceptance examples:
```ts
expect(wrapper.get('.menu-button').attributes('aria-expanded')).toBe('false')
expect(document.body.style.overflow).toBe('')
expect(input.attributes('maxlength')).toBe('20')
await router.push('/') // after a successful unchanged save
expect(confirmSpy).not.toHaveBeenCalled()
```

### Task 2: Approved navigation/music fidelity

Files: `frontend/src/components/layout/Navbar.vue`, `NavbarMusicControl.vue`, corresponding `__tests__/*.test.ts`.

- [x] Add a failing idle-bars/"听一会儿" regression. Extend the existing Chrome comparison to assert the nav/search gap stays near 12px at 1440px; run before edits to observe the current 422px gap fail.
- [x] Remove auto margin from `.nav-links`; add it to `.nav-actions`. Use a 34px capsule visual within a 44px anchor target, preserve mobile overflow rules.
- [x] Restore music composition, static idle bars, 54px gradient mark and loop to the right; use thin custom range tracks driven by real media values. Retain all error/disabled/focus/close affordances.
- [x] Run layout/music tests and Chrome comparison at 320/390/768/820/960/1024/1440px, light/dark/reduced-motion and configured/unconfigured music.

### Task 3: Verify, review and document

- [x] Run `npm run type-check`, `npm run test:run`, `npm run lint:check`, `npm run build` with provisioned Node in frontend.
- [x] Run backend JDK 21 Maven test with `-Dskip.npm=true -Dskip.installnodenpm=true`.
- [x] Re-run the disposable real SQLite/browser audit; the publication-prompt and tag-limit failures must now pass. Check mobile drawer via Chrome after resize and closed Shift+Tab.
- [x] Review changes for async races, stale snapshots, hidden keyboard targets, focus/scroll restore, author config and accurate documentation. Format only changed source/test files.
- [x] Update report, README variants, frontend guide and CHANGELOG with exact current results; preserve historical failure evidence and note untested environments.

## Verified result
- Frontend 31 files / 345 tests; type-check, lint (0 errors / 255 warnings), build and all 11 touched-file format checks pass.
- Backend 463 tests, no failures/errors/skips; no backend API/schema changes.
- Real SQLite/Chrome expanded workflow audit 17/17; seven-width prototype comparison and forced-colors visibility checks pass.
- Independent review findings were reproduced and fixed; re-review confirms both P2s closed and 48 scoped assertions passing.
