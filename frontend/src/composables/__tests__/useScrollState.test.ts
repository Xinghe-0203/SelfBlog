import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { defineComponent, nextTick } from 'vue'
import { mount, enableAutoUnmount } from '@vue/test-utils'
import { useScrollState } from '../useScrollState'

/** 用文本渲染状态，避免断言 wrapper.vm 上的动态属性带来的类型体操。 */
const Harness = defineComponent({
  setup() {
    const { scrolled, direction } = useScrollState(16)
    return { scrolled, direction }
  },
  template: '<div data-test="state">{{ scrolled }}|{{ direction }}</div>'
})

function mountHarness() {
  return mount(Harness)
}

/**
 * 每个用例结束必须卸载：window 上的 scroll 监听是全局的，
 * 不卸载就会在用例之间叠加，把"每帧只调度一次"的断言搅成 N 次。
 */
enableAutoUnmount(afterEach)

/**
 * 受控的 rAF 队列。真实浏览器里 rAF 是异步的 —— 不在下一帧跑回调就不会执行，
 * 正因如此同一帧内的多次 scroll 才会被合并。用同步 rAF 替身测不出这个语义，
 * 所以这里把回调攒进队列，由用例显式 flush 一帧。
 */
let frameQueue: FrameRequestCallback[] = []
let rafCalls = 0

function flushFrame(): void {
  const pending = frameQueue
  frameQueue = []
  for (const cb of pending) cb(0)
}

/** jsdom 的 scrollY 默认不可写；写入后派发一次 scroll，再跑完一帧。 */
async function scrollTo(y: number): Promise<void> {
  Object.defineProperty(window, 'scrollY', {
    writable: true,
    configurable: true,
    value: y
  })
  window.dispatchEvent(new Event('scroll'))
  flushFrame()
  await nextTick()
}

describe('useScrollState', () => {
  beforeEach(() => {
    frameQueue = []
    rafCalls = 0
    vi.stubGlobal('requestAnimationFrame', (cb: FrameRequestCallback) => {
      rafCalls++
      frameQueue.push(cb)
      return rafCalls
    })
    Object.defineProperty(window, 'scrollY', {
      writable: true,
      configurable: true,
      value: 0
    })
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('starts unscrolled at the top of the page', () => {
    expect(mountHarness().get('[data-test="state"]').text()).toBe('false|up')
  })

  it('stays unscrolled while below the threshold', async () => {
    const wrapper = mountHarness()
    await scrollTo(16)
    expect(wrapper.get('[data-test="state"]').text()).toBe('false|up')
  })

  it('flags scrolled once past the threshold', async () => {
    const wrapper = mountHarness()
    await scrollTo(17)
    expect(wrapper.get('[data-test="state"]').text()).toBe('true|down')
  })

  it('reports upward movement after scrolling down', async () => {
    const wrapper = mountHarness()
    await scrollTo(400)
    await scrollTo(200)
    expect(wrapper.get('[data-test="state"]').text()).toBe('true|up')
  })

  it('drops back to unscrolled when returning to the top', async () => {
    const wrapper = mountHarness()
    await scrollTo(400)
    await scrollTo(0)
    expect(wrapper.get('[data-test="state"]').text()).toBe('false|up')
  })

  it('coalesces bursts of scroll events into a single frame', async () => {
    mountHarness()
    rafCalls = 0

    for (let i = 0; i < 20; i++) window.dispatchEvent(new Event('scroll'))

    // 20 次事件只换来一次帧调度 —— 这才是"每帧最多处理一次"。
    expect(rafCalls).toBe(1)
  })

  it('stops listening after unmount', async () => {
    const wrapper = mountHarness()
    await scrollTo(400)
    const state = wrapper.get('[data-test="state"]').element as HTMLElement
    expect(state.textContent).toBe('true|down')

    wrapper.unmount()
    await scrollTo(600)

    // 卸载后监听已摘除，状态被冻结在上一次的值。
    expect(state.textContent).toBe('true|down')
  })

  it('cancels the frame queued before unmount instead of updating after teardown', async () => {
    const wrapper = mountHarness()
    // 替身按真实语义移除队列里的回调：取消后这一帧不会再被执行。
    const cancel = vi.fn((id: number) => {
      frameQueue = frameQueue.filter((_, index) => index + 1 !== id)
    })
    vi.stubGlobal('cancelAnimationFrame', cancel)

    Object.defineProperty(window, 'scrollY', { writable: true, configurable: true, value: 400 })
    window.dispatchEvent(new Event('scroll'))
    expect(frameQueue).toHaveLength(1)

    wrapper.unmount()

    // 排队中的那一帧（id 为 1）被显式取消，而不是等它自己跑完。
    expect(cancel).toHaveBeenCalledWith(1)
    expect(frameQueue).toHaveLength(0)

    // 就算再 flush 也没有待执行的帧：组件销毁后不会再读 scrollY、写 ref。
    flushFrame()
    await nextTick()
    expect(rafCalls).toBe(1)
  })
})
