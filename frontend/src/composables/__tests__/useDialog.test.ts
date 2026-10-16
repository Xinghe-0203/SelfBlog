import { afterEach, describe, expect, it } from 'vitest'
import { defineComponent, nextTick, ref } from 'vue'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import { useDialog, overlayTarget } from '../useDialog'

enableAutoUnmount(afterEach)
const Dialog = defineComponent({
  props: { initial: { type: Boolean, default: true } },
  setup(props) {
    const open = ref(props.initial)
    const panel = ref<HTMLElement | null>(null)
    const dialog = useDialog(open, panel, () => { open.value = false })
    return { open, panel, ...dialog }
  },
  template: '<div v-if="open" ref="panel" tabindex="-1" role="dialog"><button class="first">First</button><button class="last">Last</button></div>'
})

describe('shared dialog lifecycle', () => {
  it('locks scroll immediately and restores the original inline overflow', async () => {
    document.body.style.overflow = 'clip'
    const wrapper = mount(Dialog, { attachTo: document.body })
    expect(document.body.style.overflow).toBe('hidden')
    wrapper.vm.open = false
    await nextTick()
    expect(document.body.style.overflow).toBe('clip')
    document.body.style.overflow = ''
  })

  it('does not release another dialog lock when a closed instance unmounts', () => {
    const active = mount(Dialog, { attachTo: document.body })
    const closed = mount(Dialog, { props: { initial: false } })
    closed.unmount()
    expect(document.body.style.overflow).toBe('hidden')
    active.unmount()
    expect(document.body.style.overflow).toBe('')
  })

  it('closes only the topmost nested dialog with Escape', async () => {
    const first = mount(Dialog, { attachTo: document.body })
    await nextTick()
    const second = mount(Dialog, { attachTo: document.body })
    await nextTick()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', bubbles: true }))
    await nextTick()
    expect(second.vm.open).toBe(false)
    expect(first.vm.open).toBe(true)
    expect(document.body.style.overflow).toBe('hidden')
    first.unmount()
    expect(document.body.style.overflow).toBe('')
  })

  it('wraps Tab, contains programmatic focus and restores the opener', async () => {
    const opener = document.createElement('button')
    document.body.append(opener)
    opener.focus()
    const wrapper = mount(Dialog, { attachTo: document.body })
    await nextTick()
    const first = wrapper.get<HTMLButtonElement>('.first').element
    const last = wrapper.get<HTMLButtonElement>('.last').element
    expect(document.activeElement).toBe(first)
    last.focus()
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true }))
    expect(document.activeElement).toBe(first)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true }))
    expect(document.activeElement).toBe(last)
    opener.focus()
    expect(document.activeElement).toBe(first)
    wrapper.unmount()
    await nextTick()
    expect(document.activeElement).toBe(opener)
    opener.remove()
  })

  it('restores focus into an underlying dialog before restoring the page', async () => {
    const first = mount(Dialog, { attachTo: document.body })
    await nextTick()
    const inside = first.get<HTMLButtonElement>('.last').element
    inside.focus()
    const second = mount(Dialog, { attachTo: document.body })
    await nextTick()
    second.unmount()
    await nextTick()
    expect(document.activeElement).toBe(inside)
  })

  it('uses the stable theme-aware host when present', () => {
    const host = document.createElement('div')
    host.id = 'overlay-root'
    document.body.append(host)
    expect(overlayTarget()).toBe(host)
    host.remove()
    expect(overlayTarget()).toBe(document.body)
  })

  it('assigns visual layer order by opening order, not component declaration order', async () => {
    const earlier = mount(Dialog, { props: { initial: false }, attachTo: document.body })
    const later = mount(Dialog, { props: { initial: false }, attachTo: document.body })
    later.vm.open = true
    await nextTick()
    earlier.vm.open = true
    await nextTick()
    expect(earlier.vm.layer).toBeGreaterThan(later.vm.layer)
  })
})
