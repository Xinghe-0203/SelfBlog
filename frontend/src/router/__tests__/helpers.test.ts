import { describe, it, expect } from 'vitest'
import { createRouter, createMemoryHistory, type RouteRecordRaw } from 'vue-router'
import { resolveAdminRedirect } from '../helpers'

const adminRoot = '/admin'
const fallback = `${adminRoot}/dashboard`

function createAdminRouter(): ReturnType<typeof createRouter> {
  const routes: RouteRecordRaw[] = [
    { path: '/', name: 'Home', component: { template: '<div />' } },
    {
      path: adminRoot,
      name: 'AdminEntry',
      component: { template: '<div />' },
      meta: { adminArea: true },
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: { template: '<div />' },
          meta: { adminArea: true }
        },
        {
          path: 'posts',
          name: 'AdminPosts',
          component: { template: '<div />' },
          meta: { adminArea: true }
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'NotFound',
      component: { template: '<div />' }
    }
  ]
  return createRouter({ history: createMemoryHistory(), routes })
}

describe('resolveAdminRedirect', () => {
  it('keeps a registered admin route and preserves its query', () => {
    const router = createAdminRouter()
    expect(resolveAdminRedirect(router, adminRoot, '/admin/posts?page=2', fallback)).toBe(
      '/admin/posts?page=2'
    )
  })

  it('falls back when the redirect is missing or not a string', () => {
    const router = createAdminRouter()
    expect(resolveAdminRedirect(router, adminRoot, undefined, fallback)).toBe(fallback)
    expect(resolveAdminRedirect(router, adminRoot, ['/admin/posts'], fallback)).toBe(fallback)
    expect(resolveAdminRedirect(router, adminRoot, '', fallback)).toBe(fallback)
  })

  it('rejects protocol-relative and absolute URLs', () => {
    const router = createAdminRouter()
    expect(resolveAdminRedirect(router, adminRoot, '//evil.com', fallback)).toBe(fallback)
    expect(resolveAdminRedirect(router, adminRoot, 'https://evil.com', fallback)).toBe(fallback)
  })

  it('rejects a double-slash admin path that only looks like an admin route', () => {
    const router = createAdminRouter()
    // 只做 startsWith('/admin') 时这条会被放行：resolve 不会折叠双斜杠
    expect(resolveAdminRedirect(router, adminRoot, '/admin//evil.com', fallback)).toBe(fallback)
  })

  it('rejects paths outside the admin namespace and unknown admin paths', () => {
    const router = createAdminRouter()
    expect(resolveAdminRedirect(router, adminRoot, '/post/1', fallback)).toBe(fallback)
    expect(resolveAdminRedirect(router, adminRoot, '/admin/nope', fallback)).toBe(fallback)
  })

  it('accepts the admin root itself', () => {
    const router = createAdminRouter()
    expect(resolveAdminRedirect(router, adminRoot, adminRoot, fallback)).toBe(adminRoot)
  })
})
