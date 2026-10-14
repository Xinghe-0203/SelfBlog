<template>
  <div v-if="siteStore.isMusicEnabled" class="music-player" :class="{ playing }">
    <audio ref="audio" :src="siteStore.settings.musicUrl" preload="none" loop @play="playing = true" @pause="playing = false" />
    <button type="button" class="music-toggle" :aria-label="playing ? '暂停背景音乐' : '播放背景音乐'" @click="toggle">
      <span aria-hidden="true">{{ playing ? 'Ⅱ' : '▶' }}</span>
    </button>
    <div class="music-copy">
      <strong>{{ siteStore.settings.musicTitle }}</strong>
      <small>{{ blocked ? '点击播放（浏览器限制自动播放）' : playing ? '正在播放' : '背景音乐' }}</small>
    </div>
    <span class="music-bars" aria-hidden="true"><i></i><i></i><i></i></span>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useSiteStore } from '@/stores/site'

const siteStore = useSiteStore()
const audio = ref<HTMLAudioElement | null>(null)
const playing = ref(false)
const blocked = ref(false)

async function tryAutoplay() {
  if (!siteStore.isMusicEnabled || !audio.value) return
  try {
    await audio.value.play()
    blocked.value = false
  } catch {
    blocked.value = true
  }
}

async function toggle() {
  if (!audio.value) return
  if (audio.value.paused) {
    try {
      await audio.value.play()
      blocked.value = false
    } catch {
      blocked.value = true
    }
  } else {
    audio.value.pause()
  }
}

onMounted(() => void tryAutoplay())
watch(() => [siteStore.isMusicEnabled, siteStore.settings.musicUrl] as const, () => void tryAutoplay())
</script>

<style scoped>
.music-player {
  position: fixed;
  z-index: 40;
  right: 22px;
  bottom: 22px;
  display: flex;
  align-items: center;
  gap: 11px;
  max-width: min(310px, calc(100vw - 28px));
  padding: 9px 14px 9px 9px;
  border: 1px solid var(--border-solid);
  border-radius: 999px;
  background: var(--surface-primary);
  box-shadow: 0 10px 35px rgb(14 34 57 / 15%);
  color: var(--text-primary);
}
.music-player audio { display: none; }
.music-toggle {
  display: grid;
  flex: 0 0 36px;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  cursor: pointer;
}
.music-copy { display: grid; min-width: 0; gap: 2px; }
.music-copy strong, .music-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.music-copy strong { font-size: .76rem; }
.music-copy small { color: var(--text-muted); font-size: .65rem; }
.music-bars { display: flex; align-items: center; gap: 2px; height: 19px; margin-left: 2px; }
.music-bars i { width: 2px; height: 5px; border-radius: 2px; background: var(--primary); opacity: .55; }
.playing .music-bars i { animation: beat .65s ease-in-out infinite alternate; }
.playing .music-bars i:nth-child(2) { animation-delay: .2s; }
.playing .music-bars i:nth-child(3) { animation-delay: .4s; }
@keyframes beat { to { height: 17px; opacity: 1; } }
@media (max-width: 480px) { .music-player { right: 12px; bottom: 12px; } }
</style>
