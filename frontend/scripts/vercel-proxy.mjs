import { mkdir, readdir, writeFile, rename } from 'node:fs/promises'
import { join } from 'node:path'

function httpsOrigin(value, name) {
  const invalid = () => new Error(`${name} must be a public HTTPS origin without credentials, path or query`)
  if (typeof value !== 'string' || !/^https:\/\/[^/?#]+\/?$/i.test(value) || /[\s\\,%]/u.test(value)) throw invalid()
  let url
  try { url = new URL(value) } catch { throw invalid() }
  if (url.username || url.password || !url.hostname || url.pathname !== '/' || url.search || url.hash) throw invalid()
  const host = url.hostname.toLowerCase()
  if (host === 'localhost' || host.endsWith('.localhost') || host.startsWith('[') || /^(?:0\.|10\.|127\.|169\.254\.|192\.168\.|172\.(?:1[6-9]|2\d|3[01])\.)/.test(host)) throw invalid()
  return url.origin
}

/** Build Output v3 routes, not runtime environment substitution in vercel.json. */
export function createProxyConfig(env) {
  const upstream = httpsOrigin(env.SELFBLOG_BACKEND_ORIGIN, 'SELFBLOG_BACKEND_ORIGIN')
  const publicOrigin = httpsOrigin(env.SITE_BASE_URL, 'SITE_BASE_URL')
  const upstreamHost = new URL(upstream).hostname
  const deploymentHosts = ['VERCEL_URL', 'VERCEL_BRANCH_URL', 'VERCEL_PROJECT_PRODUCTION_URL']
    .map(key => env[key]).filter(Boolean).map(host => String(host).toLowerCase().replace(/^https?:\/\//, '').replace(/\/$/, ''))
  if (upstream === publicOrigin || deploymentHosts.includes(upstreamHost)) throw new Error('SELFBLOG_BACKEND_ORIGIN must not point to the public site or this Vercel deployment')
  const headers = {
    'Cache-Control': 'no-store',
    'CDN-Cache-Control': 'no-store',
    'Vercel-CDN-Cache-Control': 'no-store',
    'x-vercel-enable-rewrite-caching': '0',
    'X-Frame-Options': 'DENY',
    'X-Content-Type-Options': 'nosniff',
    'Referrer-Policy': 'strict-origin-when-cross-origin'
  }
  if (env.VERCEL_ENV !== 'production') headers['X-Robots-Tag'] = 'noindex,nofollow'
  return { version: 3, routes: [{ src: '/(.*)', dest: `${upstream}/$1`, headers }] }
}

export async function writeProxyOutput(env, outputDirectory) {
  const config = createProxyConfig(env)
  await mkdir(outputDirectory, { recursive: true })
  const entries = await readdir(outputDirectory)
  if (entries.some(name => !['config.json', 'static'].includes(name))) throw new Error('Proxy output contains unexpected artifacts; use a clean dedicated output directory')
  const staticDirectory = join(outputDirectory, 'static')
  await mkdir(staticDirectory, { recursive: true })
  if ((await readdir(staticDirectory)).length) throw new Error('Proxy static directory must be empty; refusing to deploy or delete an old frontend')
  const temporary = join(outputDirectory, 'config.json.tmp')
  await writeFile(temporary, JSON.stringify(config, null, 2) + '\n', 'utf8')
  await rename(temporary, join(outputDirectory, 'config.json'))
}
