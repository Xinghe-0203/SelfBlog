<script lang="ts">
/**
 * 模型连接配置面板（v2.0.30）
 *
 * 从「网站设置」里独立出来的一块：接入任意 OpenAI 兼容接口，用于自动生成文章
 * 标题、描述与关键词。原先它和几十个站点设置挤在同一页，作者要滚很久才能找到；
 * 现在它挂在「SEO 管理」页顶部 —— 生成能力、SEO 元数据、模型连接三件事在同一屏。
 *
 * 面板**直接编辑父组件传进来的响应式表单对象**：面板与宿主页共用同一份设置，
 * 面板里改的值在宿主页保存时也会被带上。
 */

/** 面板负责的站点设置键：保存时只提交这些，不把无关设置一起写回。 */
export const LLM_SETTING_KEYS = [
  'llmEnabled',
  'llmProvider',
  'llmBaseUrl',
  'llmApiKey',
  'llmModel',
  'llmTimeoutSeconds',
  'llmMaxTokens',
  'llmThinking',
  'llmReasoningEffort',
  'llmAutoOnPublish',
  'llmCommentModeration'
] as const

/** 面板输入的形状：站点设置里 11 个 llm* 键（值都是字符串）。 */
export interface LlmForm {
  llmEnabled: string
  llmProvider: string
  llmBaseUrl: string
  llmApiKey: string
  llmModel: string
  llmTimeoutSeconds: string
  llmMaxTokens: string
  llmThinking: string
  llmReasoningEffort: string
  llmAutoOnPublish: string
  llmCommentModeration: string
}
</script>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { siteApi } from '@/api/site'
import { seoApi } from '@/api/seo'
import { useSiteStore, type SiteSettings } from '@/stores/site'
import type { SeoLlmTest } from '@/types/seo'
import type { LlmModel } from '@/types/llm'

const props = defineProps<{ form: LlmForm }>()
const emit = defineEmits<{ saved: []; failed: [message: string] }>()

const siteStore = useSiteStore()

/**
 * 直接编辑父组件传进来的响应式表单对象（同一份设置，多处共用）。
 *
 * 这里刻意取一个别名而不是在模板里写 `props.form.x`：
 * `vue/no-mutating-props` 无法区分「改 prop 本身」和「改 prop 里那个共享对象」，
 * 一律按前者报错。共享表单对象正是本组件的约定，所以用一个不同名的本地别名
 * 把「编辑共享表单」这件事表达清楚。
 */
const llm = props.form

/**
 * 服务端用来表示「密钥已保存」的哨兵串（8 个 U+2022）。
 *
 * 它是契约的一部分，不是装饰：`PUT /api/admin/site-settings` 收到这个字面值
 * 就保留已存密钥，收到空串则删除。前端不能自己造别的占位符，否则一次保存
 * 就会把作者的密钥悄悄清掉。
 */
const SECRET_MASK = '••••••••'

/** DeepSeek 官方接口的默认值：一键预设写这两项，其余（密钥/超时）保持作者原样。 */
const DEEPSEEK_BASE_URL = 'https://api.deepseek.com'
const DEEPSEEK_MODEL = 'deepseek-flash'

const testing = ref(false)
const testResult = ref<SeoLlmTest | null>(null)
const saving = ref(false)
const saveError = ref('')
const saveNotice = ref(false)

/** 密钥是否已配置：由哨兵串派生，避免把掩码当真实值展示或比较。 */
const llmKeyConfigured = computed(() => (llm.llmApiKey ?? '') === SECRET_MASK)

/** 模型候选列表（`GET /admin/seo/models`）。后端恒 200，上游不可用时回落内置候选。 */
const modelOptions = ref<LlmModel[]>([])
const modelsSource = ref<'upstream' | 'fallback' | ''>('')
const loadingModels = ref(false)

/**
 * 思考强度只在「DeepSeek + 思考模式开启」时才会真正发给上游。
 *
 * <p>下拉在其余情况下 disabled 而不是隐藏：作者要能看见这个能力存在、并且知道
 * 为什么现在不生效（提示文案就在字段下方）。</p>
 */
const reasoningEffortDisabled = computed(
  () => llm.llmThinking !== 'enabled' || llm.llmProvider !== 'deepseek'
)

/** 填入 DeepSeek 默认值：只改地址与模型，不动已保存的密钥与其它设置。 */
function applyDeepSeekPreset(): void {
  llm.llmProvider = 'deepseek'
  llm.llmBaseUrl = DEEPSEEK_BASE_URL
  llm.llmModel = DEEPSEEK_MODEL
}

/**
 * 探测上游模型候选。
 *
 * <p>失败一律**静默**回落为「没有候选下拉」，作者仍可手填模型名 —— 探测只是便利，
 * 不能因为它连不上就把整个面板标成错误。</p>
 */
async function loadModels(): Promise<void> {
  loadingModels.value = true
  try {
    const response = await seoApi.listModels()
    const payload = response.data
    modelOptions.value = payload?.models ?? []
    modelsSource.value = payload?.source ?? 'fallback'
  } catch {
    modelOptions.value = []
    modelsSource.value = ''
  } finally {
    loadingModels.value = false
  }
}

/**
 * 用**已保存**的配置测一次连通性。
 *
 * 刻意不把表单里的未保存值发过去：否则会出现「测试通过但保存失败」的错位，
 * 而且把未保存的密钥塞进额外请求会让它多经一次网络。后端对上游失败一律
 * 返回 200 + `ok:false`，所以这里只需要一个 try/catch 兜住本地网络错误。
 */
async function testConnection(): Promise<void> {
  testing.value = true
  testResult.value = null
  try {
    const response = await seoApi.testConnection()
    testResult.value = response.data
  } catch (err) {
    testResult.value = {
      ok: false,
      message:
        (err as Error & { userMessage?: string }).userMessage || '测试请求失败，请稍后重试。',
      latencyMs: 0,
      model: ''
    }
  } finally {
    testing.value = false
  }
}

/**
 * 保存连接配置。
 *
 * <p>只提交 11 个 llm* 键：后端是**局部合并**（只写传进来的键），
 * 所以不会碰到宿主页上的其它设置。</p>
 *
 * <p>密钥哨兵语义保持不变：原样提交哨兵 = 保留已存密钥，提交空串 = 删除。</p>
 */
async function saveSettings(): Promise<void> {
  saving.value = true
  saveError.value = ''
  saveNotice.value = false
  try {
    const payload: Partial<SiteSettings> = {
      llmEnabled: llm.llmEnabled,
      llmProvider: llm.llmProvider,
      llmBaseUrl: llm.llmBaseUrl,
      llmApiKey: llm.llmApiKey,
      llmModel: llm.llmModel,
      llmTimeoutSeconds: llm.llmTimeoutSeconds,
      llmMaxTokens: llm.llmMaxTokens,
      llmThinking: llm.llmThinking,
      llmReasoningEffort: llm.llmReasoningEffort,
      llmAutoOnPublish: llm.llmAutoOnPublish,
      llmCommentModeration: llm.llmCommentModeration
    }
    const response = await siteApi.updateSettings(payload)
    const updated = response.data
    if (updated) {
      for (const key of LLM_SETTING_KEYS) {
        const value = updated[key]
        if (typeof value === 'string') llm[key] = value
      }
      siteStore.applySettings(updated)
    }
    saveNotice.value = true
    emit('saved')
  } catch (err) {
    const message =
      (err as Error & { userMessage?: string }).userMessage || '保存失败，请检查填写内容后重试。'
    saveError.value = message
    emit('failed', message)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  // 模型候选是纯便利信息：探测失败静默，作者仍可手填模型名。
  void loadModels()
})
</script>

<template>
  <section class="llm-panel" data-llm-panel>
    <div class="panel-heading panel-heading-row">
      <div>
        <h2>AI 辅助与 SEO</h2>
        <p>
          接入任意 OpenAI
          兼容接口，用于自动生成文章标题、描述与关键词。密钥只保存在服务端，既不下发到前台，也不会写进日志。
        </p>
      </div>
      <div class="llm-actions">
        <button
          class="btn btn-ghost btn-sm"
          type="button"
          :disabled="testing"
          data-action="test-llm"
          @click="testConnection"
        >
          {{ testing ? '测试中…' : '测试连接' }}
        </button>
        <button
          class="btn btn-primary btn-sm"
          type="button"
          :disabled="saving"
          data-action="save-llm"
          @click="saveSettings"
        >
          {{ saving ? '保存中…' : '保存连接配置' }}
        </button>
      </div>
    </div>

    <label class="toggle-row"
      ><span
        ><strong>启用 AI 辅助</strong
        ><small>关闭后所有生成接口直接拒绝，已生成的 SEO 数据仍然生效。</small></span
      ><input
        v-model="llm.llmEnabled"
        type="checkbox"
        true-value="true"
        false-value="false"
        data-setting="llmEnabled"
    /></label>

    <div class="llm-provider">
      <label
        >服务商<select v-model="llm.llmProvider" data-setting="llmProvider">
          <option value="deepseek">DeepSeek 官方接口</option>
          <option value="custom">自定义 OpenAI 兼容接口</option>
        </select>
        <small
          >DeepSeek 才会发送思考模式与思考强度；自定义服务商按标准 OpenAI 兼容协议调用。</small
        ></label
      >
      <button
        class="btn btn-ghost btn-sm"
        type="button"
        data-action="llm-preset"
        @click="applyDeepSeekPreset"
      >
        填入 DeepSeek 默认
      </button>
    </div>

    <div class="llm-fields">
      <label
        >API 地址<input
          v-model.trim="llm.llmBaseUrl"
          inputmode="url"
          maxlength="500"
          placeholder="https://api.openai.com/v1"
          data-setting="llmBaseUrl"
        /><small
          >OpenAI 兼容的根地址，客户端会自动补 `/chat/completions`；末尾斜杠会被去掉。</small
        ></label
      >
      <label
        >API Key<input
          v-model.trim="llm.llmApiKey"
          type="password"
          maxlength="500"
          autocomplete="new-password"
          :placeholder="llmKeyConfigured ? '已配置' : '尚未配置'"
          data-setting="llmApiKey"
        /><small
          >已保存的密钥显示为 <code>{{ SECRET_MASK }}</code
          >：保持原样保存不会改动它，清空后保存即删除。<span
            class="llm-badge"
            :data-llm-key="llmKeyConfigured ? 'configured' : 'empty'"
            >{{ llmKeyConfigured ? '已配置' : '未配置' }}</span
          ></small
        ></label
      >
      <label v-if="modelOptions.length" class="llm-model-picker"
        >模型候选
        <div class="llm-model-row">
          <select v-model="llm.llmModel" data-setting="llmModelSelect">
            <option v-for="model in modelOptions" :key="model.id" :value="model.id">
              {{ model.name }}（{{ model.id }}）{{ model.supportsImage ? ' · 支持图像' : '' }}
            </option>
          </select>
          <button
            class="btn btn-ghost btn-sm"
            type="button"
            :disabled="loadingModels"
            data-action="refresh-models"
            @click="loadModels"
          >
            {{ loadingModels ? '探测中…' : '刷新' }}
          </button>
        </div>
        <small
          >来源：{{
            modelsSource === 'upstream' ? '上游实时列表' : '内置候选（上游暂不可用）'
          }}；也可在下方直接手填模型名。</small
        ></label
      >
      <label
        >模型名<input
          v-model.trim="llm.llmModel"
          maxlength="100"
          placeholder="gpt-4o-mini"
          data-setting="llmModel"
        /><small>由服务商决定，例如 deepseek-flash、gpt-4o-mini、qwen-plus。</small></label
      >
      <label
        >单次调用超时（秒）<input
          v-model.trim="llm.llmTimeoutSeconds"
          type="number"
          min="5"
          max="120"
          step="1"
          data-setting="llmTimeoutSeconds"
        /><small>5–120 秒。超时视为一次失败，不影响文章本身的保存。</small></label
      >
      <label
        >最大输出 token<input
          v-model.trim="llm.llmMaxTokens"
          type="number"
          min="0"
          max="131072"
          step="1"
          data-setting="llmMaxTokens"
        /><small
          >0–131072。填 0 表示不限制（不向上游发送该参数），由服务商默认值决定。</small
        ></label
      >
      <label
        >思考强度<select
          v-model="llm.llmReasoningEffort"
          :disabled="reasoningEffortDisabled"
          data-setting="llmReasoningEffort"
        >
          <option value="low">低 · 更快更省</option>
          <option value="high">高 · 更细致</option>
          <option value="max">最高 · 最慢最贵</option>
        </select>
        <small
          >仅在「DeepSeek + 思考模式开启」时生效；此时不再发送 temperature，由模型自行推理。</small
        ></label
      >
    </div>

    <label class="toggle-row"
      ><span
        ><strong>思考模式</strong
        ><small
          >开启后模型先推理再作答，质量更高但更慢、更贵；仅 DeepSeek
          服务商支持。关闭时思考强度下拉不可用。</small
        ></span
      ><input
        v-model="llm.llmThinking"
        type="checkbox"
        true-value="enabled"
        false-value="disabled"
        data-setting="llmThinking"
    /></label>

    <label class="toggle-row"
      ><span
        ><strong>发布后自动生成 SEO</strong
        ><small
          >文章发布后异步生成，失败只记日志、绝不阻塞保存。关闭时可到「SEO
          管理」手动批量生成。</small
        ></span
      ><input
        v-model="llm.llmAutoOnPublish"
        type="checkbox"
        true-value="true"
        false-value="false"
        data-setting="llmAutoOnPublish"
    /></label>

    <label class="toggle-row"
      ><span
        ><strong>访客评论自动审核</strong
        ><small
          >访客提交留言后异步调用模型审核，结论不是「通过」就落为待审状态；失败静默忽略，绝不影响留言提交。关闭时行为与现在一致。</small
        ></span
      ><input
        v-model="llm.llmCommentModeration"
        type="checkbox"
        true-value="true"
        false-value="false"
        data-setting="llmCommentModeration"
    /></label>

    <p v-if="testResult" class="llm-test" :data-ok="testResult.ok ? 'true' : 'false'">
      {{ testResult.message
      }}<template v-if="testResult.ok">
        （{{ testResult.model }}，{{ testResult.latencyMs }}ms）</template
      >
    </p>
    <p v-else class="llm-hint">
      测试连接使用<strong>已保存</strong>的配置；改了地址或密钥请先保存再测试。
    </p>

    <p v-if="saveNotice" class="llm-test" data-ok="true" data-llm-save="saved">连接配置已保存。</p>
    <p v-if="saveError" class="llm-test" data-ok="false" role="alert">{{ saveError }}</p>
  </section>
</template>

<style scoped>
.llm-panel {
  display: grid;
  align-content: start;
  gap: 15px;
  padding: 20px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-xl);
  background: var(--surface-primary);
}
.panel-heading {
  padding-bottom: 12px;
  border-bottom: 1px solid var(--border-light);
}
.panel-heading h2 {
  margin: 0 0 5px;
  font-size: var(--text-base);
}
.panel-heading p {
  margin: 0;
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.panel-heading-row {
  display: flex;
  align-items: start;
  justify-content: space-between;
  gap: 16px;
}
.llm-actions {
  display: flex;
  flex: 0 0 auto;
  gap: 8px;
}
input:not([type='checkbox']),
select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
  background: var(--surface-primary);
  color: var(--text-primary);
  font: inherit;
  font-size: var(--text-base);
}
.toggle-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 14px;
  padding: 13px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius);
}
.toggle-row span {
  display: grid;
  gap: 4px;
}
.toggle-row strong {
  font-size: var(--text-sm);
}
.toggle-row small {
  max-width: 400px;
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.toggle-row input {
  flex: 0 0 auto;
  width: 20px;
  height: 20px;
  accent-color: var(--accent);
}
.llm-provider {
  display: flex;
  align-items: end;
  justify-content: space-between;
  gap: 14px;
}
.llm-provider label {
  display: grid;
  flex: 1 1 auto;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.llm-provider label small {
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.llm-model-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.llm-model-row select {
  flex: 1 1 auto;
  min-width: 0;
}
.llm-model-row .btn {
  flex: 0 0 auto;
}
.llm-fields {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}
.llm-fields label {
  display: grid;
  gap: 7px;
  color: var(--text-secondary);
  font-size: var(--text-sm);
}
.llm-fields label small {
  color: var(--text-muted);
  font-size: var(--text-xs);
  line-height: 1.6;
}
.llm-fields code {
  padding: 1px 4px;
  border-radius: var(--radius-xs);
  background: var(--bg-tertiary);
  font-size: var(--text-2xs);
  letter-spacing: 0.08em;
}
.llm-badge {
  display: inline-block;
  margin-left: 6px;
  padding: 1px 7px;
  border-radius: var(--radius-full);
  font-size: var(--text-2xs);
  font-weight: 600;
}
.llm-badge[data-llm-key='configured'] {
  background: var(--success-light);
  color: var(--success);
}
.llm-badge[data-llm-key='empty'] {
  background: var(--bg-tertiary);
  color: var(--text-muted);
}
.llm-test,
.llm-hint {
  margin: 0;
  padding: 11px 13px;
  border-radius: var(--radius);
  font-size: var(--text-sm);
  line-height: 1.6;
}
.llm-test[data-ok='true'] {
  background: var(--success-light);
  color: var(--success);
}
.llm-test[data-ok='false'] {
  background: var(--error-light);
  color: var(--error);
}
.llm-hint {
  background: var(--bg-secondary);
  color: var(--text-muted);
}
@media (max-width: 760px) {
  .llm-fields {
    grid-template-columns: 1fr;
  }
  .llm-provider {
    align-items: start;
    flex-direction: column;
  }
  .panel-heading-row {
    flex-direction: column;
  }
  .llm-actions {
    flex-wrap: wrap;
  }
}
</style>
