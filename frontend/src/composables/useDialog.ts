import { nextTick, onBeforeUnmount, ref, watch, type Ref } from 'vue'

interface DialogEntry {
  element: Readonly<Ref<HTMLElement | null>>
  close: () => void
  previous: HTMLElement | null
  layer: Ref<number>
  priority: number
}

// One stack for all modal consumers, not one counter per component instance.
const stack: DialogEntry[] = []
let previousOverflow = ''
const top = () => stack[stack.length - 1]
function updateLayers(): void {
  stack.sort((left, right) => left.priority - right.priority)
  stack.forEach((entry, index) => {
    entry.layer.value = index * 2
  })
}

function focusable(element: HTMLElement): HTMLElement[] {
  return Array.from(
    element.querySelectorAll<HTMLElement>('a[href], button, input, textarea, select, [tabindex]')
  ).filter(
    (item) =>
      item.tabIndex >= 0 &&
      !item.matches(':disabled, input[type="hidden"]') &&
      !item.closest('[hidden], [inert]') &&
      getComputedStyle(item).display !== 'none' &&
      getComputedStyle(item).visibility !== 'hidden'
  )
}

function focusFirst(entry: DialogEntry): void {
  const element = entry.element.value
  if (!element) return
  ;(focusable(element)[0] || element).focus({ preventScroll: true })
}

function onKeydown(event: KeyboardEvent): void {
  const entry = top()
  if (!entry) return
  if (event.key === 'Escape') {
    event.preventDefault()
    event.stopImmediatePropagation()
    entry.close()
    return
  }
  if (event.key !== 'Tab' || !entry.element.value) return
  const candidates = focusable(entry.element.value)
  const first = candidates[0]
  const last = candidates[candidates.length - 1]
  if (!first || !last) {
    event.preventDefault()
    focusFirst(entry)
    return
  }
  if (
    event.shiftKey &&
    (document.activeElement === first || !entry.element.value.contains(document.activeElement))
  ) {
    event.preventDefault()
    last.focus()
  } else if (
    !event.shiftKey &&
    (document.activeElement === last || !entry.element.value.contains(document.activeElement))
  ) {
    event.preventDefault()
    first.focus()
  }
}

function onFocus(event: FocusEvent): void {
  const entry = top()
  if (
    entry?.element.value &&
    event.target instanceof Node &&
    !entry.element.value.contains(event.target)
  ) {
    focusFirst(entry)
  }
}

export function overlayTarget(): HTMLElement {
  return document.getElementById('overlay-root') || document.body
}

export function useDialog(
  open: Readonly<Ref<boolean>>,
  element: Readonly<Ref<HTMLElement | null>>,
  close: () => void,
  priority = 0
): { isTop: () => boolean; layer: Ref<number> } {
  let entry: DialogEntry | null = null
  const layer = ref(0)

  function release(): void {
    if (!entry) return
    const current = entry
    const wasTop = top() === current
    const index = stack.indexOf(current)
    if (index >= 0) stack.splice(index, 1)
    updateLayers()
    entry = null
    if (!stack.length) {
      document.body.style.overflow = previousOverflow
      document.removeEventListener('keydown', onKeydown, true)
      document.removeEventListener('focusin', onFocus)
    }
    if (wasTop)
      void nextTick(() => {
        // A replacement dialog may have opened while the old one was leaving.
        const remaining = top()
        if (
          current.previous?.isConnected &&
          (!remaining || remaining.element.value?.contains(current.previous))
        ) {
          current.previous.focus({ preventScroll: true })
        } else if (remaining) focusFirst(remaining)
      })
  }

  watch(
    open,
    (value) => {
      if (!value) {
        release()
        return
      }
      if (entry) return
      entry = {
        element,
        close,
        layer,
        priority,
        previous: document.activeElement instanceof HTMLElement ? document.activeElement : null
      }
      if (!stack.length) {
        previousOverflow = document.body.style.overflow
        document.body.style.overflow = 'hidden'
        document.addEventListener('keydown', onKeydown, true)
        document.addEventListener('focusin', onFocus)
      }
      stack.push(entry)
      updateLayers()
      document.dispatchEvent(new Event('selfblog:dialog-open'))
      const current = entry
      void nextTick(() => {
        if (top() === current) focusFirst(current)
      })
    },
    { immediate: true, flush: 'sync' }
  )
  // Internal v-if toggles can queue the sync open watcher before Vue's render.
  // Acquire focus again when the actual teleported panel ref is populated.
  watch(
    element,
    (value) => {
      if (value && entry && top() === entry) focusFirst(entry)
    },
    { flush: 'post' }
  )
  onBeforeUnmount(release)
  return { isTop: () => entry !== null && top() === entry, layer }
}
