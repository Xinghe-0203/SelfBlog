# Compact Glass Public Reading Pages Implementation Plan

**Delivery note (2026-10-16):** Original batch delivered in `c550963`; actual regression and live-service coverage are recorded in [the audit report](../../reviews/2026-10-16-navigation-fidelity-and-fullstack-audit.md). Original unchecked step-level boxes were not maintained during execution; they are historical planning steps, not a current completion checklist.

> **For agentic workers:** Execute inline task by task after the foundation/music plan. Use TDD for behavior and browser checks for CSS-only work.

**Goal:** Bring every active public reading route to the approved B-space/A-list design without losing reading or gallery features.

**Architecture:** Extract a focused article-row component shared by home/search/tags. Keep each page's existing API/pagination/SEO ownership. Reuse the foundation dialog infrastructure for gallery and reading overlays.

**Tech Stack:** Existing Vue/TypeScript/Pinia/CSS, Marked/DOMPurify, Vitest and local Playwright; no new dependencies.

## Global Constraints
- Approved spec: B spatial structure, A longitudinal list, default no green, card radii 12–14px and hero 14–16px.
- App already reserves 80px for navigation; pages add only their local padding.
- No new APIs, invented author copy, external default images, schema or permission changes.
- Preserve sanitization, comments/anonymous likes, adjacent posts, copy/fold/toc/lightbox and existing 210 tests.

## File structure
- Create `components/post/ArticleList.vue`: props `{posts:Post[]; featured?:boolean}`; no fetching/like API ownership.
- Modify `views/Home.vue`, `views/Archives.vue`, `views/About.vue`, `views/search/Search.vue`, `views/tag/TagDetail.vue`, `views/post/PostDetail.vue`, `views/gallery/Gallery.vue`, `views/common/NotFound.vue`.
- Modify `components/layout/Footer.vue`, `components/common/BackToTop.vue`; existing public APIs remain unchanged.

### Task 1: Article rows and home layout
- [ ] Read the complete components/pages/APIs/types before edits.
- [ ] Add ArticleList tests: real article/tag links, reading time, no cover element when missing, image error removes thumbnail, long title retained.
```ts
const wrapper = mount(ArticleList, { props: { posts: [{ id: 1, title: 'A real note', content: '正文' }] }, global: { plugins: [router] } })
expect(wrapper.find('img').exists()).toBe(false)
expect(wrapper.get('a[href="/post/1"]').text()).toBe('A real note')
```
- [ ] Run `node node_modules/vitest/vitest.mjs run src/components/post/__tests__/ArticleList.test.ts`; observe RED.
- [ ] Implement `.article-list` with article metadata/title/summary/tags/time and optional right thumbnail. Use `estimateReadMinutes`, `getSafeImageUrl`, `truncateText` from existing utilities. No forced cover placeholders.
- [ ] Add Home tests for real siteDescription/author data, first successful cover retained through pagination, failed/no cover fallback, retry and existing query-based pagination; observe failures.
- [ ] Replace Home's large duplicated identity/featured cover with B's compact hero + A list + 224px author sidebar, 24px gap, sidebar hidden <=960px. Capture the first usable cover only after the first successful response; reserve geometry and handle image failure.
- [ ] Run focused tests/type-check; compare real Vite rendering to the approved local prototype at 390/768/1440px.

### Task 2: Shared public page rhythm
- [ ] Read remaining source in full, including Footer/NotFound and complete Search/Tag CSS.
- [ ] Add tests proving search submits trimmed q, tag results keep all links, archive queries retain old pages, about renders configured avatar/bio.
```ts
await wrapper.get('form').trigger('submit')
await nextTick()
expect(router.currentRoute.value.query.q).toBe('Vue')
```
- [ ] Observe RED for changed ownership/structure; reuse ArticleList in Search/Tag, preserve fetch/suggest/pagination behavior. Restyle archives as clear grouped rows; about as author-first profile; footer, 404 and utility actions use consistent compact controls.
- [ ] Remove decorative all-caps eyebrows and meaningless glyphs from active public paths. Use semantic icons and descriptive plain Chinese copy.
- [ ] Run focused tests/lint/type-check; inspect empty/loading/error/long-title states and responsive layouts.

### Task 3: Reading and gallery overlays
- [ ] Add failing tests for gallery Escape/scroll lock/focus return, password unlock preservation, load failure and shared image lightbox. Add reader regression for title-before-cover and safe anchors.
- [ ] Run relevant new tests plus existing ImagePreview/MobileTocSheet/markdown tests before edits.
- [ ] Reorder reader header before optional cover; preserve all data/actions. Use sans body, bounded measure, semantic anchor offset and rAF-coalesced reading progress with cancellation.
- [ ] Replace gallery's ad-hoc modal/lightbox with the shared Modal/ImagePreview and dialog lifecycle. Preserve gallery API token/password values and error handling. Use token z-index, not 1000.
- [ ] Run focused/full tests and browser verify keyboard, password albums, photo navigation, code copy/folding, heading hashes and mobile toc without horizontal overflow.
- [ ] Run lint/type-check/build, update affected README and CHANGELOG with actual files/results, then checkpoint this batch only.
