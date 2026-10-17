import { afterAll, afterEach, beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { nextTick } from 'vue'
import MusicPlayer from '../MusicPlayer.vue'
import { useSiteStore } from '@/stores/site'
import { useMusicStore } from '@/stores/music'

enableAutoUnmount(afterEach)
beforeAll(() => {
  vi.spyOn(HTMLMediaElement.prototype, 'play').mockResolvedValue(undefined)
  vi.spyOn(HTMLMediaElement.prototype, 'pause').mockImplementation(() => {})
  vi.spyOn(HTMLMediaElement.prototype, 'load').mockImplementation(() => {})
})
beforeEach(() => {
  vi.clearAllMocks()
  localStorage.clear()
})
afterAll(() => vi.restoreAllMocks())

async function setup() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', component: { template: '<div />' } },
      { path: '/archives', component: { template: '<div />' } },
      { path: '/writer', component: { template: '<div />' }, meta: { adminArea: true } }
    ]
  })
  await router.push('/')
  useSiteStore().applySettings({
    musicEnabled: 'true',
    musicUrl: '/uploads/song.mp3',
    musicTitle: 'Song'
  })
  const wrapper = mount(MusicPlayer, { global: { plugins: [pinia, router] } })
  await nextTick()
  return { wrapper, router, music: useMusicStore(), site: useSiteStore() }
}

describe('headless music host', () => {
  it('does not autoplay and renders no separate floating music UI', async () => {
    const { wrapper } = await setup()
    expect(HTMLMediaElement.prototype.play).not.toHaveBeenCalled()
    expect(wrapper.find('button').exists()).toBe(false)
    expect(wrapper.find('audio').attributes('preload')).toBe('none')
  })

  it('retains the same audio across public routes and pauses on admin entry', async () => {
    const { wrapper, router, music } = await setup()
    const audio = wrapper.get('audio').element
    await music.play()
    await router.push('/archives')
    expect(wrapper.get('audio').element).toBe(audio)
    vi.mocked(HTMLMediaElement.prototype.pause).mockClear()
    await router.push('/writer')
    expect(HTMLMediaElement.prototype.pause).toHaveBeenCalled()
    await router.push('/')
    expect(HTMLMediaElement.prototype.play).toHaveBeenCalledTimes(1)
  })

  it('replaces the audio only when its configured source changes', async () => {
    const { wrapper, site } = await setup()
    const old = wrapper.get('audio').element
    site.applySettings({ musicUrl: '/uploads/new.mp3' })
    await nextTick()
    expect(wrapper.get('audio').element).not.toBe(old)
    expect(wrapper.get('audio').attributes('src')).toBe('/uploads/new.mp3')
    expect(HTMLMediaElement.prototype.play).not.toHaveBeenCalled()
  })

  it('removes disabled audio and resets the shared state', async () => {
    const { wrapper, site, music } = await setup()
    site.applySettings({ musicEnabled: 'false' })
    await nextTick()
    expect(wrapper.find('audio').exists()).toBe(false)
    expect(music.available).toBe(false)
  })
})
