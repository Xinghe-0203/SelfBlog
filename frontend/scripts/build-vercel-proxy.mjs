import { fileURLToPath } from 'node:url'
import { writeProxyOutput } from './vercel-proxy.mjs'

try {
  await writeProxyOutput(process.env, fileURLToPath(new URL('../.vercel/output/', import.meta.url)))
  console.log('Vercel proxy routing generated; HTML and assets remain owned by the source JAR.')
} catch (error) {
  console.error(error instanceof Error ? error.message : 'Vercel proxy configuration failed')
  process.exitCode = 1
}
