import { after, test } from 'node:test'
import assert from 'node:assert/strict'
import { mkdtemp, readFile, readdir, writeFile, mkdir, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { createProxyConfig, writeProxyOutput } from '../vercel-proxy.mjs'

const production = {
  SELFBLOG_BACKEND_ORIGIN: 'https://origin.example.com/',
  SITE_BASE_URL: 'https://blog.example.com',
  VERCEL_ENV: 'production'
}
const directory = await mkdtemp(join(tmpdir(), 'selfblog-proxy-test-'))
after(() => rm(directory, { recursive: true, force: true }))

test('uses one external source for HTML, API, assets, uploads and discovery', () => {
  const config = createProxyConfig(production)
  assert.equal(config.version, 3)
  const route = config.routes.at(-1)
  assert.equal(route.dest, 'https://origin.example.com/$1')
  assert.equal(route.methods, undefined)
  const match = new RegExp(`^${route.src}$`)
  for (const path of [
    '/',
    '/post/12',
    '/pages/projects',
    '/api/user/login',
    '/assets/index-hash.js',
    '/uploads/site-assets/music/a.mp3',
    '/feed.xml',
    '/sitemap-posts-2.xml'
  ]) {
    assert.equal(path.replace(match, route.dest), `https://origin.example.com${path}`)
  }
})

test('disables shared rewrite caching and retains security headers', () => {
  const route = createProxyConfig(production).routes.at(-1)
  assert.equal(route.headers['Cache-Control'], 'no-store')
  assert.equal(route.headers['CDN-Cache-Control'], 'no-store')
  assert.equal(route.headers['Vercel-CDN-Cache-Control'], 'no-store')
  assert.equal(route.headers['x-vercel-enable-rewrite-caching'], '0')
  assert.equal(route.headers['X-Frame-Options'], 'DENY')
  assert.equal(route.headers['X-Content-Type-Options'], 'nosniff')
  assert.equal(route.headers['X-Robots-Tag'], undefined)
})

test('preview and an unspecified environment are never indexable', () => {
  for (const VERCEL_ENV of ['preview', 'development', undefined]) {
    assert.equal(
      createProxyConfig({ ...production, VERCEL_ENV }).routes.at(-1).headers['X-Robots-Tag'],
      'noindex,nofollow'
    )
  }
})

for (const value of [
  '',
  undefined,
  'http://origin.example.com',
  'https://user:my-secret@example.com',
  'https://example.com/path',
  'https://example.com?q=x',
  'https://example.com/#x',
  'https://example.com\\evil',
  'https://example.com,other.test',
  ' https://example.com',
  'https://example.com\n',
  'https://localhost',
  'https://127.0.0.1',
  'https://10.0.0.1'
]) {
  test(`rejects invalid backend origin without leaking its value: ${JSON.stringify(value)?.replace('my-secret', 'REDACTED')}`, () => {
    assert.throws(
      () => createProxyConfig({ ...production, SELFBLOG_BACKEND_ORIGIN: value }),
      (error) => {
        assert.match(error.message, /SELFBLOG_BACKEND_ORIGIN/)
        assert.doesNotMatch(error.message, /my-secret/)
        return true
      }
    )
  })
}

test('requires a valid public origin, rejects a self-referencing upstream', () => {
  assert.throws(() => createProxyConfig({ ...production, SITE_BASE_URL: '' }))
  assert.throws(() =>
    createProxyConfig({ ...production, SITE_BASE_URL: 'https://origin.example.com' })
  )
  for (const name of ['VERCEL_URL', 'VERCEL_BRANCH_URL', 'VERCEL_PROJECT_PRODUCTION_URL']) {
    assert.throws(() => createProxyConfig({ ...production, [name]: 'origin.example.com' }))
  }
})

test('writes only a routing deployment, not another frontend bundle', async () => {
  const out = join(directory, 'output')
  await writeFile(join(directory, 'keep.txt'), 'unrelated')
  await writeProxyOutput(production, out)
  const config = JSON.parse(await readFile(join(out, 'config.json'), 'utf8'))
  assert.equal(config.version, 3)
  assert.deepEqual((await readdir(out)).sort(), ['config.json', 'static'])
  assert.deepEqual(await readdir(join(out, 'static')), [])
  assert.equal(await readFile(join(directory, 'keep.txt'), 'utf8'), 'unrelated')
  await writeProxyOutput({ ...production, VERCEL_ENV: 'preview' }, out)
  assert.equal(
    JSON.parse(await readFile(join(out, 'config.json'), 'utf8')).routes.at(-1).headers[
      'X-Robots-Tag'
    ],
    'noindex,nofollow'
  )
})

test('refuses stale static artifacts instead of deploying or deleting them', async () => {
  const out = join(directory, 'stale')
  await mkdir(join(out, 'static'), { recursive: true })
  await writeFile(join(out, 'static', 'index.html'), 'old build')
  await assert.rejects(writeProxyOutput(production, out), /static/)
  assert.equal(await readFile(join(out, 'static', 'index.html'), 'utf8'), 'old build')
})

test('invalid configuration does not touch a previous output', async () => {
  const out = join(directory, 'existing')
  await writeProxyOutput(production, out)
  const before = await readFile(join(out, 'config.json'), 'utf8')
  await assert.rejects(writeProxyOutput({ ...production, SITE_BASE_URL: '' }, out))
  assert.equal(await readFile(join(out, 'config.json'), 'utf8'), before)
})
