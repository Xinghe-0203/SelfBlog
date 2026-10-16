# Crawlable HTML Core Implementation Plan

> **For agentic workers:** Execute inline task-by-task with TDD and checkpoints. The generic writing-plans handoff names executing-plans/subagent-driven-development, neither available here; use the available test-driven-development workflow without inventing skills or spawning unrequested agents.

**Goal:** Return safe published reading HTML, correct metadata/status, complete sitemap shards and consistent client canonical URLs from the existing JAR.

**Architecture:** A shared SiteOrigin validates the canonical origin. The owning query services supply published-only data; reading model/content/document units separate data access from sanitization and index-template injection. Discovery documents use bounded metadata pagination independently of feed limits.

**Tech Stack:** Java21/SpringBoot3.3.13/MyBatisPlus, CommonMark Java and existingJsoup/Jackson, Vue3/Vite/TypeScript/Vitest. Approved [spec](../specs/2026-10-16-crawlable-html-and-deployment-design.md); independent [deployment plan](2026-10-16-unified-deployment-adapters.md).

## Global Constraints

- No global enlargement, framework migration, schema changes or changes to existing data/uploads.
- Public HTML never includes drafts, even for an authenticated author; no GET/HEAD counters or visitor writes.
- SITE_BASE_URL is a validated origin; configured origin wins over Host/forwarded headers. No public domain is guessed.
- Preserve dynamic admin namespace, existing public/legacy routes, API security and DENY frame headers.
- Sitemap shards at most1000 URLs, protocol maximum50000 entries/50MiB; feeds retain100 items.
- Preserve rootCLAUDE.md user edit and untracked planning notes; only scoped commits.
- Backend tests always use JDK21 and both frontend-plugin skip flags. Full baselines frontend389/backend491.

## File Map and Contracts

New package `backend/src/main/java/com/selfblog/service/site/seo/`:

- `SiteOrigin.java`: constructor(String configured), resolve(HttpServletRequest):String, normalize(String):String, absolute(String base,String path):String.
- `SeoModels.java`: Meta(title,description,image,type,path,publishedTime,updatedTime,author,tags,indexable), ReadingPage(status,id,config,meta,contentHtml,navigation).
- `SafeMarkdown.java`: render(String):String; CommonMark parse andJsoup whitelist, no service/database access.
- `ReadingContent.java`: build semantic HTML withJsoup text/attribute APIs from public DTOs, modules and settings.
- `ReadingPageService.java`: resolve(String path,String page):ReadingPage; strict public models, status and canonical query classification.
- `ReadingHtmlRenderer.java`: render(ReadingPage,String origin):String; independent parsed build-index clone, head replacement, safeJSON-LD.
- `SitemapService.java`: index(String base):String, shard(String kind,int part,String base):String, site(String base):String; protocol sizes and public filtering.

Modify `controller/SpaController.java` to return ResponseEntity<String> and delegate; `controller/site/SeoController.java` shares origin and discovery service. New static CSS is `backend/src/main/resources/static/seo-reading.css`, scoped to server reading DOM only.

Owning query additions in `service/post/PostQueryService.java` + `impl/PostQueryServiceImpl.java`:

```java
PostDetailResponse getPublishedPostDetail(Long postId);
IPage<BlogPost> getPublishedMetadata(long page, long size);
```

The detail method selectsstatus=1 atomically and reuses a private DTO assembler. Metadata selects id/title/summary/category/cover/time only, nevercontent; cappedsize1000 and deterministicid order. Existing getPostDetail author access remains unchanged.

## Commands

Frombackend, every Maven test command below uses:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
$mvn='C:\Users\Zora\.m2\wrapper\dists\apache-maven-3.9.12\59fe215c0ad6947fea90184bf7add084544567b927287592651fda3782e0e798\bin\mvn.cmd'
& $mvn test '-Dtest=SiteOriginTest' '-Dskip.npm=true' '-Dskip.installnodenpm=true'
```

Substitute only the test selector. Fromfrontend prepend`../backend/target/frontend/node` toPATH, then use`npm run type-check`, `npm run test:run`, `npm run lint:check`, `npm run build`; format/check only changed files withend-of-line auto.

### Task1: Shared validated origin and safe Markdown

Files: create SiteOrigin/SafeMarkdown and `backend/src/test/java/com/selfblog/service/site/seo/{SiteOriginTest,SafeMarkdownTest}.java`; updatepom with a pinned CommonMark dependency.

- [x] Write pure unit tests before classes exist. Assertions include:

```java
var request = new MockHttpServletRequest();
request.setScheme("http"); request.setServerName("internal"); request.setServerPort(8825);
request.addHeader("X-Forwarded-Host", "attacker.test");
assertEquals("https://blog.example.com", new SiteOrigin("https://blog.example.com/").resolve(request));
assertThrows(IllegalArgumentException.class, () -> new SiteOrigin("https://user:pass@example.com"));
assertFalse(new SafeMarkdown().render("<script>alert(1)</script>\n\n[x](javascript:alert(1))").contains("<script"));
```

- [x] Run targetedtest selectors and record missing-featureRED, not unrelated environment failure.
- [x] Implement URI validation (scheme/host/port/root-only/no credentials/query/fragment/backslash/control/multivalue); strip trailingroot slash and defaultport. Use direct request origin only when blankconfigured. SafeMarkdown escapesrawHTML, sanitizesrenderedHTML with explicit protocols and noevents/style/frames/SVG.
- [x] Run targeted testsGREEN, including relative links/images and heading/code preservation. Add backendREADME dependency/origin notes with actual validation.

### Task2: Strict published query path

Files: modify PostQueryService/Impl; test `backend/src/test/java/com/selfblog/service/post/PublishedPostQueryTest.java`. Read full controller→service→mapper→entity chain before edits; no new SQL needed when generatedqueries suffice.

- [x] Create H2 transactional tests: author-ownedstatus0 is404 onstrictmethod but existingmethod stillreturnsit; deleted/unpublished records absent; metadata body isnull; two pages are stable and have correcttotal.

```java
assertThrows(BusinessException.class, () -> queries.getPublishedPostDetail(unpublishedId));
assertTrue(queries.getPublishedMetadata(1, 1000).getRecords().stream().allMatch(p -> p.getContent() == null));
```

- [x] RunPublishedPostQueryTest RED. Add strict generatedselect and factorprivate DTO assembly without changingoldauthorpolicy.
- [x] Run new and existing post query/controller testsGREEN; counts unchanged after public reads.

### Task3: Published reading models and safe document rendering

Files: create SeoModels/ReadingContent/ReadingPageService/ReadingHtmlRenderer/CSS; tests `ReadingPageServiceTest.java`, `ReadingHtmlRendererTest.java` inservice/site/seo.

- [x] Write tests with mocked owningservices (no mockedHtmlrenderoutput): postfullbody, publishedAbout/custom, homepagination, missing/illegal ID404, emptysite200, adminPath=pages precedence, privatealbum metadata only and template noindex. A representative model assertion:

```java
var page = reading.resolve("/pages/projects", null);
assertEquals(200, page.status());
assertTrue(page.contentHtml().contains("项目正文"));
verify(pages, never()).getEditor(anyString());
```

- [x] Add renderer tests using an explicit built-index fixture with hashedassets; parse output viaJsoup, assertarticletext/meta/canonical, one#app, safescriptJSON, no duplicated tags, retainedassets, no sharedDocmutation. Includemissingtemplate503 test.
- [x] Observe RED, implement models/content/classifier/document units, keep service methods readOnly. Lists bounded to existinghomepage size and100 forarchive/tag; unsafeURL/text neverrawconcatenated. NoUA branches or account queries.
- [x] Run focusedGREEN withXSS andmodule/stylecases. Template loadedlazily so test-only backend does not requirefrontendbuild; unavailabletemplate makesHTML503, not wholeAPIstartupfailure.

### Task4: HTTP delivery and initial status/header contract

Files: modify SpaController, SecurityConfig only for needednewrootXML patterns; create `backend/src/test/java/com/selfblog/controller/site/ReadingHtmlControllerTest.java`.

- [x] Add MockMvc tests (production/api prefix, MockBeanJwtUtils and realrendererfixture) foranonymousGET/HEAD: article200fulltext, custom404, missingpublic404, adminnoindex, index.html308, excludedassets/API notHTML, controlled500/503 noindex/no-store. Assertconfiguredorigin survives spoofedheaders.

```java
mvc.perform(get("/post/1")).andExpect(status().isOk())
   .andExpect(content().contentTypeCompatibleWith("text/html"))
   .andExpect(header().string("Cache-Control", "no-store"));
```

- [x] Observe forward-to-indexRED; replace forwarding with ResponseEntityHTML, preserve resource/API boundary andsecurity. Implementgeneric safeerrorswithoutstackdetails; no catch converting5xx into404.
- [x] Run focused plus security testsGREEN. GETandHEAD return sameheaders/status; HEAD has no body and performs no counters/writes.

### Task5: Complete discovery documents

Files: newSitemapService; modifySeoController; tests `SitemapServiceTest.java`, `DiscoveryControllerTest.java`. Readtag/site chains completely if addingpaginationmethods.

- [x] Create realH2 data with1001+posts andstoredpublishedpage/noindexsettings. Assertindex/shardunion containsallpublishedIDs exactlyonce, no drafts/deleted/search/admin; invalidshards404. Assertfeeds still100 anddynamicrobots changesafteradminPathsave.
- [x] RunRED, implementpagedmetadata andstabletag/customorder with1000-item shards, and sharedorigin. SiteXML usesrealpageconfig; parts notadvertisedwhenempty/disabled; XML escapes andactualdatesonly.

```java
assertEquals(1000, Jsoup.parse(sitemaps.shard("posts", 1, base), "", Parser.xmlParser()).select("url").size());
assertEquals(1, Jsoup.parse(sitemaps.shard("posts", 2, base), "", Parser.xmlParser()).select("url").size());
```

- [x] RunGREEN, enforce50000/50MiB failures rather than truncating. Keepfeedsoriginalpaths; removeoldprivatebaseUrl/rawforwardedlogic.

### Task6: Client origin, pagination and metadata convergence

Files: modify `frontend/src/composables/{useSeoMeta,usePageSeo}.ts` and affectedviews only if needed; extend `composables/__tests__/useSeoMeta.test.ts` andviewtests.

- [x] Add RED tests forserveroriginmarker, mountedpagecanonical, page2canonical, missing/errornoindex andduplicateheadcleanup:

```ts
document.head.insertAdjacentHTML('beforeend', '<meta name="selfblog-site-origin" content="https://blog.example.com">')
applySeoMeta({ title: '文章', url: '/post/1' })
expect(document.querySelector('link[rel="canonical"]')?.getAttribute('href')).toBe('https://blog.example.com/post/1')
```

- [x] Implementvalidatedoriginread withcurrentoriginfallbackforVite, no mutableglobalSiteStorewrite. Preservepreviewheadisolation. Alignvalidpagequeryclassification/canonical withserverandkeepcurrentdefaultlayout.
- [x] Runfocusedtests,typecheckthenfullfrontendgates; localrenderDOMmountsmoke verifiesfallbackdisappearsandoneaudioownerremains.

### Task7: Full integration, evidence and scoped delivery

- [x] Runfullbackendtest (bothskips), frontend389+ tests/typecheck/lint/build/changedfileformat andpackage latestassets.
- [x] ExtendlocaldisposableSQLite/JAR runner: JSdisabledarticle/About/custombody, noindex/404, sitemapfollowallparts, strictdraftisolation, bothpublicationresponseorders; rerunold31flowsand7widthfoundationchecks.
- [x] Execute deployment-adapter tests andlocallygeneratedrouteproxy smoke; recordDocker/realVercellimits truthfully. Source review inline; reproduceactualfindingsREDbeforefix, rerunaffected/fullgates.
- [x] Update README/CHANGELOG/DEPLOY/spec/ADR and `docs/reviews/2026-10-16-seo-deployment-verification.md` withexactevidence, notpromisedindexing.
- [x] Scopedcommits only, preserveuserCLAUDE/scratch/data, pushperprioruserdeliveryrequest, verifyremoteSHA. Do notdeploytoexistingruntimeorcloudwithoutauthorization.

## Execution Status (2026-10-16)

Implemented and verified as one batch; delivery commit `951dd08` (feature) plus `4caff07` (live-audit fixes) and `6d80337` (docs) pushed to `origin/main`.

- Frontend **41 files / 392 tests**, type-check, ESLint 0 errors and production build pass; deployment contracts **27/27**.
- Backend **540 tests / 0 failures / 0 errors / 0 skipped**.
- Real packaged-JAR + fresh-SQLite audit: **8/8** (JS-disabled reading, spoofed Host, published About/custom bodies, sitemap completeness, unpublish → 404, Vue replacement, local proxy transport, no script errors/5xx) and the previous batch's browser regression **31/31**.
- Live audit also produced the transaction-boundary fix: `ReadingPageService` has no class-level transaction, and 404 regressions run with `NOT_SUPPORTED` so no enclosing test transaction can hide rollback-only failures again.
- Not verified: MySQL execution, Docker image build/run (no daemon), a real Vercel deployment, and search-engine indexing. `docs/reviews/2026-10-16-seo-deployment-verification.md` records all evidence and limits.

## Plan Review

Spec sections1–7 map totasks1–4/6; section8 totask5; sections9–10 tolinkeddeploymentplan; section11 totask7. Interfaces above are the contracts used by downstreamtasks. Core anddeployment adapters can be tested independently; no new feature is inferred from optionalplatformaccess.
