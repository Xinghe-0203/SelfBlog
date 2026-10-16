import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate, useRoute, useRouter } from 'vue-router'
import { pageApi } from '@/api/pages'
import { usePagesStore } from '@/stores/pages'
import { useSiteStore } from '@/stores/site'
import { useConfirm } from '@/composables/useConfirm'
import type { PageConfig, PageEditorDocument } from '@/types/page'

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value)) as T
export function usePageEditor() {
  const route = useRoute()
  const router = useRouter()
  const site = useSiteStore()
  const pages = usePagesStore()
  const { confirm, ConfirmDialog } = useConfirm()
  const editor = ref<PageEditorDocument | null>(null)
  const config = ref<PageConfig>({})
  const slug = ref<string | null>(null)
  const loading = ref(true)
  const busy = ref(false)
  const error = ref('')
  const success = ref('')
  const saved = ref('')
  let epoch = 0
  let confirming = false
  let deleted = false
  const snapshot = () => JSON.stringify({ slug: slug.value, config: config.value })
  const dirty = computed(() => !!editor.value && snapshot() !== saved.value)
  const hasUnpublishedChanges = computed(
    () =>
      !!editor.value &&
      (!editor.value.published ||
        JSON.stringify(editor.value.draft.config) !== JSON.stringify(editor.value.published.config))
  )
  const errorText = (reason: unknown) =>
    (reason as { userMessage?: string }).userMessage ||
    '操作失败，请检查网络后重试；当前编辑内容已保留。'

  async function load() {
    const request = ++epoch
    loading.value = true
    busy.value = false
    error.value = ''
    success.value = ''
    editor.value = null
    config.value = {}
    slug.value = null
    saved.value = ''
    try {
      const response = await pageApi.getEditor(String(route.params.id))
      if (request !== epoch) return
      editor.value = response.data
      config.value = clone(response.data.draft.config)
      slug.value = response.data.draft.slug || null
      saved.value = snapshot()
    } catch (reason) {
      if (request === epoch) error.value = errorText(reason)
    } finally {
      if (request === epoch) loading.value = false
    }
  }
  function canLeave() {
    return deleted || !dirty.value || window.confirm('有未保存的页面修改，确定要离开吗？')
  }
  onBeforeRouteLeave(canLeave)
  onBeforeRouteUpdate((to, from) => to.params.id === from.params.id || canLeave())
  function beforeUnload(event: BeforeUnloadEvent) {
    if (dirty.value) {
      event.preventDefault()
      event.returnValue = ''
    }
  }
  window.addEventListener('beforeunload', beforeUnload)
  onBeforeUnmount(() => {
    epoch++
    window.removeEventListener('beforeunload', beforeUnload)
  })
  watch(() => route.params.id, load, { immediate: true })

  async function reload() {
    if (canLeave()) await load()
  }
  async function write(action: 'save' | 'publish' | 'unpublish' | 'delete') {
    const current = editor.value
    if (!current || busy.value || confirming) return
    if (action === 'publish' && dirty.value) {
      error.value = '请先保存草稿，再发布已保存的版本。'
      return
    }
    if (action === 'unpublish' || action === 'delete') {
      confirming = true
      const accepted = await confirm(
        action === 'delete'
          ? '确定删除这个内容页及其草稿吗？此操作不能撤销。'
          : '确定下线这个内容页吗？读者将无法再访问它。'
      )
      confirming = false
      if (!accepted || current !== editor.value) return
    }
    const request = epoch
    const sent = snapshot()
    const payload = clone(config.value)
    const sentSlug = slug.value
    busy.value = true
    error.value = ''
    success.value = ''
    try {
      const revision = current.draft.revision
      if (action === 'delete') {
        await pageApi.delete(current.id, revision)
        if (request !== epoch) return
        deleted = true
        void pages.loadManifest()
        await router.push(`${site.adminRoot}/pages`)
        return
      }
      const response =
        action === 'save'
          ? await pageApi.saveDraft(current.id, revision, payload, sentSlug)
          : action === 'publish'
            ? await pageApi.publish(current.id, revision)
            : await pageApi.unpublish(current.id, revision)
      if (request !== epoch) return
      editor.value = response.data
      // Never replace the live form with an older response: preserve edits made in flight.
      if (action === 'save') saved.value = sent
      else void pages.loadManifest()
      success.value =
        action === 'save'
          ? '草稿已保存，线上内容未改变。'
          : action === 'publish'
            ? '页面已发布。'
            : '页面已下线，草稿仍然保留。'
      if (dirty.value) success.value += ' 当前还有新改动未保存。'
    } catch (reason) {
      if (request === epoch) error.value = errorText(reason)
    } finally {
      if (request === epoch) busy.value = false
    }
  }
  function reset() {
    if (!busy.value && editor.value?.builtin) {
      config.value = {}
      success.value = '已在当前草稿恢复默认，请保存后再发布。'
    }
  }
  return {
    editor,
    config,
    slug,
    loading,
    busy,
    error,
    success,
    dirty,
    hasUnpublishedChanges,
    reload,
    write,
    reset,
    ConfirmDialog
  }
}
