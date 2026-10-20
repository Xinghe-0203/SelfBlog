<template>
  <div class="post-edit-page">
    <button class="back-btn glass" @click="router.back()">
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
      >
        <polyline points="15 18 9 12 15 6" />
      </svg>
      返回
    </button>

    <div v-if="isLoading" class="loading-state glass">
      <div class="loading-spinner"></div>
      <p>加载中...</p>
    </div>

    <div v-else class="edit-container">
      <!-- 编辑器头部 -->
      <div class="editor-header glass">
        <div class="header-top">
          <input
            v-model="form.title"
            type="text"
            class="title-input"
            placeholder="请输入文章标题..."
            maxlength="200"
          />
          <div class="header-actions">
            <span class="save-status" :class="saveStatus">
              <svg
                v-if="saveStatus === 'saving'"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                class="spin"
              >
                <path d="M21 12a9 9 0 1 1-6.219-8.56" />
              </svg>
              <svg
                v-else-if="saveStatus === 'saved'"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <polyline points="20 6 9 17 4 12" />
              </svg>
              {{ saveStatusText }}
            </span>
            <button
              class="btn btn-ghost btn-sm"
              @click="saveDraft()"
              :disabled="saving || publishing || (!form.title && !form.content)"
            >
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z" />
                <polyline points="17 21 17 13 7 13 7 21" />
                <polyline points="7 3 7 8 15 8" />
              </svg>
              保存草稿
            </button>
            <button class="btn btn-secondary btn-sm" @click="showPreview = !showPreview">
              <svg
                v-if="!showPreview"
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
              <svg
                v-else
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
              {{ showPreview ? '编辑' : '预览' }}
            </button>
            <button
              class="btn btn-primary btn-sm"
              @click="publishPost"
              :disabled="publishing || saving || !form.title.trim() || !form.content.trim()"
            >
              <svg
                width="14"
                height="14"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
              >
                <line x1="22" y1="2" x2="11" y2="13" />
                <polygon points="22 2 15 22 11 13 2 9 22 2" />
              </svg>
              {{ publishing ? '发布中...' : '发布' }}
            </button>
          </div>
        </div>
        <div class="header-meta">
          <span class="meta-item">
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M4 7V4h16v3" />
              <path d="M9 20h6" />
              <path d="M12 4v16" />
            </svg>
            字数：{{ wordCount }}
          </span>
          <span class="meta-sep">·</span>
          <span class="meta-item">
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <circle cx="12" cy="12" r="10" />
              <polyline points="12 6 12 12 16 14" />
            </svg>
            预计阅读：{{ readingTime }}分钟
          </span>
          <span v-if="form.category" class="meta-item meta-category">
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"
              />
            </svg>
            分类：{{ categoryLabel(form.category) }}
          </span>
          <span v-if="route.params.id && postInfo.createTime" class="meta-item meta-time">
            <svg
              width="12"
              height="12"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="4" width="18" height="18" rx="2" ry="2" />
              <line x1="16" y1="2" x2="16" y2="6" />
              <line x1="8" y1="2" x2="8" y2="6" />
              <line x1="3" y1="10" x2="21" y2="10" />
            </svg>
            创建于 {{ formatDate(postInfo.createTime) }}
          </span>
        </div>
      </div>

      <!-- 编辑器主体 -->
      <div class="editor-main-wrapper">
        <div class="editor-body glass">
          <!-- 工具栏 -->
          <div class="toolbar" v-if="!showPreview">
            <div class="toolbar-group">
              <button
                class="tool-btn"
                title="撤销 (Ctrl+Z)"
                @click="undoAction"
                :disabled="!canUndo"
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="9 14 4 9 9 4" />
                  <path d="M20 20v-7a4 4 0 0 0-4-4H4" />
                </svg>
              </button>
              <button
                class="tool-btn"
                title="重做 (Ctrl+Y)"
                @click="redoAction"
                :disabled="!canRedo"
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="15 14 20 9 15 4" />
                  <path d="M4 20v-7a4 4 0 0 0 4-4h12" />
                </svg>
              </button>
            </div>
            <span class="toolbar-divider"></span>
            <div class="toolbar-group">
              <select
                class="font-size-select"
                title="字号"
                @change="insertHeading(($event.target as HTMLSelectElement).value)"
                :value="currentHeading"
              >
                <option value="">正文</option>
                <option value="1">标题1</option>
                <option value="2">标题2</option>
                <option value="3">标题3</option>
                <option value="4">标题4</option>
              </select>
            </div>
            <span class="toolbar-divider"></span>
            <div class="toolbar-group">
              <button class="tool-btn" title="粗体 (Ctrl+B)" @click="insertMarkdown('**', '**')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2.5"
                >
                  <path d="M6 4h8a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z" />
                  <path d="M6 12h9a4 4 0 0 1 4 4 4 4 0 0 1-4 4H6z" />
                </svg>
              </button>
              <button class="tool-btn" title="斜体 (Ctrl+I)" @click="insertMarkdown('*', '*')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="19" y1="4" x2="10" y2="4" />
                  <line x1="14" y1="20" x2="5" y2="20" />
                  <line x1="15" y1="4" x2="9" y2="20" />
                </svg>
              </button>
              <button class="tool-btn" title="删除线" @click="insertMarkdown('~~', '~~')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    d="M16 4H9a3 3 0 0 0-3 3v0a3 3 0 0 0 3 3h6a3 3 0 0 1 3 3v0a3 3 0 0 1-3 3H8"
                  />
                  <line x1="4" y1="12" x2="20" y2="12" />
                </svg>
              </button>
              <button class="tool-btn" title="高亮" @click="insertMarkdown('<mark>', '</mark>')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M12 20h9" />
                  <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z" />
                </svg>
              </button>
            </div>
            <span class="toolbar-divider"></span>
            <div class="toolbar-group">
              <button class="tool-btn" title="行内代码" @click="insertCode">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <polyline points="16 18 22 12 16 6" />
                  <polyline points="8 6 2 12 8 18" />
                </svg>
              </button>
              <button class="tool-btn" title="代码块" @click="insertCodeBlock">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <polyline points="14 8 18 12 14 16" />
                  <polyline points="10 16 6 12 10 8" />
                </svg>
              </button>
            </div>
            <span class="toolbar-divider"></span>
            <div class="toolbar-group">
              <button class="tool-btn" title="引用" @click="insertMarkdown('> ', '')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    d="M3 21c3 0 7-1 7-8V5c0-1.25-.756-2.017-2-2H4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2 1 0 1 0 1 1v1c0 1-1 2-2 2s-1 .008-1 1.031V21z"
                  />
                  <path
                    d="M15 21c3 0 7-1 7-8V5c0-1.25-.757-2.017-2-2h-4c-1.25 0-2 .75-2 1.972V11c0 1.25.75 2 2 2h.75c0 2.25.25 4-2.75 4v3z"
                  />
                </svg>
              </button>
            </div>
            <span class="toolbar-divider"></span>
            <div class="toolbar-group">
              <button class="tool-btn" title="无序列表" @click="insertMarkdown('- ', '')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="9" y1="6" x2="20" y2="6" />
                  <line x1="9" y1="12" x2="20" y2="12" />
                  <line x1="9" y1="18" x2="20" y2="18" />
                  <circle cx="5" cy="6" r="1" fill="currentColor" />
                  <circle cx="5" cy="12" r="1" fill="currentColor" />
                  <circle cx="5" cy="18" r="1" fill="currentColor" />
                </svg>
              </button>
              <button class="tool-btn" title="有序列表" @click="insertMarkdown('1. ', '')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="10" y1="6" x2="21" y2="6" />
                  <line x1="10" y1="12" x2="21" y2="12" />
                  <line x1="10" y1="18" x2="21" y2="18" />
                  <text x="3" y="8" font-size="7" fill="currentColor" stroke="none">1</text>
                  <text x="3" y="14" font-size="7" fill="currentColor" stroke="none">2</text>
                  <text x="3" y="20" font-size="7" fill="currentColor" stroke="none">3</text>
                </svg>
              </button>
              <button class="tool-btn" title="任务列表" @click="insertMarkdown('- [ ] ', '')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <rect x="3" y="5" width="6" height="6" rx="1" />
                  <path d="M12 7h9" />
                  <rect x="3" y="13" width="6" height="6" rx="1" />
                  <path d="M12 15h9" />
                </svg>
              </button>
            </div>
            <span class="toolbar-divider"></span>
            <div class="toolbar-group">
              <button class="tool-btn" title="链接" @click="insertLink">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71" />
                  <path d="M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71" />
                </svg>
              </button>
              <button class="tool-btn" title="上传图片" @click="imageInput?.click()">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <polyline points="21 15 16 10 5 21" />
                </svg>
              </button>
              <button
                class="tool-btn"
                title="表格"
                @click="
                  insertMarkdown(
                    '\n| 列1 | 列2 | 列3 |\n| --- | --- | --- |\n| 内容 | 内容 | 内容 |\n',
                    ''
                  )
                "
              >
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <line x1="3" y1="9" x2="21" y2="9" />
                  <line x1="3" y1="15" x2="21" y2="15" />
                  <line x1="9" y1="3" x2="9" y2="21" />
                  <line x1="15" y1="3" x2="15" y2="21" />
                </svg>
              </button>
            </div>
            <span class="toolbar-divider"></span>
            <div class="toolbar-group">
              <button class="tool-btn" title="分割线" @click="insertMarkdown('\n---\n', '')">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="2" y1="12" x2="22" y2="12" />
                </svg>
              </button>
              <button class="tool-btn" title="清除格式" @click="clearFormat">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path d="M17 14l2-2m0 0l2-2m0 0l-2-2m0 0l-2-2" />
                  <path d="M7 10l-2 2m0 0l-2 2m0 0l2 2m0 0l2 2" />
                  <path d="M12 2v20" />
                </svg>
              </button>
            </div>
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              ref="imageInput"
              hidden
              @change="uploadContentImage"
            />
            <div v-if="uploadingImage" class="upload-progress">
              <div class="upload-progress-bar" :style="{ width: uploadProgress + '%' }"></div>
              <span class="upload-progress-text">上传中... {{ uploadProgress }}%</span>
            </div>
          </div>

          <!-- 编辑器/预览区 -->
          <div class="editor-content">
            <textarea
              v-if="!showPreview"
              v-model="form.content"
              class="content-input"
              placeholder="请输入文章内容（支持Markdown语法）..."
              @keydown.tab.prevent="insertTab"
              @keydown.ctrl.z.prevent="undoAction"
              @keydown.ctrl.y.prevent="redoAction"
              @keydown.ctrl.b.prevent="insertMarkdown('**', '**')"
              @keydown.ctrl.i.prevent="insertMarkdown('*', '*')"
              @keydown.ctrl.u.prevent="insertMarkdown('<u>', '</u>')"
              @keydown.shift.space.prevent="insertMarkdown('<mark>', '</mark>')"
              ref="contentTextarea"
              maxlength="300000"
            ></textarea>
            <div v-else class="editor-preview">
              <div v-if="form.content" class="markdown-body" v-html="renderedContent"></div>
              <div v-else class="empty-preview">
                <svg
                  width="48"
                  height="48"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="1.5"
                >
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                  <polyline points="14 2 14 8 20 8" />
                </svg>
                <span>暂无内容可预览</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 侧边栏 -->
      <div class="editor-sidebar">
        <!-- 文章信息 -->
        <div class="sidebar-section glass">
          <h3 class="sidebar-title">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
              <polyline points="14 2 14 8 20 8" />
            </svg>
            文章信息
          </h3>
          <div class="form-group">
            <label class="form-label">摘要</label>
            <textarea
              v-model="form.summary"
              class="summary-input form-input"
              placeholder="文章摘要（选填，用于列表展示）"
              maxlength="500"
              rows="3"
            ></textarea>
            <span class="char-hint">{{ form.summary.length }}/500</span>
          </div>
        </div>

        <!-- 封面图 -->
        <div class="sidebar-section glass">
          <h3 class="sidebar-title">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
              <circle cx="8.5" cy="8.5" r="1.5" />
              <polyline points="21 15 16 10 5 21" />
            </svg>
            封面图
          </h3>
          <div class="cover-upload">
            <input
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              @change="handleCoverUpload"
              ref="coverInput"
              hidden
            />
            <div v-if="form.coverImage" class="cover-preview">
              <img :src="form.coverImage" alt="封面图" />
              <button class="remove-cover" @click="removeCover">
                <svg
                  width="14"
                  height="14"
                  viewBox="0 0 24 24"
                  fill="none"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              </button>
            </div>
            <button v-else class="upload-btn" @click="coverInput?.click()">
              <svg
                width="32"
                height="32"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.5"
              >
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                <circle cx="8.5" cy="8.5" r="1.5" />
                <polyline points="21 15 16 10 5 21" />
              </svg>
              <span>点击上传封面图</span>
              <span class="upload-hint">支持 JPG、PNG、GIF、WebP</span>
            </button>
          </div>
        </div>

        <!-- 分类 -->
        <div class="sidebar-section glass">
          <h3 class="sidebar-title">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M22 19a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h5l2 3h9a2 2 0 0 1 2 2z"
              />
            </svg>
            分类
          </h3>
          <select v-model="form.category" class="form-input">
            <option value="">请选择分类</option>
            <option value="tech">技术</option>
            <option value="life">生活</option>
            <option value="study">学习</option>
            <option value="other">其他</option>
          </select>
        </div>

        <!-- 标签 -->
        <div class="sidebar-section glass">
          <h3 class="sidebar-title">
            <svg
              width="14"
              height="14"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2"
            >
              <path
                d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"
              />
              <line x1="7" y1="7" x2="7.01" y2="7" />
            </svg>
            标签
          </h3>
          <div class="tag-input-container">
            <div class="selected-tags">
              <span v-for="tag in selectedTags" :key="tag.id || tag.name" class="tag-badge">
                {{ tag.name }}
                <button class="remove-tag" @click="removeTag(tag)">
                  <svg
                    width="10"
                    height="10"
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="3"
                  >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                  </svg>
                </button>
              </span>
            </div>
            <input
              v-model="tagInput"
              type="text"
              class="tag-input"
              placeholder="输入标签搜索..."
              @keyup.enter="addTag"
              @input="filterTags"
              maxlength="20"
            />
          </div>
          <div v-if="filteredTags.length" class="suggested-tags">
            <button
              v-for="tag in filteredTags"
              :key="tag.id"
              class="suggested-tag"
              @click="addSuggestedTag(tag)"
            >
              {{ tag.name }}
            </button>
          </div>
          <div
            v-if="tagInput.trim() && !allTags.find((t) => t.name === tagInput.trim())"
            class="no-tags"
          >
            <button class="btn btn-text btn-xs" @click="createTagAndAdd">
              创建「{{ tagInput }}」标签
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <ConfirmDialog />
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { postApi } from '../../api/post'
import { tagApi } from '../../api/tag'
import { mediaApi } from '../../api/media'
import { useLogger } from '../../utils/logger'
import { toast } from '../../utils/toast'
import { useConfirm } from '../../composables/useConfirm'
import { formatDate } from '../../utils'

const route = useRoute()
const router = useRouter()
const logger = useLogger('PostEdit')
const { confirm, ConfirmDialog } = useConfirm()

const form = reactive({ title: '', content: '', summary: '', category: '', coverImage: '' })
const postInfo = reactive({ createTime: '', viewCount: 0, likeCount: 0 })
const showPreview = ref(false)
const saving = ref(false)
const publishing = ref(false)
const confirmingPublication = ref(false)
const tagInput = ref('')
const selectedTags = ref<any[]>([])
const allTags = ref<any[]>([])
const filteredTags = ref<any[]>([])
const currentDraftId = ref<number | null>(null)
const contentTextarea = ref<HTMLTextAreaElement | null>(null)
const imageInput = ref<HTMLInputElement | null>(null)
const coverInput = ref<HTMLInputElement | null>(null)
const saveStatus = ref('')
const uploadingImage = ref(false)
const uploadProgress = ref(0)
const isLoading = ref(false)
function postData() {
  return {
    ...form,
    tagIds: selectedTags.value.filter((tag) => tag.id).map((tag) => tag.id),
    tagNames: selectedTags.value.map((tag) => tag.name)
  }
}
// A completed request only saves its submitted snapshot, not subsequent edits.
const savedSnapshot = ref(JSON.stringify(postData()))
const dirty = computed(() => JSON.stringify(postData()) !== savedSnapshot.value)
const publishedPostId = ref<number>()
const editingPostId = computed(() => {
  const id = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
  return id || publishedPostId.value
})
let saveStatusTimer: ReturnType<typeof setTimeout> | undefined

function acceptSavedSnapshot(snapshot: string) {
  savedSnapshot.value = snapshot
  saveStatus.value = dirty.value ? 'changed' : 'saved'
  clearTimeout(saveStatusTimer)
  saveStatusTimer = setTimeout(() => {
    saveStatus.value = ''
  }, 3000)
}

const renderedContent = computed(() => {
  if (!form.content) return ''
  try {
    return DOMPurify.sanitize(marked.parse(form.content) as string, {
      FORBID_TAGS: ['style', 'script', 'iframe', 'form', 'input', 'button', 'textarea', 'select'],
      FORBID_ATTR: ['onerror', 'onload', 'onclick', 'onmouseover']
    })
  } catch {
    return DOMPurify.sanitize(form.content)
  }
})

const wordCount = computed(() => {
  const text = form.content || ''
  return text.replace(/\s/g, '').length
})

const readingTime = computed(() => {
  const text = form.content || ''
  const wc = text.replace(/\s/g, '').length
  return Math.max(1, Math.ceil(wc / 500))
})

const saveStatusText = computed(() => {
  switch (saveStatus.value) {
    case 'saving':
      return '保存中...'
    case 'saved':
      return '已保存'
    case 'changed':
      return '有新改动未保存'
    default:
      return ''
  }
})

function categoryLabel(category: string) {
  const labels: Record<string, string> = {
    tech: '技术',
    life: '生活',
    study: '学习',
    other: '其他'
  }
  return labels[category] || category
}

function insertMarkdown(before: string, after: string) {
  const ta = contentTextarea.value
  if (!ta) return
  const start = ta.selectionStart,
    end = ta.selectionEnd
  const text = form.content
  const selected = text.substring(start, end)
  saveHistory()
  form.content = text.substring(0, start) + before + selected + after + text.substring(end)
  nextTick(() => {
    ta.focus()
    ta.setSelectionRange(start + before.length, start + before.length + selected.length)
  })
}

function insertCode() {
  insertMarkdown('`', '`')
}

function insertCodeBlock() {
  insertMarkdown('```\n', '\n```')
}

function insertLink() {
  const ta = contentTextarea.value
  if (!ta) return
  const start = ta.selectionStart,
    end = ta.selectionEnd
  const selected = form.content.substring(start, end) || '链接文字'
  const linkText = `[${selected}](url)`
  saveHistory()
  form.content = form.content.substring(0, start) + linkText + form.content.substring(end)
}

function insertTab(e: KeyboardEvent) {
  const ta = e.target as HTMLTextAreaElement
  const start = ta.selectionStart,
    end = ta.selectionEnd
  saveHistory()
  form.content = form.content.substring(0, start) + '  ' + form.content.substring(end)
  nextTick(() => ta.setSelectionRange(start + 2, start + 2))
}

// Undo/Redo
const history = ref([''])
const historyIndex = ref(0)
const currentHeading = ref('')

const canUndo = computed(() => historyIndex.value > 0)
const canRedo = computed(() => historyIndex.value < history.value.length - 1)

function saveHistory() {
  if (history.value[historyIndex.value] === form.content) return
  if (historyIndex.value < history.value.length - 1) {
    history.value = history.value.slice(0, historyIndex.value + 1)
  }
  history.value.push(form.content)
  historyIndex.value = history.value.length - 1
  if (history.value.length > 50) {
    history.value.shift()
    historyIndex.value--
  }
}

let historyTimer: ReturnType<typeof setTimeout> | undefined = undefined
watch(
  () => form.content,
  () => {
    clearTimeout(historyTimer)
    historyTimer = setTimeout(() => saveHistory(), 2000)
  }
)

function undoAction() {
  if (canUndo.value) {
    historyIndex.value--
    form.content = history.value[historyIndex.value] ?? ''
  }
}

function redoAction() {
  if (canRedo.value) {
    historyIndex.value++
    form.content = history.value[historyIndex.value] ?? ''
  }
}

function insertHeading(level: string) {
  if (!level) return
  const prefix = '#'.repeat(parseInt(level)) + ' '
  const ta = contentTextarea.value
  if (!ta) return
  const start = ta.selectionStart
  const lineStart = form.content.lastIndexOf('\n', start - 1) + 1
  saveHistory()
  form.content = form.content.substring(0, lineStart) + prefix + form.content.substring(lineStart)
  currentHeading.value = ''
}

function clearFormat() {
  const ta = contentTextarea.value
  if (!ta) return
  const start = ta.selectionStart,
    end = ta.selectionEnd
  if (start === end) return
  const selected = form.content.substring(start, end)
  let cleaned = selected
    .replace(/\*\*(.+?)\*\*/g, '$1')
    .replace(/\*(.+?)\*/g, '$1')
    .replace(/~~(.+?)~~/g, '$1')
    .replace(/`(.+?)`/g, '$1')
    .replace(/<u>(.+?)<\/u>/g, '$1')
    .replace(/<mark>(.+?)<\/mark>/g, '$1')
  saveHistory()
  form.content = form.content.substring(0, start) + cleaned + form.content.substring(end)
}

async function uploadContentImage(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  uploadingImage.value = true
  try {
    const res = await mediaApi.uploadFile(file, 'article')
    const url = res.data?.url || res.data?.fileName
    if (url) {
      const ta = contentTextarea.value
      const start = ta?.selectionStart ?? form.content.length
      const md = `\n![图片](${url})\n`
      saveHistory()
      form.content =
        form.content.substring(0, start) + md + form.content.substring(ta?.selectionEnd ?? start)
    }
    toast.success('图片已插入')
  } catch (err: any) {
    logger.error('upload content image error', { error: err.message })
    toast.error(err.response?.data?.message || '图片上传失败')
  } finally {
    uploadingImage.value = false
    target.value = ''
  }
}

// ========== 标签 ==========

const fetchTags = async () => {
  try {
    const response = await tagApi.getTagList()
    allTags.value = response.data || []
  } catch (err: any) {
    logger.error('Failed to fetch tags', { error: err.message })
  }
}

function filterTags() {
  const input = tagInput.value.trim().toLowerCase()
  if (!input) {
    filteredTags.value = []
    return
  }
  filteredTags.value = allTags.value
    .filter(
      (t) => t.name.toLowerCase().includes(input) && !selectedTags.value.find((s) => s.id === t.id)
    )
    .slice(0, 8)
}

function addTag() {
  const name = tagInput.value.trim()
  if (!name) return
  const existing = allTags.value.find((t) => t.name === name)
  if (existing) {
    if (!selectedTags.value.find((s) => s.id === existing.id)) selectedTags.value.push(existing)
  } else {
    if (!selectedTags.value.find((s) => s.name === name)) selectedTags.value.push({ name })
  }
  tagInput.value = ''
  filteredTags.value = []
}

function addSuggestedTag(tag: any) {
  if (!selectedTags.value.find((s) => s.id === tag.id)) selectedTags.value.push(tag)
  tagInput.value = ''
  filteredTags.value = []
}

function removeTag(tag: any) {
  selectedTags.value = selectedTags.value.filter((s) =>
    tag.id ? s.id !== tag.id : s.name !== tag.name
  )
}

async function createTagAndAdd() {
  const name = tagInput.value.trim()
  if (!name || selectedTags.value.find((s) => s.name === name)) return
  try {
    const res = await tagApi.createTag({ name })
    const newTag = res.data
    if (newTag && newTag.id) {
      selectedTags.value.push(newTag)
    }
    await fetchTags()
    tagInput.value = ''
    toast.success('标签已创建')
  } catch (err: any) {
    logger.error('create tag error', { error: err.message })
    toast.error(err.response?.data?.message || '创建标签失败')
  }
}

// ========== 封面图 ==========

async function handleCoverUpload(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  try {
    const res = await mediaApi.uploadFile(file, 'cover')
    form.coverImage = (res.data as any)?.fileUrl || (res.data as any)?.url || ''
    toast.success('封面图上传成功')
  } catch (err: any) {
    logger.error('cover upload error', { error: err.message })
    toast.error(err.response?.data?.message || '上传封面图失败')
  }
  if (coverInput.value) coverInput.value.value = ''
}

function removeCover() {
  form.coverImage = ''
}

// ========== 文章数据 ==========

const fetchPost = async () => {
  const postId = Array.isArray(route.params.id) ? route.params.id[0] : route.params.id
  if (!postId) return
  try {
    const res = await postApi.getPostById(postId)
    const post = res.data
    form.title = post.title || ''
    form.content = post.content || ''
    form.summary = post.summary || ''
    form.category = post.category || ''
    form.coverImage = post.coverImage || ''
    selectedTags.value = post.tags || []
    postInfo.createTime = post.createTime || ''
    postInfo.viewCount = post.viewCount || 0
    postInfo.likeCount = post.likeCount || 0
  } catch (err: any) {
    logger.error('fetch post error', { error: err.message })
    toast.error(err.response?.data?.message || '加载文章失败')
  }
}

const fetchDraftById = async (draftId: string) => {
  try {
    const res = await postApi.getDraft(draftId)
    if (res.data) {
      currentDraftId.value = Number(draftId)
      publishedPostId.value = res.data.postId
      form.title = res.data.title || ''
      form.content = res.data.content || ''
      form.summary = res.data.summary || ''
      form.category = res.data.category || ''
      selectedTags.value = res.data.tags || []
      form.coverImage = res.data.coverImage || ''
    }
  } catch (err: any) {
    logger.error('fetch draft by id error', { error: err.message })
    toast.error(err.response?.data?.message || '加载草稿失败')
  }
}

// ========== 保存与发布 ==========

const saveDraft = async (silent = false) => {
  if (saving.value || publishing.value || confirmingPublication.value) return
  if (!form.title && !form.content) {
    toast.warning('请输入标题或内容')
    return
  }
  const payload = postData()
  const snapshot = JSON.stringify(payload)
  saving.value = true
  saveStatus.value = 'saving'
  try {
    const data = {
      ...payload,
      draftId: currentDraftId.value || undefined,
      ...(editingPostId.value ? { postId: Number(editingPostId.value) } : {})
    }
    const res = await postApi.saveDraft(data)
    currentDraftId.value =
      typeof res.data === 'number'
        ? res.data
        : res.data?.draftId || res.data?.id || currentDraftId.value
    acceptSavedSnapshot(snapshot)
  } catch (err: any) {
    logger.error('save draft error', { error: err.message })
    saveStatus.value = ''
    if (!silent) toast.error(err.userMessage || err.response?.data?.message || '保存草稿失败')
  } finally {
    saving.value = false
  }
}

const publishPost = async () => {
  if (publishing.value || saving.value || confirmingPublication.value) return
  if (!form.title.trim()) {
    toast.warning('请输入文章标题')
    return
  }
  if (!form.content.trim()) {
    toast.warning('请输入文章内容')
    return
  }
  if (!form.category) {
    toast.warning('请选择文章分类')
    return
  }
  if (form.content.length > 300000) {
    toast.warning('文章内容不能超过30万字符')
    return
  }

  // Keep the opener enabled until the dialog has captured it for focus return.
  confirmingPublication.value = true
  try {
    const ok = await confirm('确定要发布此文章吗？')
    if (!ok) return
    confirmingPublication.value = false
    publishing.value = true
    const payload = postData()
    const snapshot = JSON.stringify(payload)
    const postId = editingPostId.value

    if (postId) {
      await postApi.updatePost(postId, payload)
      toast.success('文章更新成功')
    } else {
      const response = await postApi.createPost(payload)
      // The live API returns a numeric ID; retain compatibility with older object responses.
      const created = response.data as number | { id: number }
      publishedPostId.value = typeof created === 'number' ? created : created.id
      toast.success('文章发布成功')
    }

    acceptSavedSnapshot(snapshot)
    if (currentDraftId.value) {
      try {
        await postApi.deleteDraft(currentDraftId.value)
      } catch {
        /* ignore */
      }
      currentDraftId.value = null
    }
    if (dirty.value) {
      toast.warning('文章已发布；发布期间的新改动仍未保存。')
      return
    }
    await router.push('/')
  } catch (err: any) {
    logger.error('publish error', { error: err.message })
    toast.error(err.response?.data?.message || '发布失败')
  } finally {
    publishing.value = false
    confirmingPublication.value = false
  }
}

const autoSave = async () => {
  if (!dirty.value) return
  if (!form.title && !form.content) return
  await saveDraft(true)
}

function handleBeforeUnload(e: BeforeUnloadEvent) {
  if (dirty.value) {
    e.preventDefault()
    e.returnValue = ''
  }
}

let autoSaveTimer: ReturnType<typeof setInterval> | undefined = undefined

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  isLoading.value = true
  await fetchTags()
  if (route.params.id) {
    await fetchPost()
  } else if (route.query.draft) {
    await fetchDraftById(route.query.draft as string)
  }
  isLoading.value = false
  history.value = [form.content]
  historyIndex.value = 0
  savedSnapshot.value = JSON.stringify(postData())

  autoSaveTimer = setInterval(() => {
    if (dirty.value) autoSave()
  }, 30000)
})

onUnmounted(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
  if (autoSaveTimer) {
    clearInterval(autoSaveTimer)
    autoSaveTimer = undefined
  }
  if (historyTimer) {
    clearTimeout(historyTimer)
    historyTimer = undefined
  }
  clearTimeout(saveStatusTimer)
})

onBeforeRouteLeave((_to, _from) => {
  if (dirty.value) {
    const answer = window.confirm('有未保存的更改，确定要离开吗？')
    if (!answer) return false
  }
})
</script>

<style scoped>
/* 主页面 */
.post-edit-page {
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px 24px 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 返回按钮 */
.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 44px;
  padding-inline: 14px;
  background: var(--surface-primary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  color: var(--text-secondary);
  cursor: pointer;
  font-size: var(--text-sm);
  transition:
    color var(--transition-fast),
    border-color var(--transition-fast);
  width: fit-content;
}

.back-btn:hover {
  color: var(--accent);
  border-color: var(--accent);
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 56px;
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  border: 1px solid var(--border-light);
  color: var(--text-muted);
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 编辑容器 */
.edit-container {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: var(--spacing-lg);
  align-items: start;
}

/* 编辑器头部 */
.editor-header {
  grid-column: 1 / -1;
  padding: 18px 22px;
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  border: 1px solid var(--border-light);
}

.header-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.title-input {
  flex: 1;
  min-width: 0;
  border: none;
  font-size: var(--text-2xl);
  font-weight: 700;
  background: transparent;
  color: var(--text-primary);
  overflow-wrap: break-word;
  word-break: break-word;
}

.title-input:focus {
  outline: none;
}

.title-input::placeholder {
  color: var(--text-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  flex-shrink: 0;
}

.save-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  color: var(--text-muted);
  min-width: 70px;
  text-align: right;
  transition: all var(--transition);
}

.save-status.saving {
  color: var(--primary);
}

.save-status.saved {
  color: var(--success);
}

.spin {
  animation: spin 1s linear infinite;
}

.header-meta {
  display: flex;
  align-items: center;
  gap: var(--spacing-sm);
  margin-top: var(--spacing-sm);
  font-size: var(--text-xs);
  color: var(--text-muted);
  flex-wrap: wrap;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.meta-sep {
  opacity: 0.5;
}

.meta-category,
.meta-time {
  color: var(--text-secondary);
}

/* 编辑器主体 */
.editor-main-wrapper {
  grid-column: 1;
  /* 网格项默认 min-width:auto，会让 .toolbar 的 overflow-x:auto 失效并把整页撑宽 */
  min-width: 0;
}

.editor-body {
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  border: 1px solid var(--border-light);
  overflow: hidden;
  min-height: 500px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 8px 12px;
  border-bottom: 1px solid var(--border-light);
  background: var(--surface-secondary);
  flex-wrap: wrap;
}

.toolbar-group {
  display: flex;
  align-items: center;
  gap: 2px;
}

.tool-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: transparent;
  border-radius: var(--radius-sm);
  cursor: pointer;
  font-size: var(--text-sm);
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  justify-content: center;
  transition:
    background var(--transition-fast),
    color var(--transition-fast);
  flex-shrink: 0;
}

.tool-btn:hover:not(:disabled) {
  background: var(--accent-light);
  color: var(--accent);
}

.tool-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.toolbar-divider {
  width: 1px;
  height: 20px;
  background: var(--border-light);
  margin: 0 4px;
  flex-shrink: 0;
}

.font-size-select {
  width: auto;
  height: 36px;
  padding: 0 10px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  font-size: var(--text-xs);
  color: var(--text-secondary);
  cursor: pointer;
}

.font-size-select:focus-visible {
  border-color: var(--accent);
  outline: 2px solid var(--accent-light);
}

.upload-progress {
  position: relative;
  width: 100px;
  height: 20px;
  background: var(--border);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-left: var(--spacing-sm);
  flex-shrink: 0;
}

.upload-progress-bar {
  position: absolute;
  left: 0;
  top: 0;
  height: 100%;
  background: linear-gradient(90deg, var(--primary-start), var(--primary-end));
  transition: width 0.3s ease;
  border-radius: var(--radius-full);
}

.upload-progress-text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-2xs);
  color: var(--text-inverse);
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
}

/* 编辑器内容区 */
.editor-content {
  position: relative;
}

.content-input {
  width: 100%;
  min-height: 460px;
  padding: 20px;
  border: none;
  font-size: var(--text-base);
  line-height: 1.9;
  color: var(--text-primary);
  background: transparent;
  resize: vertical;
  font-family: var(--font-mono), monospace;
}

.content-input:focus {
  outline: none;
}

.content-input::placeholder {
  color: var(--text-muted);
}

.editor-preview {
  padding: var(--spacing-lg);
  min-height: 460px;
}

.empty-preview {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--spacing-md);
  height: 300px;
  color: var(--text-muted);
}

/* 侧边栏 */
.editor-sidebar {
  grid-column: 2;
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.sidebar-section {
  padding: 18px;
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
  border: 1px solid var(--border-light);
}

.sidebar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--text-sm);
  font-weight: 600;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border-light);
  color: var(--text-primary);
}

.form-group {
  margin-bottom: 0;
}

.form-label {
  display: block;
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.summary-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  font-size: var(--text-sm);
  color: var(--text-primary);
  background: var(--surface-primary);
  resize: vertical;
  line-height: 1.6;
}

.summary-input:focus-visible {
  border-color: var(--accent);
  outline: 2px solid var(--accent-light);
  outline-offset: 1px;
}

.char-hint {
  display: block;
  text-align: right;
  font-size: var(--text-2xs);
  color: var(--text-muted);
  margin-top: 4px;
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  font-size: var(--text-base);
  background: var(--surface-primary);
  color: var(--text-primary);
}

.form-input:focus-visible {
  border-color: var(--accent);
  outline: 2px solid var(--accent-light);
  outline-offset: 1px;
}

/* 封面图上传 */
.cover-upload {
  margin-top: var(--spacing-sm);
}

.cover-preview {
  position: relative;
  display: inline-block;
  width: 100%;
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.cover-preview img {
  width: 100%;
  max-height: 160px;
  object-fit: cover;
  border-radius: var(--radius-lg);
}

.remove-cover {
  position: absolute;
  top: 8px;
  right: 8px;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  background: var(--preview-scrim);
  color: #fff;
  border: none;
  border-radius: var(--radius);
  cursor: pointer;
  font-size: var(--text-sm);
}

.remove-cover:hover {
  background: var(--error);
}

.upload-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 26px 16px;
  background: var(--surface-secondary);
  border: 1px dashed var(--border-light);
  border-radius: var(--radius-lg);
  color: var(--text-muted);
  cursor: pointer;
  transition:
    border-color var(--transition-fast),
    color var(--transition-fast);
  width: 100%;
}

.upload-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-light);
}

.upload-hint {
  font-size: var(--text-2xs);
  opacity: 0.7;
}

/* 标签选择 */
.tag-input-container {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: var(--spacing-sm);
  border: 1px solid var(--glass-border);
  border-radius: var(--radius);
  min-height: 40px;
  background: var(--glass-bg);
  transition: all var(--transition);
}

.tag-input-container:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.selected-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 2px 8px;
  background: var(--primary-light);
  color: var(--primary);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: 500;
}

.remove-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: none;
  color: inherit;
  cursor: pointer;
  padding: 0;
  line-height: 1;
  opacity: 0.6;
  transition: all var(--transition);
  border-radius: var(--radius-full);
}

.remove-tag:hover {
  opacity: 1;
  background: var(--primary);
  color: white;
}

.tag-input {
  flex: 1;
  min-width: 80px;
  border: none;
  font-size: var(--text-sm);
  background: transparent;
  color: var(--text-primary);
  padding: 2px 4px;
}

.tag-input:focus {
  outline: none;
}

.tag-input::placeholder {
  color: var(--text-muted);
}

.suggested-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-top: 8px;
}

.suggested-tag {
  min-height: 30px;
  padding: 2px 10px;
  font-size: var(--text-xs);
  background: var(--surface-secondary);
  color: var(--text-secondary);
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
}

.suggested-tag:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-light);
}

.no-tags {
  margin-top: 8px;
}

.btn-text {
  background: none;
  border: none;
  color: var(--accent);
  cursor: pointer;
  font-size: var(--text-xs);
  padding: 4px 0;
}

.btn-text:hover {
  text-decoration: underline;
}

.btn-xs {
  font-size: var(--text-xs);
}

/* 响应式 */
@media (max-width: 1020px) {
  .edit-container {
    grid-template-columns: 1fr;
  }

  .editor-sidebar {
    grid-column: 1;
  }

  .header-top {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: flex-end;
  }

  .post-edit-page {
    padding: 20px 16px 40px;
  }

  .title-input {
    font-size: var(--text-xl);
  }
}

@media (max-width: 768px) {
  .editor-body {
    min-height: 300px;
  }

  .content-input {
    min-height: 300px;
    padding: 16px;
    font-size: var(--text-base);
  }

  .toolbar {
    overflow-x: auto;
    flex-wrap: nowrap;
    -webkit-overflow-scrolling: touch;
  }

  .editor-header {
    padding: 16px;
  }
}

@media (max-width: 480px) {
  .header-actions {
    flex-wrap: wrap;
    gap: 6px;
  }

  .save-status {
    width: 100%;
    text-align: left;
    order: -1;
  }
}
</style>
