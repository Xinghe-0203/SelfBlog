import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import NavbarMusicControl from '../NavbarMusicControl.vue'
import { useMusicStore } from '@/stores/music'

enableAutoUnmount(afterEach)
beforeEach(() => {
  localStorage.clear()
  setActivePinia(createPinia())
})

function setup(open = true) {
  const music = useMusicStore()
  music.configure({ enabled: true, url: '/uploads/song.mp3', title: 'A real song' })
  const wrapper = mount(NavbarMusicControl, { props: { open } })
  return { wrapper, music }
}

describe('navigation music control', () => {
  it('keeps the approved idle sound bars and listening label without implying playback', () => {
    const { wrapper, music } = setup(false)
    expect(wrapper.get('.music-trigger').text()).toBe('听一会儿')
    expect(wrapper.findAll('.music-bars i')).toHaveLength(3)
    expect(music.playing).toBe(false)
    expect(wrapper.get('.music-trigger').classes()).not.toContain('is-playing')
  })
  it('does not appear without configured music', () => {
    expect(
      mount(NavbarMusicControl, { props: { open: false } })
        .find('button')
        .exists()
    ).toBe(false)
  })

  it('requests a panel without playing or creating another audio', async () => {
    const { wrapper, music } = setup(false)
    const play = vi.spyOn(music, 'play')
    await wrapper.get('.music-trigger').trigger('click')
    expect(wrapper.emitted('toggle')).toHaveLength(1)
    expect(play).not.toHaveBeenCalled()
    expect(wrapper.find('audio').exists()).toBe(false)
  })

  it('shows real metadata, disables unknown seeking and invokes transport on click', async () => {
    const { wrapper, music } = setup()
    const play = vi.spyOn(music, 'play').mockResolvedValue(undefined)
    expect(wrapper.text()).toContain('A real song')
    expect(wrapper.get<HTMLInputElement>('[aria-label="播放进度"]').element.disabled).toBe(true)
    expect(wrapper.text()).toContain('--:--')
    await wrapper.get('.player-play').trigger('click')
    expect(play).toHaveBeenCalledTimes(1)
    music.duration = 180
    music.currentTime = 27
    await wrapper.vm.$nextTick()
    expect(wrapper.get<HTMLInputElement>('[aria-label="播放进度"]').element.disabled).toBe(false)
    expect(wrapper.text()).toContain('0:27')
    expect(wrapper.text()).toContain('3:00')
  })

  it('offers a device volume hint when browser volume control is unsupported', async () => {
    const { wrapper, music } = setup()
    music.volumeSupported = false
    await wrapper.vm.$nextTick()
    expect(wrapper.find('[aria-label="音量"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('设备音量键')
  })

  it('supports loop preference, inline retry and an explicit close action', async () => {
    const { wrapper, music } = setup()
    await wrapper.get('.loop-button').trigger('click')
    expect(music.loop).toBe(false)
    music.error = '音乐暂时无法播放'
    await wrapper.vm.$nextTick()
    const play = vi.spyOn(music, 'play').mockResolvedValue(undefined)
    expect(wrapper.get('[role="status"]').text()).toContain('音乐暂时无法播放')
    await wrapper.get('.retry-button').trigger('click')
    expect(play).toHaveBeenCalled()
    await wrapper.get('[aria-label="关闭音乐面板"]').trigger('click')
    expect(wrapper.emitted('close')).toHaveLength(1)
  })
})
