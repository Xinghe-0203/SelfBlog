<template>
  <div class="comments-page">
    <header class="page-heading">
      <div>
        <h1>评论管理</h1>
        <p>审核访客留言。评论展示开关位于网站设置。</p>
      </div>
      <select v-model="statusFilter" aria-label="按状态筛选" @change="load">
        <option value="">全部状态</option>
        <option value="1">已展示</option>
        <option value="0">已隐藏</option>
        <option value="2">垃圾评论</option>
      </select>
    </header>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <section class="comment-list" aria-live="polite">
      <article v-for="comment in comments" :key="comment.id" class="comment-card">
        <div class="comment-top">
          <div>
            <strong>{{ comment.nickname }}</strong
            ><span
              >评论于
              <router-link :to="`/post/${comment.postId}`" target="_blank">{{
                comment.postTitle || `文章 #${comment.postId}`
              }}</router-link></span
            >
          </div>
          <time>{{ formatDate(comment.createTime) }}</time>
        </div>
        <p class="comment-content">{{ comment.content }}</p>
        <div class="comment-footer">
          <span class="status" :class="`status-${comment.status}`">{{
            statusName(comment.status)
          }}</span>
          <div class="actions">
            <button v-if="comment.status !== 1" type="button" @click="changeStatus(comment, 1)">
              展示</button
            ><button v-if="comment.status !== 0" type="button" @click="changeStatus(comment, 0)">
              隐藏</button
            ><button v-if="comment.status !== 2" type="button" @click="changeStatus(comment, 2)">
              标记垃圾</button
            ><button class="danger" type="button" @click="remove(comment)">
              <Icon name="trash" :size="14" />删除
            </button>
          </div>
        </div>
      </article>
      <p v-if="!loading && !comments.length" class="empty">没有找到评论。</p>
      <p v-if="loading" class="empty">正在加载评论…</p>
    </section>
    <div class="pagination">
      <button type="button" :disabled="page <= 1" @click="changePage(page - 1)">
        <Icon name="chevron-left" :size="15" />上一页</button
      ><span>{{ page }} / {{ pages }}</span
      ><button type="button" :disabled="page >= pages" @click="changePage(page + 1)">
        下一页<Icon name="chevron-right" :size="15" />
      </button>
    </div>
    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { publicInteractionApi, type GuestComment } from '@/api/public-interactions'
import { formatDate } from '@/utils'
import { toast } from '@/utils/toast'
import { useConfirm } from '@/composables/useConfirm'
import Icon from '@/components/common/Icon.vue'

const comments = ref<GuestComment[]>([])
const loading = ref(true)
const error = ref('')
const statusFilter = ref('')
const page = ref(1)
const pages = ref(1)
const { confirm, ConfirmDialog } = useConfirm()

async function load() {
  loading.value = true
  error.value = ''
  try {
    const result = await publicInteractionApi.getAdminComments({
      pageNum: page.value,
      pageSize: 20,
      status: statusFilter.value === '' ? undefined : Number(statusFilter.value)
    })
    comments.value = result.data?.records || []
    pages.value = Math.max(1, result.data?.pages || 1)
  } catch {
    error.value = '评论列表暂时无法加载。'
  } finally {
    loading.value = false
  }
}

function statusName(status: number) {
  return status === 1 ? '展示中' : status === 2 ? '垃圾评论' : '已隐藏'
}
function changePage(next: number) {
  if (next < 1 || next > pages.value) return
  page.value = next
  void load()
}
async function changeStatus(comment: GuestComment, status: number) {
  try {
    await publicInteractionApi.updateCommentStatus(comment.id, status)
    comment.status = status
    toast.success('评论状态已更新')
  } catch {
    toast.error('更新失败，请重试')
  }
}
async function remove(comment: GuestComment) {
  if (!(await confirm(`确定删除来自「${comment.nickname}」的这条评论吗？`, '删除评论'))) return
  try {
    await publicInteractionApi.deleteComment(comment.id)
    comments.value = comments.value.filter((item) => item.id !== comment.id)
    toast.success('评论已删除')
    // 本地过滤不会更新服务端页数：删空当前页时退回上一页并重新拉取。
    if (comments.value.length === 0 && page.value > 1) {
      page.value -= 1
    }
    await load()
  } catch {
    toast.error('删除失败，请重试')
  }
}

onMounted(() => void load())
</script>

<style scoped>
.comments-page {
  display: grid;
  gap: 16px;
  color: var(--text-primary);
}
.page-heading {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}
.page-heading h1 {
  margin: 0 0 8px;
  font-size: var(--text-2xl);
}
.page-heading p:not(.eyebrow) {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-sm);
}
select {
  min-height: 44px;
  padding-inline: 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font-size: var(--text-sm);
}
.comment-list {
  display: grid;
  gap: 12px;
}
.comment-card {
  padding: 18px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-lg);
  background: var(--surface-primary);
}
.comment-top,
.comment-footer,
.comment-top > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.comment-top > div {
  justify-content: flex-start;
  flex-wrap: wrap;
}
.comment-top span,
.comment-top time {
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.comment-top a {
  color: var(--accent);
}
.comment-content {
  margin: 15px 0;
  font-size: var(--text-sm);
  line-height: 1.8;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.comment-footer {
  padding-top: 12px;
  border-top: 1px solid var(--border-light);
}
.status {
  padding: 3px 8px;
  border-radius: var(--radius-xs);
  font-size: var(--text-2xs);
}
.status-1 {
  background: var(--success-light);
  color: var(--success);
}
.status-0 {
  background: var(--bg-secondary);
  color: var(--text-secondary);
}
.status-2 {
  background: var(--error-light);
  color: var(--error);
}
.actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: end;
  gap: 4px;
}
.actions button,
.pagination button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 36px;
  padding-inline: 10px;
  border-radius: var(--radius-sm);
  color: var(--text-secondary);
  font-size: var(--text-xs);
}
.actions button:hover {
  background: var(--bg-secondary);
  color: var(--accent);
}
.actions .danger:hover {
  color: var(--error);
}
.empty,
.error {
  padding: 30px;
  color: var(--text-muted);
  text-align: center;
  font-size: var(--text-sm);
}
.error {
  color: var(--error);
}
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  color: var(--text-muted);
  font-size: var(--text-xs);
}
.pagination button {
  min-height: 44px;
}
.pagination button:disabled {
  opacity: 0.45;
}
@media (max-width: 580px) {
  .page-heading {
    align-items: start;
    flex-direction: column;
  }
  .comment-top,
  .comment-footer {
    align-items: start;
    flex-direction: column;
  }
  .actions {
    justify-content: start;
  }
}
</style>
