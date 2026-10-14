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

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const siteStore = useSiteStore()

watch(
  () => [route.path, userStore.isAdmin] as const,
  ([path, isAdmin]) => {
    if (isAdmin && path === siteStore.adminRoot) {
      const redirect = route.query.redirect
      const destination = typeof redirect === 'string' && redirect.startsWith(siteStore.adminRoot)
        ? redirect
        : `${siteStore.adminRoot}/dashboard`
      void router.replace(destination)
    }
  },
  { immediate: true }
)
</script>
