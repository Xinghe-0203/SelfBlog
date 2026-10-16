<script setup lang="ts">
import { useMusicStore } from '@/stores/music'
import Icon from '@/components/common/Icon.vue'

defineProps<{ open: boolean }>()
const emit = defineEmits<{ toggle: [trigger: HTMLElement]; close: [] }>()
const music = useMusicStore()

function time(value: number): string {
  if (!Number.isFinite(value) || value < 0) return '--:--'
  return `${Math.floor(value / 60)}:${String(Math.floor(value % 60)).padStart(2, '0')}`
}
function togglePlayback(): void {
  if (music.playing || music.loading) music.pause()
  else void music.play()
}
</script>

<template>
  <template v-if="music.available">
    <button type="button" class="music-trigger" :class="{ 'is-playing': music.playing }" aria-label="打开音乐面板" aria-controls="nav-music-panel" :aria-expanded="open" @click="emit('toggle', $event.currentTarget as HTMLElement)">
      <span v-if="music.playing" class="music-bars" aria-hidden="true"><i /><i /><i /></span>
      <Icon v-else name="music" :size="18" />
      <span class="music-word">音乐</span>
    </button>
    <Transition name="music-panel" @before-leave="$event.setAttribute('inert', '')">
      <section v-if="open" id="nav-music-panel" class="music-panel functional-glass" data-nav-panel role="region" aria-label="背景音乐">
        <header class="player-heading">
          <div class="track-mark" aria-hidden="true"><Icon name="music" :size="23" /></div>
          <div class="track-copy"><strong>{{ music.title }}</strong><span>{{ music.loading ? '正在加载…' : music.playing ? '正在播放' : '已暂停' }}</span></div>
          <button type="button" class="player-close" aria-label="关闭音乐面板" @click="emit('close')"><Icon name="close" :size="17" /></button>
        </header>
        <div class="track-progress">
          <input type="range" aria-label="播放进度" min="0" :max="music.duration || 0" :value="music.currentTime" :disabled="music.duration <= 0" step="0.1" @input="music.seek(Number(($event.target as HTMLInputElement).value))" />
          <div class="track-times"><span>{{ time(music.currentTime) }}</span><span>{{ music.duration > 0 ? time(music.duration) : '--:--' }}</span></div>
        </div>
        <div class="transport-row">
          <button type="button" class="loop-button" :aria-pressed="music.loop" aria-label="单曲循环" @click="music.setLoop(!music.loop)"><Icon name="repeat" :size="18" /></button>
          <button type="button" class="player-play" :aria-label="music.loading ? '取消加载' : music.playing ? '暂停音乐' : '播放音乐'" :aria-busy="music.loading" @click="togglePlayback"><Icon :name="music.playing || music.loading ? 'pause' : 'play'" :size="22" /></button>
          <span class="transport-label">{{ music.loop ? '单曲循环' : '播完即停' }}</span>
        </div>
        <div v-if="music.volumeSupported" class="volume-row"><Icon name="volume" :size="17" /><input type="range" aria-label="音量" min="0" max="1" step="0.01" :value="music.volume" @input="music.setVolume(Number(($event.target as HTMLInputElement).value))" /><span>{{ Math.round(music.volume * 100) }}%</span></div>
        <p v-else class="volume-hint">请使用设备音量键调整音量。</p>
        <div v-if="music.error" class="player-error" role="status"><p>{{ music.error }}</p><button type="button" class="retry-button" @click="music.play">重试播放</button></div>
      </section>
    </Transition>
  </template>
</template>

<style scoped>
.music-trigger { display: flex; align-items: center; justify-content: center; flex-shrink: 0; gap: 6px; min-width: 44px; height: 44px; padding-inline: 10px; border-radius: var(--radius); color: var(--text-secondary); font-size: 12px; }
.music-trigger:hover, .music-trigger[aria-expanded='true'], .music-trigger.is-playing { color: var(--accent); background: var(--accent-light); }
.music-bars { display: flex; align-items: center; justify-content: center; gap: 2px; width: 18px; height: 18px; }
.music-bars i { width: 2px; height: 6px; border-radius: var(--radius-full); background: currentColor; animation: music-beat 650ms ease-in-out infinite alternate; }
.music-bars i:nth-child(2) { animation-delay: -200ms; height: 13px; }
.music-bars i:nth-child(3) { animation-delay: -400ms; }
@keyframes music-beat { to { height: 16px; } }
.music-panel { position: absolute; top: calc(100% + 10px); right: 44px; z-index: var(--z-nav-panel); width: min(302px, calc(100vw - 32px)); padding: 20px; border-radius: var(--radius-2xl); max-height: calc(100dvh - 90px); overflow-y: auto; overscroll-behavior: contain; color: var(--text-primary); }
.player-heading { display: flex; align-items: center; gap: 11px; }
.track-mark { display: grid; flex: 0 0 44px; place-items: center; width: 44px; height: 44px; border-radius: var(--radius); background: var(--accent-light); color: var(--accent); }
.track-copy { display: grid; min-width: 0; flex: 1; gap: 4px; }
.track-copy strong { font-size: 13px; line-height: 1.5; overflow-wrap: anywhere; }
.track-copy span { font-size: 11px; color: var(--text-secondary); }
.player-close { display: grid; place-items: center; width: 44px; height: 44px; margin: -10px -10px -10px 0; border-radius: var(--radius); color: var(--text-secondary); }
.player-close:hover { background: var(--bg-secondary); }
.track-progress { margin-top: 18px; }
input[type='range'] { display: block; width: 100%; min-width: 0; height: 24px; margin: 0; accent-color: var(--accent); cursor: pointer; }
input:disabled { opacity: .45; cursor: not-allowed; }
.track-times { display: flex; justify-content: space-between; color: var(--text-secondary); font-size: 11px; font-variant-numeric: tabular-nums; }
.transport-row { display: grid; grid-template-columns: 1fr 48px 1fr; align-items: center; gap: 10px; margin-block: 15px; }
.player-play { display: grid; place-items: center; width: 48px; height: 48px; border-radius: var(--radius-full); background: var(--contrast-bg); color: var(--contrast-fg); transition: transform var(--transition-fast); }
.player-play:active { transform: scale(.95); }
.loop-button { display: grid; place-items: center; width: 44px; height: 44px; border-radius: var(--radius); color: var(--text-secondary); }
.loop-button[aria-pressed='true'] { color: var(--accent); background: var(--accent-light); }
.transport-label { text-align: right; font-size: 10px; color: var(--text-secondary); }
.volume-row { display: flex; align-items: center; gap: 9px; color: var(--text-secondary); }
.volume-row span { min-width: 30px; text-align: right; font-size: 10px; font-variant-numeric: tabular-nums; }
.volume-hint { color: var(--text-secondary); font-size: 12px; }
.player-error { margin-top: 14px; padding-top: 12px; border-top: 1px solid var(--border-light); font-size: 12px; line-height: 1.7; }
.retry-button { min-height: 44px; color: var(--accent); font-size: 12px; font-weight: 600; }
.music-panel-enter-active, .music-panel-leave-active { transition: opacity 180ms ease, transform 180ms var(--ease-out); }
.music-panel-enter-from, .music-panel-leave-to { opacity: 0; transform: translateY(-5px); }
@media (max-width: 1100px) { .music-word { display: none; } .music-trigger { width: 44px; padding: 0; } }
@media (max-width: 720px) { .music-panel { right: 0; width: min(302px, calc(100vw - 20px)); } }
@media (prefers-reduced-motion: reduce) { .music-bars i { animation: none; } .music-panel-enter-from, .music-panel-leave-to { transform: none; } }
</style>
