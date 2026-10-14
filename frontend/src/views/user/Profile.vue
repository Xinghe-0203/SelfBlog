<template>
  <div class="profile-page">
    <button class="back-btn" @click="goBack" aria-label="返回">
      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
      <span>返回</span>
    </button>

    <div v-if="pageError" class="error-card">
      <div class="error-card-body">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="error-icon"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
        <p>{{ pageError }}</p>
        <button class="btn btn-primary" @click="initLoad">重新加载</button>
      </div>
    </div>

    <template v-else>
      <!-- ===== Hero Section ===== -->
      <section class="hero-section">
        <!-- Cover -->
        <div class="hero-cover">
          <img
            :src="getSafeImageUrl(user?.coverImage, '/default-cover.jpg')"
            alt=""
            class="cover-image"
            loading="eager"
          />
          <div class="cover-overlay"></div>
          <button
            class="cover-edit-btn"
            title="更换封面图"
            @click="triggerCoverUpload"
            :disabled="coverUploading"
            aria-label="更换封面图"
          >
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
              <circle cx="12" cy="13" r="4"/>
            </svg>
            <span class="cover-edit-label">更换封面</span>
          </button>
          <input
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            ref="coverInputRef"
            hidden
            @change="handleCoverUpload"
          />
        </div>

        <!-- Profile Body -->
        <div class="hero-body">
          <!-- Skeleton -->
          <div v-if="loading.user" class="hero-skeleton">
            <div class="skeleton squircle-skeleton"></div>
            <div class="skeleton skeleton-name"></div>
            <div class="skeleton skeleton-bio"></div>
            <div class="skeleton-stats-grid">
              <div class="skeleton skeleton-stat" v-for="n in 4" :key="n"></div>
            </div>
          </div>

          <!-- Loaded -->
          <template v-else>
            <div class="hero-avatar-area">
              <div class="squircle-avatar-wrap">
                <img
                  :src="user?.avatar || '/default-avatar.png'"
                  :alt="user?.nickname"
                  class="squircle-avatar"
                  @error="onAvatarError"
                />
              </div>
              <span class="admin-badge" v-if="user?.role === 'ROLE_ADMIN'">
                <svg width="11" height="11" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
                管理员
              </span>
            </div>

            <div class="hero-info">
              <h1 class="hero-nickname">{{ user?.nickname || user?.username }}</h1>
              <p class="hero-bio">{{ user?.bio || '这个人很懒，什么都没写' }}</p>

              <!-- Bento Stats Grid -->
              <div class="stats-bento">
                <component
                  v-for="stat in statItems"
                  :key="stat.label"
                  :is="stat.to ? 'router-link' : 'div'"
                  :to="stat.to"
                  class="stat-cell"
                >
                  <span class="stat-number">{{ stat.value }}</span>
                  <span class="stat-name">{{ stat.label }}</span>
                </component>
              </div>

              <!-- Actions -->
              <div class="hero-actions">
                <router-link to="/profile-edit" class="btn btn-primary btn-sm">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                  编辑资料
                </router-link>
                <router-link to="/password-change" class="btn btn-ghost btn-sm">修改密码</router-link>
              </div>
            </div>
          </template>
        </div>
      </section>

      <!-- ===== Tabs Section ===== -->
      <section class="tabs-section">
        <nav class="tabs-nav" ref="tabsRef" role="tablist" aria-label="个人内容标签">
          <button
            v-for="(tab, i) in tabs"
            :key="tab.key"
            :ref="(el: any) => { if (el) tabEls[i] = el }"
            class="tab-btn"
            :class="{ active: activeTab === tab.key }"
            :aria-selected="activeTab === tab.key"
            role="tab"
            @click="switchTab(tab.key, i)"
          >
            <span class="tab-icon" v-html="tab.icon"></span>
            <span class="tab-label">{{ tab.label }}</span>
            <span class="tab-count" v-if="tab.count !== undefined">{{ tab.count }}</span>
          </button>
          <div class="tab-indicator" :style="indicatorStyle" aria-hidden="true"></div>
        </nav>

        <div class="tabs-content">
          <!-- Posts Tab -->
          <div v-show="activeTab === 'posts'" class="tab-panel" role="tabpanel" key="posts">
            <div v-if="loading.posts" class="skeleton-list">
              <div v-for="n in 3" :key="n" class="skeleton-card-item">
                <div class="skeleton skeleton-card-title"></div>
                <div class="skeleton skeleton-card-text"></div>
                <div class="skeleton skeleton-card-text short"></div>
                <div class="skeleton skeleton-card-meta"></div>
              </div>
            </div>
            <div v-else-if="postsError" class="tab-error">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <p>{{ postsError }}</p>
              <button class="btn btn-sm btn-primary" @click="fetchPosts">重试</button>
            </div>
            <div v-else-if="posts.length === 0" class="empty-state">
              <div class="empty-icon">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="0.75"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/></svg>
              </div>
              <p class="empty-title">还没有发表文章</p>
              <p class="empty-desc">写下你的第一篇博客，分享你的想法</p>
              <router-link to="/post-edit" class="btn btn-primary btn-sm">开始写作</router-link>
            </div>
            <div v-else class="content-list">
              <article
                v-for="(post, idx) in posts"
                :key="post.id"
                class="post-row"
                :style="{ '--i': idx }"
              >
                <div class="post-row-main">
                  <h3 class="post-row-title">
                    <router-link :to="`/post/${post.id}`">{{ post.title }}</router-link>
                  </h3>
                  <p class="post-row-excerpt">{{ truncateText(post.summary, 120) }}</p>
                </div>
                <div class="post-row-meta">
                  <span class="meta-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
                    {{ formatRelativeTime(post.createTime) }}
                  </span>
                  <span class="meta-pill views-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
                    {{ post.viewCount || 0 }}
                  </span>
                  <span class="meta-pill like-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                    {{ post.likeCount || 0 }}
                  </span>
                  <span class="meta-pill comment-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                    {{ post.commentCount || 0 }}
                  </span>
                </div>
                <div class="post-row-actions">
                  <router-link :to="`/post-edit/${post.id}`" class="icon-btn" title="编辑" aria-label="编辑文章">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg>
                  </router-link>
                  <button class="icon-btn danger" title="删除" aria-label="删除文章" @click="openConfirm('删除文章', `确定删除「${post.title}」吗？删除后不可恢复。`, () => deletePost(post.id))">
                    <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
                  </button>
                </div>
              </article>
              <div v-if="postsTotalPages > 1" class="pagination-bar">
                <div class="pagination">
                  <button class="pagination-btn" :disabled="postsPage <= 1" @click="postsPage--; fetchPosts()" aria-label="上一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
                  </button>
                  <button v-for="p in postsTotalPages" :key="p" class="pagination-btn" :class="{ active: p === postsPage }" @click="postsPage = p; fetchPosts()">{{ p }}</button>
                  <button class="pagination-btn" :disabled="postsPage >= postsTotalPages" @click="postsPage++; fetchPosts()" aria-label="下一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                  </button>
                </div>
                <span class="pagination-info">共 {{ postsTotal }} 篇</span>
              </div>
            </div>
          </div>

          <!-- Circle Tab -->
          <div v-show="activeTab === 'circle'" class="tab-panel" role="tabpanel" key="circle">
            <div v-if="loading.circles" class="skeleton-list">
              <div v-for="n in 3" :key="n" class="skeleton-card-item">
                <div class="skeleton skeleton-card-title"></div>
                <div class="skeleton skeleton-card-text"></div>
                <div class="skeleton skeleton-card-text short"></div>
              </div>
            </div>
            <div v-else-if="circlesError" class="tab-error">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <p>{{ circlesError }}</p>
              <button class="btn btn-sm btn-primary" @click="fetchCircles">重试</button>
            </div>
            <div v-else-if="circles.length === 0" class="empty-state">
              <div class="empty-icon">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="0.75"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </div>
              <p class="empty-title">还没有发布校友圈动态</p>
              <p class="empty-desc">分享你的校园生活点滴</p>
              <router-link to="/circle" class="btn btn-primary btn-sm">去发布</router-link>
            </div>
            <div v-else class="content-list">
              <article
                v-for="(post, idx) in circles"
                :key="post.id"
                class="post-row circle-row"
                :style="{ '--i': idx }"
              >
                <div class="post-row-main">
                  <p class="post-row-excerpt">{{ truncateText(post.content, 140) }}</p>
                </div>
                <div class="post-row-meta">
                  <span class="meta-pill like-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                    {{ post.likeCount || 0 }}
                  </span>
                  <span class="meta-pill comment-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
                    {{ post.commentCount || 0 }}
                  </span>
                  <span class="meta-pill">
                    {{ formatRelativeTime(post.createTime) }}
                  </span>
                </div>
                <router-link :to="`/circle/${post.id}`" class="detail-link">
                  查看
                  <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="9 18 15 12 9 6"/></svg>
                </router-link>
              </article>
              <div v-if="circlesTotalPages > 1" class="pagination-bar">
                <div class="pagination">
                  <button class="pagination-btn" :disabled="circlesPage <= 1" @click="circlesPage--; fetchCircles()" aria-label="上一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
                  </button>
                  <button v-for="p in circlesTotalPages" :key="p" class="pagination-btn" :class="{ active: p === circlesPage }" @click="circlesPage = p; fetchCircles()">{{ p }}</button>
                  <button class="pagination-btn" :disabled="circlesPage >= circlesTotalPages" @click="circlesPage++; fetchCircles()" aria-label="下一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                  </button>
                </div>
                <span class="pagination-info">共 {{ circlesTotal }} 条</span>
              </div>
            </div>
          </div>

          <!-- Likes Tab -->
          <div v-show="activeTab === 'likes'" class="tab-panel" role="tabpanel" key="likes">
            <div v-if="loading.likes" class="skeleton-list">
              <div v-for="n in 3" :key="n" class="skeleton-card-item">
                <div class="skeleton skeleton-card-title"></div>
                <div class="skeleton skeleton-card-text"></div>
                <div class="skeleton skeleton-card-meta"></div>
              </div>
            </div>
            <div v-else-if="likesError" class="tab-error">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <p>{{ likesError }}</p>
              <button class="btn btn-sm btn-primary" @click="fetchLikes">重试</button>
            </div>
            <div v-else-if="likes.length === 0" class="empty-state">
              <div class="empty-icon">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="0.75"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
              </div>
              <p class="empty-title">还没有点赞过文章</p>
              <p class="empty-desc">浏览文章，为你喜欢的内容点赞</p>
              <router-link to="/" class="btn btn-primary btn-sm">去发现</router-link>
            </div>
            <div v-else class="content-list">
              <article
                v-for="(like, idx) in likes"
                :key="like.likeId"
                class="post-row"
                :style="{ '--i': idx }"
              >
                <div class="post-row-main">
                  <h3 class="post-row-title">
                    <router-link :to="`/post/${like.postId}`">{{ like.title }}</router-link>
                  </h3>
                  <p class="post-row-excerpt">{{ truncateText(like.summary, 120) }}</p>
                </div>
                <div class="post-row-meta">
                  <span class="meta-pill like-pill">
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>
                    点赞于 {{ formatRelativeTime(like.likeTime) }}
                  </span>
                </div>
              </article>
              <div v-if="likesTotalPages > 1" class="pagination-bar">
                <div class="pagination">
                  <button class="pagination-btn" :disabled="likesPage <= 1" @click="likesPage--; fetchLikes()" aria-label="上一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
                  </button>
                  <button v-for="p in likesTotalPages" :key="p" class="pagination-btn" :class="{ active: p === likesPage }" @click="likesPage = p; fetchLikes()">{{ p }}</button>
                  <button class="pagination-btn" :disabled="likesPage >= likesTotalPages" @click="likesPage++; fetchLikes()" aria-label="下一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                  </button>
                </div>
                <span class="pagination-info">共 {{ likesTotal }} 条</span>
              </div>
            </div>
          </div>

          <!-- Comments Tab -->
          <div v-show="activeTab === 'comments'" class="tab-panel" role="tabpanel" key="comments">
            <div v-if="loading.comments" class="skeleton-list">
              <div v-for="n in 3" :key="n" class="skeleton-card-item">
                <div class="skeleton skeleton-card-title"></div>
                <div class="skeleton skeleton-card-text"></div>
                <div class="skeleton skeleton-card-meta"></div>
              </div>
            </div>
            <div v-else-if="commentsError" class="tab-error">
              <svg width="36" height="36" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
              <p>{{ commentsError }}</p>
              <button class="btn btn-sm btn-primary" @click="fetchComments">重试</button>
            </div>
            <div v-else-if="comments.length === 0" class="empty-state">
              <div class="empty-icon">
                <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="0.75"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>
              </div>
              <p class="empty-title">还没有发表过评论</p>
              <p class="empty-desc">在文章下方留下你的见解</p>
            </div>
            <div v-else class="content-list">
              <article
                v-for="(comment, idx) in comments"
                :key="comment.id"
                class="comment-row"
                :style="{ '--i': idx }"
              >
                <div class="comment-quote" aria-hidden="true">&ldquo;</div>
                <div class="comment-body">
                  <p class="comment-text">{{ comment.content }}</p>
                  <div class="comment-meta-line">
                    <span>评论于</span>
                    <router-link :to="`/post/${comment.postId}`" class="comment-post-link">{{ comment.postTitle }}</router-link>
                    <span class="comment-sep">&middot;</span>
                    <span class="comment-time">{{ formatRelativeTime(comment.createTime) }}</span>
                  </div>
                </div>
              </article>
              <div v-if="commentsTotalPages > 1" class="pagination-bar">
                <div class="pagination">
                  <button class="pagination-btn" :disabled="commentsPage <= 1" @click="commentsPage--; fetchComments()" aria-label="上一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
                  </button>
                  <button v-for="p in commentsTotalPages" :key="p" class="pagination-btn" :class="{ active: p === commentsPage }" @click="commentsPage = p; fetchComments()">{{ p }}</button>
                  <button class="pagination-btn" :disabled="commentsPage >= commentsTotalPages" @click="commentsPage++; fetchComments()" aria-label="下一页">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
                  </button>
                </div>
                <span class="pagination-info">共 {{ commentsTotal }} 条</span>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <ConfirmDialog />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { userApi } from '../../api/user'
import { postApi } from '../../api/post'
import { likeApi } from '../../api/like'
import { commentApi } from '../../api/comment'
import { followApi } from '../../api/follow'
import { mediaApi } from '../../api/media'
import { circleApi } from '../../api/circle'
import { formatRelativeTime, truncateText, getSafeImageUrl } from '../../utils'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { useConfirm } from '../../composables/useConfirm'

const userStore = useUserStore()
const router = useRouter()
const logger = useLogger('Profile')

function goBack() {
  if (window.history.length > 1) {
    router.back()
  } else {
    router.push('/')
  }
}

const pageError = ref('')
const user = ref<any>(null)
const stats = ref<Record<string, any>>({})

const coverUploading = ref(false)
const coverInputRef = ref<HTMLInputElement | null>(null)

const loading = reactive({ user: false, posts: false, likes: false, comments: false, circles: false })

const posts = ref<any[]>([])
const likes = ref<any[]>([])
const comments = ref<any[]>([])

const postsPage = ref(1)
const postsTotal = ref(0)
const postsTotalPages = ref(1)
const pageSize = 10

const likesPage = ref(1)
const likesTotal = ref(0)
const likesTotalPages = ref(1)

const commentsPage = ref(1)
const commentsTotal = ref(0)
const commentsTotalPages = ref(1)

const postsError = ref('')
const likesError = ref('')
const commentsError = ref('')

const circles = ref<any[]>([])
const circlesPage = ref(1)
const circlesTotal = ref(0)
const circlesTotalPages = ref(1)
const circlesError = ref('')

const activeTab = ref('posts')
const activeTabIndex = ref(0)
const tabEls = ref<any[]>([])

const statItems = computed(() => [
  { label: '文章', value: stats.value.postCount || 0, to: undefined },
  { label: '获赞', value: stats.value.likeCount || 0, to: undefined },
  { label: '粉丝', value: stats.value.followerCount || 0, to: '/followers' },
  { label: '关注', value: stats.value.followingCount || 0, to: '/following' }
])

const tabs = computed(() => [
  { key: 'posts', label: '文章', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14.5 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V7.5L14.5 2z"/><polyline points="14 2 14 8 20 8"/></svg>', count: stats.value.postCount },
  { key: 'circle', label: '校友圈', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>', count: undefined },
  { key: 'likes', label: '点赞', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/></svg>', count: stats.value.likeCount },
  { key: 'comments', label: '评论', icon: '<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/></svg>', count: undefined }
])

const { confirm, ConfirmDialog } = useConfirm()

const indicatorStyle = computed(() => {
  const el = tabEls.value[activeTabIndex.value]
  if (!el) return { width: '0px', opacity: 0 }
  return {
    width: `${el.offsetWidth}px`,
    transform: `translateX(${el.offsetLeft}px)`,
    opacity: 1
  }
})

function onAvatarError(e: Event) {
  (e.target as HTMLImageElement).src = '/default-avatar.png'
}

const triggerCoverUpload = () => {
  coverInputRef.value?.click()
}

const handleCoverUpload = async (e: Event) => {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    const res = await mediaApi.uploadFile(file, 'cover')
    const uploadData = res.data as any
    if (uploadData?.fileUrl) {
      await userApi.updateCoverImage(uploadData.fileUrl)
      if (user.value) user.value.coverImage = uploadData.fileUrl
      toast.success('封面图更新成功')
    }
  } catch (err: any) {
    logger.error('cover upload error', { error: err.message })
    toast.error(err.response?.data?.message || '封面上传失败')
  } finally {
    coverUploading.value = false
    if (coverInputRef.value) coverInputRef.value.value = ''
  }
}

async function openConfirm(title: string, message: string, onConfirm: () => void) {
  const ok = await confirm(message, title)
  if (ok) onConfirm()
}

function switchTab(key: string, index: number) {
  activeTab.value = key
  activeTabIndex.value = index
  if (key === 'posts') fetchPosts()
  else if (key === 'circle') fetchCircles()
  else if (key === 'likes') fetchLikes()
  else if (key === 'comments') fetchComments()
}

async function initLoad() {
  pageError.value = ''
  loading.user = true
  try {
    const [userResponse, statsResponse] = await Promise.all([
      userApi.getUserById(userStore.userId!),
      followApi.getFollowCounts(userStore.userId!)
    ])
    user.value = userResponse.data

    const countsData = (statsResponse.data || {}) as any
    let postCount = 0
    let likeCount = 0
    try {
      const postsResp = await postApi.getMyPosts({ pageNum: 1, pageSize: 1 })
      postCount = (postsResp.data as any)?.total || 0
    } catch { /* ignore */ }
    try {
      const likesResp = await likeApi.getMyLikes({ pageNum: 1, pageSize: 1 })
      likeCount = (likesResp.data as any)?.total || 0
    } catch { /* ignore */ }
    stats.value = {
      postCount,
      likeCount,
      ...countsData
    }
  } catch (error: any) {
    logger.error('Failed to fetch user', { error: error.message })
    pageError.value = '加载用户信息失败，请稍后重试'
  } finally {
    loading.user = false
  }
}

async function fetchPosts() {
  loading.posts = true
  postsError.value = ''
  try {
    const response = await postApi.getMyPosts({ pageNum: postsPage.value, pageSize })
    const data = (response.data || {}) as any
    posts.value = data.records || []
    postsTotal.value = data.total || 0
    postsTotalPages.value = data.pages || 1
  } catch (error: any) {
    logger.error('Failed to fetch posts', { error: error.message })
    postsError.value = error.response?.data?.message || '加载文章列表失败'
  } finally {
    loading.posts = false
  }
}

async function fetchLikes() {
  loading.likes = true
  likesError.value = ''
  try {
    const response = await likeApi.getMyLikes({ pageNum: likesPage.value, pageSize })
    const data = (response.data || {}) as any
    likes.value = data.records || []
    likesTotal.value = data.total || 0
    likesTotalPages.value = data.pages || 1
  } catch (error: any) {
    logger.error('Failed to fetch likes', { error: error.message })
    likesError.value = error.response?.data?.message || '加载点赞列表失败'
  } finally {
    loading.likes = false
  }
}

async function fetchComments() {
  loading.comments = true
  commentsError.value = ''
  try {
    const response = await commentApi.getMyComments({ pageNum: commentsPage.value, pageSize })
    const data = (response.data || {}) as any
    comments.value = data.records || []
    commentsTotal.value = data.total || 0
    commentsTotalPages.value = data.pages || 1
  } catch (error: any) {
    logger.error('Failed to fetch comments', { error: error.message })
    commentsError.value = error.response?.data?.message || '加载评论列表失败'
  } finally {
    loading.comments = false
  }
}

const fetchCircles = async () => {
  if (!user.value?.id) return
  loading.circles = true
  circlesError.value = ''
  try {
    const res = await circleApi.getUserPosts(user.value.id, { pageNum: circlesPage.value, pageSize })
    const data = res.data || {}
    if (Array.isArray(data)) {
      circles.value = data
      circlesTotal.value = data.length
      circlesTotalPages.value = 1
    } else {
      circles.value = data.records || []
      circlesTotal.value = data.total || 0
      circlesTotalPages.value = data.pages || 1
    }
  } catch (err: any) {
    logger.error('fetch circles error', { error: err.message })
    circlesError.value = err.response?.data?.message || '加载失败'
    circles.value = []
  } finally {
    loading.circles = false
  }
}

async function deletePost(postId: number) {
  try {
    await postApi.deletePost(postId)
    posts.value = posts.value.filter(p => p.id !== postId)
    postsTotal.value = Math.max(0, postsTotal.value - 1)
    if (posts.value.length === 0 && postsPage.value > 1) {
      postsPage.value--
      await fetchPosts()
    }
    toast.success('删除成功')
  } catch (error: any) {
    logger.error('Failed to delete post', { error: error.message })
    toast.error(error.response?.data?.message || '删除失败')
  }
}

onMounted(() => {
  initLoad()
  nextTick(() => fetchPosts())
})
</script>

<style scoped>
/* ================================================================
   Profile Page — Bento Grid Minimalist Redesign
   Aesthetic: Clean bento cells, squircle avatar, underline tabs,
   muted pastel accent pills, generous whitespace, subtle texture.
   ================================================================ */

.profile-page {
  grid-column: 1 / -1;
  max-width: var(--container-lg);
  margin: 0 auto;
  padding: 32px 24px 80px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  position: relative;
}

/* Subtle texture overlay on the page */
.profile-page::before {
  content: '';
  position: fixed;
  inset: 0;
  background:
    radial-gradient(circle at 15% 25%, rgba(13, 148, 136, 0.03) 0%, transparent 45%),
    radial-gradient(circle at 85% 75%, rgba(249, 115, 22, 0.02) 0%, transparent 45%);
  pointer-events: none;
  z-index: 0;
}

.profile-page > * {
  position: relative;
  z-index: 1;
}

/* ==================== Back Button ==================== */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--surface-solid);
  border: 1px solid var(--border-solid);
  border-radius: var(--radius);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  font-family: var(--font-sans);
  transition: all var(--duration-normal) var(--ease-default);
  width: fit-content;
  box-shadow: var(--shadow-xs);
}

.back-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
  transform: translateX(-2px);
  box-shadow: var(--shadow-sm);
}

/* ==================== Hero Section ==================== */
.hero-section {
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: var(--surface-solid);
  border: 1px solid var(--border-solid);
  box-shadow: var(--shadow-sm);
  animation: heroReveal 0.5s var(--ease-out) both;
}

@keyframes heroReveal {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* --- Cover --- */
.hero-cover {
  position: relative;
  height: 220px;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.8s var(--ease-smooth);
  display: block;
}

.hero-cover:hover .cover-image {
  transform: scale(1.02);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(12, 10, 9, 0.05) 0%,
    transparent 35%,
    rgba(12, 10, 9, 0.45) 100%
  );
  pointer-events: none;
}

.cover-edit-btn {
  position: absolute;
  bottom: 14px;
  right: 14px;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 12px;
  border-radius: var(--radius);
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.12);
  cursor: pointer;
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  font-family: var(--font-sans);
  opacity: 0;
  transform: translateY(4px);
  transition: all var(--duration-normal) var(--ease-default);
  z-index: 2;
}

.hero-cover:hover .cover-edit-btn {
  opacity: 1;
  transform: translateY(0);
}

.cover-edit-btn:hover {
  background: rgba(0, 0, 0, 0.6);
}

.cover-edit-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.cover-edit-label {
  display: none;
}

@media (min-width: 640px) {
  .cover-edit-label {
    display: inline;
  }
}

/* --- Hero Body --- */
.hero-body {
  padding: 0 32px 32px;
  position: relative;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

/* --- Avatar: Squircle Shape --- */
.hero-avatar-area {
  flex-shrink: 0;
  margin-top: -44px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.squircle-avatar-wrap {
  position: relative;
  padding: 4px;
  border-radius: 22%;
  background: var(--surface-solid);
  box-shadow: var(--shadow-md);
}

.squircle-avatar {
  width: 108px;
  height: 108px;
  border-radius: 20%;
  object-fit: cover;
  display: block;
  border: 3px solid var(--surface-solid);
  transition: transform var(--duration-normal) var(--ease-default);
}

.squircle-avatar:hover {
  transform: scale(1.03);
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  background: var(--primary);
  color: var(--text-inverse);
  font-size: 0.65rem;
  font-weight: var(--font-semibold);
  font-family: var(--font-sans);
  border-radius: var(--radius-full);
  letter-spacing: 0.02em;
}

/* --- Hero Info --- */
.hero-info {
  flex: 1;
  min-width: 0;
  padding-top: 16px;
}

.hero-nickname {
  font-family: var(--font-display);
  font-size: 1.75rem;
  font-weight: var(--font-bold);
  color: var(--text-primary);
  margin-bottom: 6px;
  letter-spacing: -0.025em;
  line-height: var(--leading-tight);
}

.hero-bio {
  font-size: var(--text-base);
  color: var(--text-secondary);
  margin-bottom: 20px;
  line-height: var(--leading-normal);
  max-width: 440px;
}

/* --- Bento Stats Grid --- */
.stats-bento {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  margin-bottom: 20px;
}

.stat-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  transition: all var(--duration-normal) var(--ease-default);
  text-decoration: none;
  color: inherit;
  cursor: default;
}

a.stat-cell,
a.stat-cell[href] {
  cursor: pointer;
}

.stat-cell:hover {
  background: var(--primary-light);
  border-color: var(--primary);
  transform: translateY(-2px);
  box-shadow: var(--shadow-sm);
}

.stat-number {
  display: block;
  font-size: 1.375rem;
  font-weight: var(--font-extrabold);
  color: var(--text-primary);
  line-height: 1;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.stat-name {
  font-size: 0.6875rem;
  color: var(--text-muted);
  margin-top: 4px;
  font-weight: var(--font-medium);
  text-transform: uppercase;
  letter-spacing: 0.06em;
  font-family: var(--font-sans);
}

/* --- Hero Actions --- */
.hero-actions {
  display: flex;
  gap: 10px;
}

/* --- Hero Skeleton --- */
.hero-skeleton {
  padding: 20px 0;
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
}

.squircle-skeleton {
  width: 108px;
  height: 108px;
  border-radius: 22%;
  margin-top: -44px;
}

.skeleton-name {
  width: 160px;
  height: 28px;
}

.skeleton-bio {
  width: 260px;
  height: 16px;
}

.skeleton-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  width: 100%;
  max-width: 420px;
}

.skeleton-stat {
  height: 56px;
  border-radius: var(--radius-md);
}

/* ==================== Tabs Section ==================== */
.tabs-section {
  border-radius: var(--radius-xl);
  background: var(--surface-solid);
  border: 1px solid var(--border-solid);
  box-shadow: var(--shadow-xs);
  overflow: hidden;
  animation: tabsReveal 0.5s var(--ease-out) 0.1s both;
}

@keyframes tabsReveal {
  from {
    opacity: 0;
    transform: translateY(12px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* --- Tab Nav --- */
.tabs-nav {
  display: flex;
  position: relative;
  border-bottom: 1px solid var(--border);
  padding: 0 12px;
  overflow-x: auto;
  scrollbar-width: none;
}

.tabs-nav::-webkit-scrollbar {
  display: none;
}

.tab-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 14px 16px;
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  font-family: var(--font-sans);
  background: none;
  border: none;
  color: var(--text-muted);
  cursor: pointer;
  transition: color var(--duration-normal) var(--ease-default);
  position: relative;
  z-index: 1;
  white-space: nowrap;
  flex-shrink: 0;
}

.tab-btn:hover {
  color: var(--text-secondary);
}

.tab-btn.active {
  color: var(--primary);
}

.tab-icon {
  display: inline-flex;
  color: inherit;
  opacity: 0.7;
  transition: opacity var(--duration-normal) var(--ease-default);
}

.tab-btn.active .tab-icon {
  opacity: 1;
}

.tab-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 16px;
  padding: 0 4px;
  font-size: 0.625rem;
  font-weight: var(--font-semibold);
  background: var(--bg-secondary);
  color: var(--text-muted);
  border-radius: var(--radius-full);
  transition: all var(--duration-normal) var(--ease-default);
}

.tab-btn.active .tab-count {
  background: var(--primary-light);
  color: var(--primary);
}

/* Underline indicator */
.tab-indicator {
  position: absolute;
  bottom: -1px;
  left: 0;
  height: 2px;
  background: var(--primary);
  border-radius: 2px 2px 0 0;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 2;
  opacity: 0;
}

/* --- Tab Content --- */
.tabs-content {
  padding: 24px;
  min-height: 200px;
}

/* ==================== Content List (Posts, Circles) ==================== */
.content-list {
  display: flex;
  flex-direction: column;
}

.post-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 0;
  border-bottom: 1px solid var(--border);
  transition: background var(--duration-fast) var(--ease-default);
  animation: rowFadeIn 0.35s var(--ease-out) calc(var(--i, 0) * 50ms) both;
}

.post-row:last-child {
  border-bottom: none;
}

.post-row:hover {
  background: var(--primary-subtle);
}

@keyframes rowFadeIn {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.post-row-main {
  flex: 1;
  min-width: 0;
}

.post-row-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  font-family: var(--font-sans);
  margin-bottom: 4px;
  line-height: var(--leading-snug);
}

.post-row-title a {
  color: var(--text-primary);
  text-decoration: none;
  transition: color var(--duration-fast) var(--ease-default);
}

.post-row-title a:hover {
  color: var(--primary);
}

.post-row-excerpt {
  font-size: var(--text-sm);
  color: var(--text-secondary);
  line-height: var(--leading-normal);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-row-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.meta-pill {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.6875rem;
  color: var(--text-muted);
  font-family: var(--font-sans);
  font-weight: var(--font-medium);
  padding: 3px 8px;
  border-radius: var(--radius-full);
  background: var(--bg-secondary);
  border: 1px solid var(--border);
  white-space: nowrap;
}

.meta-pill svg {
  opacity: 0.6;
}

/* Pastel accent pills for interaction counts */
.like-pill {
  background: rgba(236, 72, 153, 0.06);
  border-color: rgba(236, 72, 153, 0.12);
  color: #DB2777;
}

.comment-pill {
  background: rgba(59, 130, 246, 0.06);
  border-color: rgba(59, 130, 246, 0.12);
  color: #2563EB;
}

.views-pill {
  background: rgba(13, 148, 136, 0.06);
  border-color: rgba(13, 148, 136, 0.12);
  color: var(--primary);
}

.dark-mode .like-pill {
  background: rgba(244, 114, 182, 0.1);
  border-color: rgba(244, 114, 182, 0.18);
  color: #F472B6;
}

.dark-mode .comment-pill {
  background: rgba(96, 165, 250, 0.1);
  border-color: rgba(96, 165, 250, 0.18);
  color: #60A5FA;
}

.dark-mode .views-pill {
  background: rgba(45, 212, 191, 0.1);
  border-color: rgba(45, 212, 191, 0.18);
  color: #2DD4BF;
}

.post-row-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-default);
}

.post-row:hover .post-row-actions {
  opacity: 1;
}

.icon-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  text-decoration: none;
  background: transparent;
  color: var(--text-muted);
}

.icon-btn:hover {
  background: var(--primary-light);
  color: var(--primary);
}

.icon-btn.danger:hover {
  background: var(--error-light);
  color: var(--error);
}

.detail-link {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-weight: var(--font-medium);
  font-size: var(--text-sm);
  color: var(--primary);
  text-decoration: none;
  transition: gap var(--duration-fast) var(--ease-default);
  flex-shrink: 0;
  font-family: var(--font-sans);
}

.detail-link:hover {
  gap: 6px;
  color: var(--primary-hover);
}

/* ==================== Comment Rows ==================== */
.comment-row {
  display: flex;
  gap: 12px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border);
  animation: rowFadeIn 0.35s var(--ease-out) calc(var(--i, 0) * 50ms) both;
}

.comment-row:last-child {
  border-bottom: none;
}

.comment-quote {
  flex-shrink: 0;
  font-size: 2.2rem;
  font-weight: 800;
  color: var(--primary);
  opacity: 0.12;
  line-height: 1;
  font-family: Georgia, serif;
  user-select: none;
  margin-top: -4px;
}

.comment-body {
  flex: 1;
  min-width: 0;
}

.comment-text {
  font-size: var(--text-sm);
  color: var(--text-primary);
  line-height: var(--leading-normal);
  margin-bottom: 8px;
}

.comment-meta-line {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: var(--text-xs);
  color: var(--text-muted);
  font-family: var(--font-sans);
  flex-wrap: wrap;
}

.comment-post-link {
  font-weight: var(--font-medium);
  color: var(--primary);
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.comment-sep {
  opacity: 0.4;
}

.comment-time {
  color: var(--text-muted);
}

/* ==================== Empty State ==================== */
.empty-state {
  padding: 56px 24px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.empty-icon {
  margin-bottom: 20px;
  color: var(--text-muted);
  opacity: 0.18;
  animation: gentleFloat 4s ease-in-out infinite;
}

@keyframes gentleFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

.empty-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  font-family: var(--font-display);
  color: var(--text-primary);
  margin-bottom: 6px;
}

.empty-desc {
  font-size: var(--text-sm);
  color: var(--text-muted);
  margin-bottom: 20px;
  max-width: 280px;
}

/* ==================== Error States ==================== */
.tab-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 48px 20px;
  color: var(--text-muted);
}

.tab-error p {
  font-size: var(--text-sm);
}

.error-card {
  text-align: center;
  border-radius: var(--radius-xl);
  background: var(--surface-solid);
  border: 1px solid var(--border-solid);
  box-shadow: var(--shadow-sm);
}

.error-card-body {
  padding: 56px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.error-icon {
  color: var(--text-muted);
  opacity: 0.4;
}

.error-card-body p {
  color: var(--text-secondary);
  font-size: var(--text-sm);
}

/* ==================== Skeleton ==================== */
.skeleton-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.skeleton-card-item {
  padding: 18px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--bg-secondary);
}

.skeleton-card-title {
  width: 55%;
  height: 18px;
  margin-bottom: 10px;
}

.skeleton-card-text {
  width: 85%;
  height: 14px;
  margin-bottom: 6px;
}

.skeleton-card-text.short {
  width: 45%;
}

.skeleton-card-meta {
  width: 180px;
  height: 14px;
  margin-top: 10px;
}

/* ==================== Pagination ==================== */
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding-top: 20px;
  margin-top: 8px;
}

.pagination-info {
  font-size: var(--text-xs);
  color: var(--text-muted);
}

/* ==================== Responsive ==================== */
@media (max-width: 768px) {
  .profile-page {
    padding: 16px 16px 64px;
    gap: 16px;
  }

  .hero-cover {
    height: 150px;
  }

  .hero-body {
    flex-direction: column;
    align-items: center;
    text-align: center;
    padding: 0 20px 24px;
    gap: 12px;
  }

  .hero-avatar-area {
    margin-top: -40px;
  }

  .squircle-avatar {
    width: 92px;
    height: 92px;
  }

  .hero-info {
    padding-top: 4px;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .hero-nickname {
    font-size: 1.375rem;
  }

  .hero-bio {
    text-align: center;
    max-width: 320px;
  }

  .stats-bento {
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
    width: 100%;
  }

  .stat-cell {
    padding: 10px 4px;
  }

  .stat-number {
    font-size: 1.125rem;
  }

  .hero-actions {
    justify-content: center;
  }

  .tabs-content {
    padding: 16px;
  }

  /* Stack post rows on mobile */
  .post-row {
    flex-wrap: wrap;
    gap: 10px;
  }

  .post-row-meta {
    order: 3;
    width: 100%;
    justify-content: flex-start;
    padding-top: 8px;
    border-top: 1px solid var(--border);
  }

  .post-row-actions {
    opacity: 1;
    order: 2;
  }

  .tab-btn {
    padding: 12px 12px;
    font-size: var(--text-xs);
  }

  .tab-label {
    display: none;
  }

  .tab-btn .tab-icon + .tab-label {
    display: none;
  }

  .comment-row {
    gap: 8px;
  }

  .comment-quote {
    font-size: 1.6rem;
  }
}

@media (max-width: 480px) {
  .hero-nickname {
    font-size: 1.25rem;
  }

  .stats-bento {
    gap: 6px;
  }

  .stat-cell {
    padding: 8px 4px;
  }

  .stat-number {
    font-size: 1rem;
  }

  .stat-name {
    font-size: 0.6rem;
  }

  .meta-pill {
    font-size: 0.625rem;
    padding: 2px 6px;
  }
}
</style>
