<script setup lang="ts">
import { onBeforeUnmount, watch, type ComponentPublicInstance } from 'vue'
import { useRoute } from 'vue-router'
import { useMusicStore } from '@/stores/music'
import { useSiteStore } from '@/stores/site'

const site = useSiteStore()
const music = useMusicStore()
const route = useRoute()

watch(
  () => [site.settings.musicEnabled, site.settings.musicUrl, site.settings.musicTitle] as const,
  ([enabled, url, title]) => music.configure({ enabled: enabled === 'true', url, title }),
  { immediate: true, flush: 'sync' }
)
watch(() => route.meta.adminArea === true, music.setSuspended, { immediate: true, flush: 'sync' })

function attach(element: Element | ComponentPublicInstance | null): void {
  music.attach(element instanceof HTMLAudioElement ? element : null)
}
onBeforeUnmount(() => music.attach(null))
</script>

<template>
  <audio
    v-if="music.available"
    :key="music.source"
    :ref="attach"
    :src="music.source"
    preload="none"
    hidden
  />
</template>
