/** Admin routes are created at startup from the server-managed path. */
import type { RouteRecordRaw } from 'vue-router'
import { lazyLoad } from '../helpers'

export function createAdminRoutes(adminPath: string): RouteRecordRaw[] {
  const root = `/${adminPath}`
  const adminMeta = { requiresAuth: true, requiresAdmin: true, adminArea: true }

  return [
    {
      path: root,
      name: 'AdminEntry',
      component: lazyLoad(() => import('@/views/admin/AdminEntry.vue')),
      meta: { title: '作者后台', adminArea: true },
      children: [
        {
          path: 'dashboard',
          name: 'AdminDashboard',
          component: lazyLoad(() => import('@/views/admin/Dashboard.vue')),
          meta: { ...adminMeta, title: '后台概览' }
        },
        {
          path: 'posts',
          name: 'AdminPosts',
          component: lazyLoad(() => import('@/views/admin/Posts.vue')),
          meta: { ...adminMeta, title: '文章管理' }
        },
        {
          path: 'write',
          name: 'AdminPostCreate',
          component: lazyLoad(() => import('@/views/post/PostEdit.vue')),
          meta: { ...adminMeta, title: '发布文章' }
        },
        {
          path: 'edit/:id',
          name: 'AdminPostEdit',
          component: lazyLoad(() => import('@/views/post/PostEdit.vue')),
          meta: { ...adminMeta, title: '编辑文章' }
        },
        {
          path: 'tags',
          name: 'AdminTags',
          component: lazyLoad(() => import('@/views/admin/TagsManagement.vue')),
          meta: { ...adminMeta, title: '标签管理' }
        },
        {
          path: 'visitors',
          name: 'AdminVisitors',
          component: lazyLoad(() => import('@/views/admin/Visitors.vue')),
          meta: { ...adminMeta, title: '访客统计' }
        },
        {
          path: 'comments',
          name: 'AdminComments',
          component: lazyLoad(() => import('@/views/admin/Comments.vue')),
          meta: { ...adminMeta, title: '评论管理' }
        },
        {
          path: 'albums',
          name: 'AdminAlbums',
          component: lazyLoad(() => import('@/views/admin/Albums.vue')),
          meta: { ...adminMeta, title: '相册管理' }
        },
        {
          path: 'settings',
          name: 'AdminSettings',
          component: lazyLoad(() => import('@/views/admin/Settings.vue')),
          meta: { ...adminMeta, title: '网站设置' }
        }
      ]
    }
  ]
}
