/**
 * 确认对话框组合式函数
 */

import { ref, onBeforeUnmount, defineComponent, h } from 'vue'
import Modal from '@/components/common/Modal.vue'

export function useConfirm() {
  const isOpen = ref(false)
  const msg = ref('')
  const dialogTitle = ref('')
  let resolver: ((value: boolean) => void) | null = null

  onBeforeUnmount(() => {
    if (resolver) {
      resolver(false)
      resolver = null
    }
  })

  const confirm = (message: string, title = '确认操作'): Promise<boolean> => {
    if (resolver) {
      resolver(false)
    }
    msg.value = message
    dialogTitle.value = title
    isOpen.value = true
    return new Promise<boolean>((resolve) => {
      resolver = resolve
    })
  }

  const ConfirmDialog = defineComponent({
    setup() {
      const handleConfirm = () => {
        isOpen.value = false
        const r = resolver
        resolver = null
        r?.(true)
      }
      const handleCancel = () => {
        isOpen.value = false
        const r = resolver
        resolver = null
        r?.(false)
      }
      return () =>
          h(
          Modal,
          {
            show: isOpen.value,
            title: dialogTitle.value,
            width: '440px',
            onClose: handleCancel
          },
          {
            default: () =>
              h(
                'p',
                {
                  style: 'font-size: 0.9rem; color: var(--text-secondary); line-height: 1.6;'
                },
                msg.value
              ),
            footer: () => [
              h(
                'button',
                {
                  class: 'btn btn-secondary',
                  onClick: handleCancel
                },
                '取消'
              ),
              h(
                'button',
                {
                  class: 'btn btn-primary',
                  style: 'margin-left: 8px;',
                  onClick: handleConfirm
                },
                '确定'
              )
            ]
          }
        )
    }
  })

  return { confirm, ConfirmDialog }
}
