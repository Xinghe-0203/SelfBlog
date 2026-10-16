# Compact Glass Foundation and Navigation Music Implementation Plan

> **For agentic workers:** Execute inline in this session, task by task, using the loaded TDD and file-planning workflows. Delegation is optional, not required. Steps use checkbox syntax for tracking.

**Goal:** Ship the approved smaller-radius design foundation, reference-style navigation and one reliable shared audio player.

**Architecture:** Keep Vue/Pinia and App as the lifecycle root. Separate the audio element/transport state from navigation presentation; keep theme-aware overlays outside route transitions. Shared CSS remains semantic and content surfaces stay solid.

**Tech Stack:** Vue 3, TypeScript strict/noUncheckedIndexedAccess, Pinia, existing SVG registry, Vitest/jsdom. No new dependencies.

## Global Constraints
- Source of truth: `../specs/2026-10-16-selfblog-ios-glass-redesign-design.md`, approved by the user.
- Desktop navigation 52px, mobile about 50px; `.app-main` retains its 80px offset.
- Default no green; author theme settings remain authoritative. Cards 12–14px, hero/popover 14–16px, buttons 8px.
- Never autoplay; only one audio element, public route changes preserve playback, admin entry pauses.
- Preserve existing CLAUDE.md edits, backend/schema/auth/bootstrap behavior and all 210 baseline tests.
- Commands run in frontend using `node node_modules/<tool>/<entry>` to avoid broken system npm shims.

## File boundaries
- `styles/variables.css`, `dark-mode.css`, `skins.css`, `base.css`, `a11y.css`, `glassmorphism.css`: tokens and opt-in functional glass.
- `stores/music.ts`: transport state, preferences, source-generation guards and media listeners.
- `components/layout/MusicPlayer.vue`: headless single audio host and route/settings binding.
- `components/layout/NavbarMusicControl.vue`: controlled popover, transport inputs and error display.
- `components/layout/Navbar.vue`: routes/search/responsive navigation, mutually exclusive panels.
- `composables/useDialog.ts`: stacked focus/Escape/scroll locking; `App.vue`: stable theme-aware overlay host.

### Task 1: Shared visual foundation
**Files:** Modify the six style files above, `stores/site.ts`, `App.vue`, `styles/components.css`.
**Interfaces:** Existing semantic tokens remain; add `--functional-glass-bg/edge/shadow/blur`, `--anchor-offset`, `--z-nav-panel`. Opt-in `.functional-glass` never changes the content-card layer.
- [ ] Read complete affected files and run the baseline: `node node_modules/vitest/vitest.mjs run` (210 tests).
- [ ] Add a store regression showing that custom primary colors derive their soft colors instead of leaving stale blue:
```ts
store.applySettings({ themePrimaryColor: '#a34466' })
expect(document.getElementById('selfblog')?.style.getPropertyValue('--accent')).toBe('#a34466')
expect(document.getElementById('selfblog')?.style.getPropertyValue('--accent-light')).toContain('color-mix')
```
- [ ] Run `node node_modules/vitest/vitest.mjs run src/stores/__tests__/site.test.ts`; observe the missing derived token failure.
- [ ] Define compact tokens and theme aliases directly in their owning files:
```css
--radius-xs:4px; --radius-sm:6px; --radius:8px; --radius-md:8px;
--radius-lg:12px; --radius-xl:14px; --radius-2xl:16px; --radius-3xl:20px;
--anchor-offset:96px; --z-nav-panel:1030;
```
Set system sans as display/body, quiet shadows and neutral solid canvas. Add functional glass with blur support, reduced-transparency/high-contrast fallbacks. Remove theme color interpolation that produces unreadable intermediate palettes.
- [ ] Re-run store tests, type-check and lint. Pure CSS acceptance is checked in the browser, not fake CSS string tests.

### Task 2: Shared audio transport (TDD)
**Files:** Create `stores/music.ts`, `stores/__tests__/music.test.ts`; replace `components/layout/MusicPlayer.vue`; add host tests; update `App.vue` mount.
**Interfaces:** `useMusicStore()` exposes `available`, `source`, `title`, `playing`, `loading`, `error`, `currentTime`, `duration`, `volume`, `loop`, `volumeSupported`; actions `configure({enabled,url,title})`, `attach(HTMLAudioElement|null)`, `setSuspended(boolean)`, `play():Promise<void>`, `pause()`, `seek(number)`, `setVolume(number)`, `setLoop(boolean)`.
- [ ] Write EventTarget-backed media tests before the store exists. Cases: no autoplay, events own playing state, valid metadata/clamped seek, rejected play, stale play after source change, detachment, admin suspension, malformed/blocked preference storage and unsupported volume.
```ts
store.configure({ enabled: true, url: '/uploads/song.mp3', title: 'Song' })
store.attach(audio)
expect(audio.play).not.toHaveBeenCalled()
await store.play()
audio.dispatchEvent(new Event('playing'))
expect(store.playing).toBe(true)
store.setSuspended(true)
expect(audio.pause).toHaveBeenCalled()
expect(store.playing).toBe(false)
```
- [ ] Run `node node_modules/vitest/vitest.mjs run src/stores/__tests__/music.test.ts`; verify missing module/behavior fails.
- [ ] Implement media state with a captured element and monotonically increasing attachment/source generation. A stale play continuation pauses its captured old element, never a newly attached element. Remove listeners on detach. Invalid/disabled settings immediately pause and reset state.
- [ ] Implement the host with a keyed source so replacement audio cannot inherit late old-source events:
```vue
<audio v-if="music.available" :key="music.source" :ref="attach" :src="music.source" preload="none" />
```
Watch site settings synchronously into configure; watch `route.meta.adminArea` into setSuspended. Host stays mounted outside router-view even in the admin area. Restore only preferences, never playback intent.
- [ ] Run store/host tests, type-check and full baseline. Add a pending-play test for play→pause→play as well as source replacement.

### Task 3: Compact navigation and music UI
**Files:** Replace `Navbar.vue`; create `NavbarMusicControl.vue` and tests; extend `icons.ts`; update existing Navbar tests to the approved visibility behavior.
**Interfaces:** Music control props `open:boolean`; emits `toggle` and `close`; consumes music store, never creates audio. Navbar owns `panel: 'menu'|'search'|'music'|null` and closes panels on outside click/Escape/route/modal events.
- [ ] Change the old hide-on-scroll expectation to remain visible; add failing tests for route closure, music/menu exclusivity, empty search, correct search query and one public menu per route.
```ts
await wrapper.get('.menu-toggle').trigger('click')
await router.push('/archives')
await nextTick()
expect(wrapper.find('.mobile-panel').exists()).toBe(false)
```
- [ ] Run focused tests and observe failures before replacing the navbar.
- [ ] Build brand→inline search→icon/text links→music/theme, soft selected fill/short underline, 52/50px stable height. At <=360px move search/theme into menu; retain 44px action hit areas. Use the approved prototype only as visual reference, not as product data.
- [ ] Build a ~300px music panel: title, accurate state, disabled-until-known seek, play/pause, volume/device-volume fallback, loop and inline retry. Opening it does not play. Use real buttons/ranges/aria labels and Escape focus return.
- [ ] Run focused + full tests; browser-check 320/390/768/820/960/1024/1440px, normal/reduced motion, both themes and custom skin.

### Task 4: Overlay lifecycle and foundation checkpoint
**Files:** Create `composables/useDialog.ts` and tests; update `Modal.vue`, `ImagePreview.vue`, `MobileTocSheet.vue`, `App.vue`.
**Interfaces:** `useDialog(open:Ref<boolean>, element:Ref<HTMLElement|null>, close:()=>void)` controls only the topmost dialog; exported `overlayTarget()` returns `#overlay-root` when present, `body` only for standalone consumers/tests. Shared lock count restores the pre-existing body overflow, not a hard-coded empty string.
- [ ] Write tests for two nested dialogs, Escape closes only the top layer, Tab wraps, focus restoration, initially closed unmount and preserving existing body overflow.
- [ ] Observe failing tests, then implement one module-owned dialog stack, lock ownership and mounted/unmounted listener cleanup. Announce modal-open to close navigation popovers.
- [ ] Mount `<div id="overlay-root" />` inside #selfblog, before components and outside route transitions. Move modal/sheet/lightbox teleports there; preserve existing props/emits.
- [ ] Run all tests/lint/type-check and browser verify dialogs cover navigation and inherit custom accent/dark skin.
- [ ] Record actual results in progress.md; commit only this batch's files, README/CHANGELOG updates and tests (never root CLAUDE.md).
