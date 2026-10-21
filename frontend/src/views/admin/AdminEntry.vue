<template>
  <AdminLayout v-if="userStore.isAdmin" />
  <Login v-else />
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AdminLayout from './AdminLayout.vue'
import Login from '@/views/auth/Login.vue'
import { useUserStore } from '@/stores/user'
import { useSiteStore } from '@/stores/site'
import { resolveAdminRedirect } from '@/router/helpers'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

watch(
  () => [route.path, userStore.isAdmin] as const,
  ([path, isAdmin]) => {
    if (isAdmin && path === siteStore.adminRoot) {
      // `?redirect=` 来自 URL：只放行已注册的后台路由（见 resolveAdminRedirect）
      const destination = resolveAdminRedirect(
        router,
        siteStore.adminRoot,
        route.query.redirect,
        `${siteStore.adminRoot}/dashboard`
      )
      void router.replace(destination).catch(() => undefined)
    }
  },
  { immediate: true }
)
</script>
