import { ref, computed, type Ref } from 'vue'

export function usePagination<T>(
  fetchFn: (params: { pageNum: number; pageSize: number }) => Promise<{ data: { records: T[]; total: number } }>,
  defaultPageSize = 10
) {
  const pageNum = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)
  const list = ref<T[]>([]) as Ref<T[]>
  const loading = ref(false)

  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
  const hasNext = computed(() => pageNum.value < totalPages.value)
  const hasPrev = computed(() => pageNum.value > 1)

  const fetchList = async () => {
    loading.value = true
    try {
      const res = await fetchFn({ pageNum: pageNum.value, pageSize: pageSize.value })
      list.value = res.data?.records || []
      total.value = res.data?.total || 0
    } catch {
      list.value = []
      total.value = 0
    } finally {
      loading.value = false
    }
  }

  const nextPage = () => { if (hasNext.value) { pageNum.value++; fetchList() } }
  const prevPage = () => { if (hasPrev.value) { pageNum.value--; fetchList() } }
  const goToPage = (page: number) => { if (page >= 1 && page <= totalPages.value) { pageNum.value = page; fetchList() } }
  const reset = () => { pageNum.value = 1; fetchList() }

  return { pageNum, pageSize, total, totalPages, list, loading, hasNext, hasPrev, fetchList, nextPage, prevPage, goToPage, reset }
}
