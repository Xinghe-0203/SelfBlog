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
    <button
      type="button"
      class="music-trigger"
      :class="{ 'is-playing': music.playing }"
      aria-label="打开音乐面板"
      aria-controls="nav-music-panel"
      :aria-expanded="open"
      @click="emit('toggle', $event.currentTarget as HTMLElement)"
    >
      <span class="music-bars" aria-hidden="true"><i /><i /><i /></span>
      <span class="music-word">听一会儿</span>
    </button>
    <Transition name="music-panel" @before-leave="$event.setAttribute('inert', '')">
      <section
        v-if="open"
        id="nav-music-panel"
        class="music-panel functional-glass"
        data-nav-panel
        role="region"
        aria-label="背景音乐"
      >
        <header class="player-heading">
          <div class="track-mark" aria-hidden="true"><Icon name="music" :size="23" /></div>
          <div class="track-copy">
            <strong>{{ music.title }}</strong
            ><span>{{ music.loading ? '正在加载…' : music.playing ? '正在播放' : '已暂停' }}</span>
          </div>
          <button
            type="button"
            class="player-close"
            aria-label="关闭音乐面板"
            @click="emit('close')"
          >
            <Icon name="close" :size="17" />
          </button>
        </header>
        <div class="track-progress">
          <input
            type="range"
            aria-label="播放进度"
            min="0"
            :max="music.duration || 0"
            :value="music.currentTime"
            :style="{
              '--range-progress': `${music.duration > 0 ? Math.min(100, (music.currentTime / music.duration) * 100) : 0}%`
            }"
            :disabled="music.duration <= 0"
            step="0.1"
            @input="music.seek(Number(($event.target as HTMLInputElement).value))"
          />
          <div class="track-times">
            <span>{{ time(music.currentTime) }}</span
            ><span>{{ music.duration > 0 ? time(music.duration) : '--:--' }}</span>
          </div>
        </div>
        <div class="transport-row">
          <span class="transport-note" aria-hidden="true"><Icon name="music" :size="18" /></span>
          <button
            type="button"
            class="player-play"
            :aria-label="music.loading ? '取消加载' : music.playing ? '暂停音乐' : '播放音乐'"
            :aria-busy="music.loading"
            @click="togglePlayback"
          >
            <Icon :name="music.playing || music.loading ? 'pause' : 'play'" :size="22" />
          </button>
          <button
            type="button"
            class="loop-button"
            :aria-pressed="music.loop"
            aria-label="单曲循环"
            @click="music.setLoop(!music.loop)"
          >
            <Icon name="repeat" :size="18" />
          </button>
        </div>
        <div v-if="music.volumeSupported" class="volume-row">
          <Icon name="volume" :size="17" /><input
            type="range"
            aria-label="音量"
            min="0"
            max="1"
            step="0.01"
            :value="music.volume"
            :style="{ '--range-progress': `${music.volume * 100}%` }"
            @input="music.setVolume(Number(($event.target as HTMLInputElement).value))"
          />
        </div>
        <p v-else class="volume-hint">请使用设备音量键调整音量。</p>
        <div v-if="music.error" class="player-error" role="status">
          <p>{{ music.error }}</p>
          <button type="button" class="retry-button" @click="music.play">重试播放</button>
        </div>
      </section>
    </Transition>
  </template>
</template>

<style scoped>
.music-trigger {
  position: relative;
  isolation: isolate;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  gap: 8px;
  min-width: 44px;
  height: 44px;
  padding-inline: 12px;
  border-radius: var(--radius);
  color: var(--text-primary);
  font-size: var(--text-2xs);
  white-space: nowrap;
}
.music-trigger::before {
  position: absolute;
  content: '';
  z-index: -1;
  inset: 5px 0;
  border-radius: inherit;
}
.music-trigger:hover,
.music-trigger[aria-expanded='true'],
.music-trigger.is-playing {
  color: var(--accent);
}
.music-trigger:hover::before,
.music-trigger[aria-expanded='true']::before {
  background: var(--accent-light);
}
.music-bars {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  width: 18px;
  height: 18px;
}
.music-bars {
  color: var(--accent);
}
.music-bars i {
  width: 2px;
  height: 6px;
  border-radius: var(--radius-full);
  background: currentColor;
}
.is-playing .music-bars i {
  animation: music-beat 650ms ease-in-out infinite alternate;
}
.music-bars i:nth-child(2) {
  animation-delay: -200ms;
  height: 13px;
}
.music-bars i:nth-child(3) {
  animation-delay: -400ms;
}
@keyframes music-beat {
  to {
    height: 16px;
  }
}
.music-panel {
  position: absolute;
  top: calc(100% + 10px);
  right: 44px;
  z-index: var(--z-nav-panel);
  width: min(302px, calc(100vw - 32px));
  padding: 20px;
  border-radius: var(--radius-2xl);
  max-height: calc(100dvh - 90px);
  overflow-y: auto;
  overscroll-behavior: contain;
  color: var(--text-primary);
  box-shadow:
    var(--shadow-lg),
    inset 0 1px 0 var(--functional-glass-edge);
}
.player-heading {
  display: flex;
  align-items: center;
  gap: 11px;
}
.track-mark {
  display: grid;
  flex: 0 0 54px;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: var(--radius-md);
  background: linear-gradient(
    145deg,
    color-mix(in srgb, var(--accent) 24%, var(--text-secondary)),
    color-mix(in srgb, var(--accent) 30%, var(--text-primary))
  );
  color: var(--surface-primary);
  box-shadow: inset 0 1px 1px var(--functional-glass-edge);
}
.track-copy {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: 4px;
}
.track-copy strong {
  font-size: var(--text-sm);
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.track-copy span {
  font-size: var(--text-2xs);
  color: var(--text-secondary);
}
.player-close {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  margin: -10px -10px -10px 0;
  border-radius: var(--radius);
  color: var(--text-secondary);
}
.player-close:hover {
  background: var(--bg-secondary);
}
.track-progress {
  margin-top: 18px;
}
input[type='range'] {
  --range-fill: var(--accent);
  --range-rail: var(--border-strong);
  display: block;
  appearance: none;
  -webkit-appearance: none;
  width: 100%;
  min-width: 0;
  height: 24px;
  margin: 0;
  border: 0;
  background: transparent;
  cursor: pointer;
}
input[type='range']::-webkit-slider-runnable-track {
  height: 3px;
  border-radius: var(--radius-full);
  background: linear-gradient(
    to right,
    var(--range-fill) var(--range-progress, 0%),
    var(--range-rail) var(--range-progress, 0%)
  );
}
input[type='range']::-webkit-slider-thumb {
  appearance: none;
  -webkit-appearance: none;
  width: 14px;
  height: 14px;
  margin-top: -5.5px;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--range-fill);
}
input[type='range']::-moz-range-track {
  height: 3px;
  border-radius: var(--radius-full);
  background: var(--range-rail);
}
input[type='range']::-moz-range-progress {
  height: 3px;
  background: var(--range-fill);
}
input[type='range']::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border: 0;
  border-radius: var(--radius-full);
  background: var(--range-fill);
}
input:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
.track-times {
  display: flex;
  justify-content: space-between;
  color: var(--text-secondary);
  font-size: var(--text-2xs);
  font-variant-numeric: tabular-nums;
}
.transport-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 26px;
  margin-block: 12px 18px;
}
.transport-note {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  color: var(--text-muted);
}
.player-play {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  border-radius: var(--radius-full);
  background: var(--contrast-bg);
  color: var(--contrast-fg);
  transition: transform var(--transition-fast);
}
.player-play:active {
  transform: scale(0.95);
}
.loop-button {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  border-radius: var(--radius);
  color: var(--text-secondary);
}
.loop-button[aria-pressed='true'] {
  color: var(--accent);
}
.loop-button:hover {
  background: var(--accent-light);
}
.volume-row {
  display: flex;
  align-items: center;
  gap: 9px;
  color: var(--text-secondary);
}
.volume-row input {
  --range-fill: var(--contrast-bg);
}
.volume-hint {
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.player-error {
  margin-top: 14px;
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
  font-size: var(--text-xs);
  line-height: 1.7;
}
.retry-button {
  min-height: 44px;
  color: var(--accent);
  font-size: var(--text-xs);
  font-weight: 600;
}
.music-panel-enter-active,
.music-panel-leave-active {
  transition:
    opacity 180ms ease,
    transform 180ms var(--ease-out);
}
.music-panel-enter-from,
.music-panel-leave-to {
  opacity: 0;
  transform: translateY(-5px);
}
@media (max-width: 1100px) {
  .music-word {
    display: none;
  }
  .music-trigger {
    width: 44px;
    padding: 0;
  }
}
@media (max-width: 720px) {
  .music-panel {
    right: 0;
    width: min(302px, calc(100vw - 20px));
  }
}
@media (prefers-reduced-motion: reduce) {
  .music-bars i {
    animation: none;
  }
  .music-panel-enter-from,
  .music-panel-leave-to {
    transform: none;
  }
}
@media (forced-colors: active) {
  /* System colors keep custom rails/thumbs and the icon-only mobile entry visible. */
  input[type='range'] {
    forced-color-adjust: none;
    --range-fill: Highlight;
    --range-rail: GrayText;
  }
  .music-bars {
    forced-color-adjust: none;
    color: ButtonText;
  }
}
</style>
