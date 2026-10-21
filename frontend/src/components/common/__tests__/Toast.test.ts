import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, flushPromises, mount } from '@vue/test-utils'
import Toast from '../Toast.vue'
import { addToast, removeToast, toasts } from '@/utils/toast'

enableAutoUnmount(afterEach)
beforeEach(() => {
  toasts.value.splice(0, toasts.value.length)
})
afterEach(() => vi.unstubAllGlobals())
describe('notification host', () => {
  it('inherits the app theme through the stable overlay host', () => {
    const host = document.createElement('div')
    host.id = 'overlay-root'
    document.body.append(host)
    const wrapper = mount(Toast)
    try {
      expect(host.querySelector('.toast-container')).not.toBeNull()
    } finally {
      wrapper.unmount()
      host.remove()
    }
  })

  it('re-arms the progress loop for a toast pushed after the list went empty', async () => {
    const frames: FrameRequestCallback[] = []
    let rafCalls = 0
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      rafCalls++
      frames.push(cb)
      return rafCalls
    })
    vi.stubGlobal('cancelAnimationFrame', vi.fn())

    // 桩掉 TransitionGroup/Transition：Vue 的过渡系统自己也用 rAF，会把帧计数搅浑。
    const wrapper = mount(Toast, {
      global: { stubs: { teleport: true, transition: true, 'transition-group': true } }
    })
    try {
      await flushPromises()
      const firstId = addToast('第一条', 'success', 0)
      await flushPromises()
      const drain = () => {
        const pending = frames.splice(0, frames.length)
        pending.forEach((cb) => cb(0))
      }
      drain()

      // 列表清空：队列里那一帧执行后不应再排队，且必须释放帧 id。
      removeToast(firstId)
      await flushPromises()
      drain()
      expect(frames.length).toBe(0)

      const beforeSecond = rafCalls
      addToast('第二条', 'success', 0)
      await flushPromises()
      // 旧实现里 animFrame 仍是非空旧 id，watch 的 `!animFrame` 看门条件恒假，这里不会再有新帧。
      expect(rafCalls).toBeGreaterThan(beforeSecond)
      expect(frames.length).toBeGreaterThan(0)
    } finally {
      wrapper.unmount()
    }
  })
})
