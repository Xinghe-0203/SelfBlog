# Unified Deployment Adapters Implementation Plan

> **For agentic workers:** Execute inline with available TDD and file-based checkpoints. No unrequested subagents or unavailable execution skills. This independently testable plan accompanies [core HTML](2026-10-16-crawlable-html-core.md).

**Goal:** Replace broken deployment examples with a validated Vercel unified proxy and safe root-context Docker builds without changing existing data.

**Architecture:** Vercel receives Build Output v3 routes generated from explicitorigins and forwards tooneJAR; it does notcompileanotherVuebundle. Dockerbuilds thesameJAR fromrepo-rootinputs; existingMySQLComposeisretained andSQLitehasanexplicitseparateentry.

**Tech Stack:** Nodebuiltinmodules/`node:test`, VercelBuildOutputAPIv3, Maven/JDK21/JRE21, DockerCompose.

## Global Constraints

- PreserveUIproportions, standard`npm run build`, existingMySQLvolume names andalluserdata.
- HTTPSroot-onlyorigins; backendandpublicoriginsmustdiffer; rejectloops/credentials/query/fragment/backslashes/controlinput. Nohardcodedreferencehost.
- NoindependentVercelstaticindex/assets, noNodeproductionrenderer, noSQLiteonVercel.
- Dynamicproxyresponsesno-store; retainDENY/nosniff/referrerheaders; previewHTTPnoindex.
- Rootcontextmustexcludereal.env/databases/media/.git/notes/node_modules/target/dist. Neverrunvolume deletion.
- NoDockerexecutablecurrentlyavailable; staticverificationisnotcontainer-runtimeproof.

### Task1: Build-time Vercel proxy

Files: create `frontend/scripts/{vercel-proxy,build-vercel-proxy}.mjs`, `frontend/scripts/__tests__/vercel-proxy.test.mjs`; modifyfrontend/package.json, frontend/vercel.json, .gitignore ifgeneratedoutputnotalreadyignored.

Interfaces:

```js
export function createProxyConfig(env) // returns {version:3,routes:[...]}; no filesystem/network effects
export async function writeProxyOutput(env, outputDirectory) // writes config and empty static dir only within explicit output dir
```

- [ ] Write`node:test` REDbeforeimplementation forvalidrouteforwarding, missing/invalidorigins, samedomainandcurrentdeploymentloop, securityheaders/no-store andnonproductionnoindex. Useisolatedtempoutputtoshowthereisnoindex/assets, andsentinelfilesoutsideoutputarenottouched.

```js
const config = createProxyConfig({ SELFBLOG_BACKEND_ORIGIN: 'https://origin.example.com', SITE_BASE_URL: 'https://blog.example.com', VERCEL_ENV: 'production' })
assert.equal(config.version, 3)
assert.equal(config.routes.at(-1).dest, 'https://origin.example.com/$1')
assert.throws(() => createProxyConfig({ SELFBLOG_BACKEND_ORIGIN: 'https://same.example', SITE_BASE_URL: 'https://same.example' }))
```

- [ ] Runfromfrontend`node --test scripts/__tests__/vercel-proxy.test.mjs`; recordfeaturemissingRED.
- [ ] ImplementURLvalidation, routesheaderpolicyandbuildwriter usingonlynodebuiltins. Destcatchall`/(.*)` preservespath/queryandhasnomethodfilter. Scriptfailsbeforewritingoninvalidconfiganddoesnotprintenvsecrets. Removeonlyownedoldconfig/staticoutput, neverprojectfiles.
- [ ] Setvercel.jsonframeworknullanddedicatedbuildcommand; normalVitebuildscriptunchanged. Add`test:deployment`scriptcoveringthistestfileandDockerconfigtest.
- [ ] RuntestsGREEN; checkscriptformatandsyntax, generatedconfigvalidJSONandnoreferencehost. DocumentnewVercelOtherpreset/envs/backendfirstrollout; noactualclouddeploy.

### Task2: Docker build and persistence

Files: modifybackend/Dockerfile, backend/docker-compose.yml; create .dockerignore, backend/docker-compose.sqlite.yml and `frontend/scripts/__tests__/docker-config.test.mjs`.

- [ ] WriteREDconfigtestsreadingsourcefiles: rootcontext/explicitbackend+frontendCOPY/Mavenimage/JREnonroot/selfblog.jar; no.envCOPY; allthreedatadirectories; sameMySQLvolume namesandnewgalleryvolume; SQLiteDBinthepersistentdatadir; healthtoolactuallyinstalled.

```js
assert.match(dockerfile, /FROM maven:.*temurin-21/)
assert.match(dockerfile, /COPY --from=build .*selfblog\.jar/)
assert.match(mysqlCompose, /context: \.\./)
assert.match(sqliteCompose, /jdbc:sqlite:\/app\/data\//)
```

- [ ] Run`node --test scripts/__tests__/docker-config.test.mjs`RED. Implementroot-contextmultistagebuildwithMavenfrontendplugin; copyonlyruntimeJAR; installhealthchecktoolbeforeUSER; writabledata/uploads/gallery-mediaownedbynonroot.
- [ ] RetainMySQLservice/defaultvolumesandlocalloopbackport; fixcontextandpersistgallery. AddseparateSQLiteComposewithdataandmedia volumes, explicitDB_TYPE/URL. Requiresecretswithoutbaking.env. Nochanginguser'sruntime.env.
- [ ] RuntestsGREEN and, onlyifavailable,`docker compose -f backend/docker-compose.yml config --quiet`andSQLiteequivalentwiththrowawayenv; neverleaksecretsintooutput. WithoutDockerrecordblocked, notpassed.
- [ ] UpdateDEPLOYupgradeinstructions: backupentiredatabase/WAL/media, exportoldunmountedgallerydir, existingMySQLmigrationversusnewemptySQL, noautomaticdatamove/volumeremoval. PairconfigcheckswithactualMavenpackaginginmainverification.

### Task3: Local protocol smoke and final review

- [ ] UsegeneratedroutingconfiginlocalproxyoverdisposableJAR toverifyHTML404/head/body, API401/POSTbody/auth, UTF8query, publicmediaRangeandprotectedalbumdenial. ThisisnotrealVercelvalidation.
- [ ] Full`npm run test:deployment`andcoreplanregressions; inspectactualsource/lockchanges. Includefailedinputandsecret-safeerrors.
- [ ] RecordDocker/realVercel/SMPTextensionsonlyifactuallyrun; noexistingservices/dataorcloudresourceschanged.
- [ ] Commit/pushonlyscopewithupdatedREADME/CHANGELOG/DEPLOYandverificationrecord, aspartofapproveddelivery.

## Plan Review

Approvedspecsections9–10arecoveredbytasks1–2, section11bytask3andcoreplanTask7. URLsandheaderpolicyarepurefunctionsunderNodebuiltin tests; lifecycle/filesystemtestsusetempdirectories. Runtimecapabilitylimitsandproductiondomainconfigurationareexplicit, notsilentlyassumed.
