// Static source contracts only. Passing these does not mean a Docker daemon built/ran an image.
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

const root = new URL('../../../', import.meta.url)
const read = (path) => readFile(new URL(path, root), 'utf8')

test('Docker builds the exact JAR with Maven and both repository projects', async () => {
  const dockerfile = await read('backend/Dockerfile')
  assert.match(dockerfile, /FROM maven:.*temurin-21 AS build/)
  assert.match(dockerfile, /COPY backend\/pom\.xml/)
  assert.match(dockerfile, /COPY backend\/src/)
  assert.match(dockerfile, /COPY frontend\//)
  assert.match(dockerfile, /COPY --from=build \/build\/backend\/target\/selfblog\.jar/)
  assert.doesNotMatch(dockerfile, /COPY .*\.env/)
  assert.doesNotMatch(dockerfile, /backend-0\.0\.1|skip\.npm/)
})

test('runtime is nonroot, owns media/data directories and contains its healthcheck tool', async () => {
  const dockerfile = await read('backend/Dockerfile')
  assert.match(dockerfile, /FROM eclipse-temurin:21-jre/)
  assert.match(dockerfile, /apt-get install .*curl/)
  for (const path of ['/app/data', '/app/uploads', '/app/gallery-media'])
    assert.ok(dockerfile.includes(path))
  assert.match(dockerfile, /USER appuser/)
  assert.match(dockerfile, /HEALTHCHECK[\s\S]*curl -f/)
  assert.match(dockerfile, /ENTRYPOINT \["java", "-jar", "\/app\/selfblog\.jar"\]/)
})

test('existing MySQL compose keeps volumes and root build context', async () => {
  const compose = await read('backend/docker-compose.yml')
  assert.match(compose, /context: \.\./)
  assert.match(compose, /dockerfile: backend\/Dockerfile/)
  assert.match(compose, /DB_TYPE: mysql/)
  assert.match(compose, /mysql_data:\/var\/lib\/mysql/)
  assert.match(compose, /app_uploads:\/app\/uploads/)
  assert.match(compose, /app_gallery_media:\/app\/gallery-media/)
  assert.match(compose, /127\.0\.0\.1:8825:8825/)
  assert.match(compose, /\$\$\{MYSQL_ROOT_PASSWORD\}/)
  assert.doesNotMatch(compose, /wget/)
})

test('SQLite deployment has explicit persistent database and both media stores', async () => {
  const compose = await read('backend/docker-compose.sqlite.yml')
  assert.match(compose, /DB_TYPE: sqlite/)
  assert.match(compose, /jdbc:sqlite:\/app\/data\/campus_blog\.db/)
  assert.match(compose, /sqlite_data:\/app\/data/)
  assert.match(compose, /app_uploads:\/app\/uploads/)
  assert.match(compose, /app_gallery_media:\/app\/gallery-media/)
  assert.match(compose, /context: \.\./)
  assert.doesNotMatch(compose, /schema\.sql|mysql:/)
})

test('root build context excludes secrets, existing data and local-only work', async () => {
  const ignore = await read('.dockerignore')
  for (const pattern of [
    '**/.env',
    '**/*.db',
    '**/*-wal',
    '**/*-shm',
    '**/uploads',
    '**/gallery-media',
    '.git',
    '**/node_modules',
    '**/target',
    '**/dist',
    'task_plan.md',
    'findings.md',
    'progress.md'
  ])
    assert.ok(ignore.split(/\r?\n/).includes(pattern), pattern)
})

test('Vercel project does not auto-generate a Vite SPA fallback', async () => {
  const config = JSON.parse(await read('frontend/vercel.json'))
  const manifest = JSON.parse(await read('frontend/package.json'))
  assert.equal(config.framework, null)
  assert.equal(config.buildCommand, 'node scripts/build-vercel-proxy.mjs')
  assert.equal(config.outputDirectory, undefined)
  assert.equal(config.rewrites, undefined)
  assert.equal(manifest.scripts.build, 'vue-tsc --noEmit && vite build')
})
