# 第五章 Vue 3 全家桶实战

在前面的章节里，我们已经用 Spring Boot 把后端 API 搭了起来。本章开始进入 SelfBlog 的前端世界。我们将以 SelfBlog 的真实源码为蓝本，系统学习 **Vue 3 全家桶**：核心的 Composition API、模板语法、组件体系、Vue Router 4 路由、Pinia 状态管理、Axios 网络请求封装，以及测试与性能优化。学完本章，你应当能够独立读懂并扩展 SelfBlog 的前端代码。

SelfBlog 前端使用的技术栈版本如下（以 `frontend/package.json` 与 `package-lock.json` 锁定版本为准）：

| 依赖 | 版本 | 作用 |
| --- | --- | --- |
| vue | 3.5.44 | 渐进式 JavaScript UI 框架 |
| vite | 6.4.4 | 开发服务器与构建工具 |
| vue-router | 4.3.0 | 官方路由 |
| pinia | 2.1.7 | 官方状态管理 |
| axios | 1.7.4 | HTTP 客户端 |
| marked + dompurify | 12.x / 3.x | Markdown 渲染与 XSS 防护 |
| typescript + vue-tsc | 6.0.3 / 3.3.3 | 类型检查 |
| vitest + @vue/test-utils | 4.1.8 / 2.4.10 | 单元测试（jsdom 环境） |

---

## 5.1 Vue 3 简介与环境搭建

### 5.1.1 什么是 Vue 3：渐进式框架

Vue 是一款用于构建用户界面的**渐进式框架**。所谓"渐进式"，是指你可以按需采用它的能力：最简单时，只把 Vue 当作一个模板引擎嵌进现有页面；往后需要组件化，就引入单文件组件；页面多了需要路由，就加 Vue Router；数据共享复杂了，再加 Pinia。每一层都是可选的，不必一开始就接受整套框架体系。

Vue 3 相比 Vue 2 的几个关键变化：

1. **Composition API（组合式 API）**：以函数的方式组织逻辑，让"同一个功能的代码"聚在一起，逻辑可以抽成可复用的组合式函数（composable）。
2. **性能更好**：重写的响应式系统用 `Proxy` 替代 `Object.defineProperty`，能侦测到属性新增/删除和数组索引变化；编译期做了大量静态提升、补丁标记（patch flag）优化，更新更快、内存更省。
3. **更好的 TypeScript 支持**：整套 API 都是类型友好的，`defineProps<T>()` 直接从泛型推导 props 类型。
4. **`<script setup>` 语法糖**：让单文件组件的写法更简洁，SelfBlog 全部组件都采用这种写法。

### 5.1.2 Composition API 与 Options API

同一个组件可以用两种风格书写。先看 **Options API**（选项式）：

```vue
<script>
export default {
  data() {
    return { count: 0, doubled: 0 }
  },
  computed: {
    double() {
      return this.count * 2
    }
  },
  methods: {
    increment() {
      this.count++
    }
  },
  mounted() {
    console.log('组件已挂载')
  }
}
</script>

<template>
  <button @click="increment">点了 {{ count }} 次，翻倍是 {{ double }}</button>
</template>
```

再看等价的 **Composition API**（组合式）：

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'

const count = ref(0)
const double = computed(() => count.value * 2)

function increment(): void {
  count.value++
}

onMounted(() => {
  console.log('组件已挂载')
})
</script>

<template>
  <button @click="increment">点了 {{ count }} 次，翻倍是 {{ double }}</button>
</template>
```

两者能力完全等价，区别在于**代码组织方式**：

- Options API 按"选项类型"归类（数据都进 `data`，方法都进 `methods`），组件小时直观，但组件一大，同一个功能的代码就被撕成了几段，在数千行的组件里来回跳转非常痛苦。
- Composition API 按"功能"归类，`ref`/`computed`/函数/监听器写在一起；而且逻辑可以整体抽离成 `useXxx()` 函数，在多个组件间复用。

**如何选择**：新项目、需要 TypeScript、需要复用逻辑，用 Composition API；维护老的 Vue 2 风格代码库，或只是写几个极简的演示组件，Options API 也完全没问题。SelfBlog 前端规范明确要求"使用 Vue 3 Composition API（`<script setup>`）"（见 `frontend/CLAUDE.md`），全书后续示例都以这种风格为主。

### 5.1.3 用 Vite 创建项目

**Vite**（法语"快"，读 /vit/）是 Vue 作者尤雨溪开发的构建工具。开发阶段它基于浏览器原生 ES Module，按需编译被请求的模块，所以冷启动几乎是毫秒级的；热更新（HMR）也只替换改动的那一小块，改完代码浏览器瞬间就能看到效果。生产构建则用 Rollup（Vite 6 里也可用 esbuild 压缩）打包出高度优化的静态资源。

创建一个 Vue 3 + TypeScript 项目的推荐方式：

```bash
# 官方脚手架 create-vue，会交互式询问要不要 Router / Pinia / Vitest 等
npm create vue@latest my-blog

# 或者只用 Vite 模板
npm create vite@latest my-blog -- --template vue-ts

cd my-blog
npm install
npm run dev   # 默认 http://localhost:5173
```

create-vue 会问一系列问题：是否使用 TypeScript（建议选是）、是否引入 Vue Router、是否引入 Pinia、是否引入 Vitest 单元测试、是否引入 ESLint/Prettier 等。SelfBlog 选择了以上全部。

启动后你会看到两个关键命令：

```bash
npm run dev      # 开发服务器（SelfBlog 固定端口 3000）
npm run build    # 先 vue-tsc --noEmit 类型检查，再 vite build 打包
```

SelfBlog 的 `frontend/package.json` 里还能看到几个工程化脚本，它们构成了前端的"质量闸门"：`lint:check`（ESLint 只读检查）、`format:check`（Prettier 格式检查）、`type-check`（`vue-tsc --noEmit`）、`test:run`（Vitest 单次运行）。每次提交前这五项都应该保持绿色——这是保证"43 个测试文件 / 400 个测试"基线不倒退的习惯。

### 5.1.4 认识项目结构：以 SelfBlog 为例

一个 Vite + Vue 3 项目的入口是根目录的 `index.html`——它**不是**由某段代码生成的，而就是应用的宿主页面，其中有一行 `<div id="app"></div>`，Vue 应用最终会挂载到这个节点上。真正的 JS 入口由 `index.html` 里的 `<script type="module" src="/src/main.ts">` 引入。

SelfBlog 前端 `frontend/src` 的目录结构如下：

```
src/
├── main.ts          # 应用入口：创建应用、装配插件、启动引导
├── App.vue          # 根组件（主题根 #selfblog、导航、路由出口）
├── api/             # Axios 封装与各业务 API 模块（index.ts / site.ts / post.ts ...）
├── components/      # 通用组件（common/、layout/、post/、pages/、admin/）
├── composables/     # 组合式函数（useDialog / useConfirm / useSeoMeta / useScrollReveal ...）
├── router/          # 路由（index.ts、guards.ts、modules/ 按业务拆分）
├── stores/          # Pinia 状态（site / theme / user / music / pages）
├── styles/          # 样式（main.css 入口 + variables.css 设计 token + dark-mode.css ...）
├── types/           # TypeScript 类型定义
├── utils/           # 工具（crypto / logger / toast / visitor / reading）
├── constants/       # 常量（如 STORAGE_KEY_PREFIX = 'edu_'）
├── views/           # 页面级组件（Home / About / Archives / admin/ ...）
└── test/            # 测试全局配置 setup.ts
```

几个值得记住的分层约定：

- **`views/` 放"页面级"组件**（一个路由对应一个），**`components/` 放可复用组件**（被页面引用）。
- **`api/` 统一收口网络请求**：页面不直接写 `axios.get(...)`，而是调用 `src/api/` 里导出的语义化函数。
- **`stores/` 只放需要跨组件共享的状态**，组件私有的临时状态仍留在组件里。
- `vite.config.ts` 里配置了两个对本项目至关重要的东西：**路径别名** `@` → `src`（所以到处能看到 `import x from '@/stores/site'`），以及**开发代理**——`/api` 与 `/uploads` 都转发到 `VITE_API_TARGET`（默认 `http://localhost:8825`，即后端端口），这样开发时不存在跨域问题。

```ts
// frontend/vite.config.ts（节选）
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  return {
    plugins: [vue()],
    resolve: { alias: { '@': resolve(__dirname, 'src') } },
    server: {
      port: 3000,
      proxy: {
        '/api': { target: env.VITE_API_TARGET || 'http://localhost:8825', changeOrigin: true },
        '/uploads': { target: env.VITE_API_TARGET || 'http://localhost:8825', changeOrigin: true }
      }
    }
  }
})
```

---

## 5.2 模板语法

Vue 的模板就是"增强版 HTML"：在原生 HTML 的基础上加入插值和指令（以 `v-` 开头的特殊属性）。模板会被编译成渲染函数，最终生成虚拟 DOM。

### 5.2.1 文本插值

双大括号 `{{ }}` 是最基本的插值，里面可以放**任何 JavaScript 表达式**（不是语句）：

```vue
<template>
  <p>{{ title }}</p>
  <p>{{ count > 0 ? `共 ${count} 篇` : '暂无文章' }}</p>
  <p>{{ new Date().getFullYear() }}</p>
  <!-- 注意：下面这些是非法的 -->
  <!-- {{ if (ok) { return 'a' } }}   语句不行 -->
  <!-- {{ const a = 1 }}              声明不行 -->
</template>
```

插值内容会被当作纯文本渲染——Vue 自动做了 HTML 转义，所以 `{{ userInput }}` 是**天然防 XSS** 的。如果你确实要渲染富文本 HTML，必须用 `v-html`，并且像 SelfBlog 那样先用 DOMPurify 消毒：

```vue
<!-- SelfBlog 渲染 Markdown 正文时：marked 转 HTML，DOMPurify 消毒，才允许 v-html -->
<article v-html="sanitizedBody"></article>
```

### 5.2.2 属性绑定 v-bind 与简写 `:`

HTML 属性想引用 JS 变量，不能用 `{{ }}`（那会被当成字面字符串），要用 `v-bind`，简写为一个冒号：

```vue
<template>
  <!-- 完整写法 -->
  <a v-bind:href="postUrl">文章链接</a>
  <!-- 简写 -->
  <a :href="postUrl">文章链接</a>

  <!-- 动态拼 class：对象语法 -->
  <div :class="{ 'tag--closable': closable, 'tag--disabled': disabled }"></div>
  <!-- 数组语法 -->
  <span :class="['tag', `tag--${variant}`, `tag--${size}`]"></span>

  <!-- 绑定布尔属性 -->
  <button :disabled="loading">保存</button>

  <!-- 动态属性名（方括号） -->
  <div :[attrName]="attrValue"></div>

  <!-- 绑定整个对象展开成多个属性 -->
  <input v-bind="{ type: 'text', placeholder: hint }" />
</template>
```

SelfBlog 的 `components/common/Tag.vue` 就大量使用这些语法：`:class` 数组 + 模板字符串拼出 BEM 风格类名，`:disabled` 绑定按钮状态。

### 5.2.3 事件绑定 v-on 与简写 `@`

`v-on` 监听 DOM 事件，简写为 `@`：

```vue
<template>
  <button @click="increment">+1</button>

  <!-- 内联表达式（简单逻辑可以直接写） -->
  <button @click="count++">内联自增</button>

  <!-- 带参数与事件对象：原生事件对象 $event -->
  <button @click="say('hi', $event)">带参调用</button>

  <!-- 修饰符链 -->
  <form @submit.prevent="onSubmit">提交不刷新页面</form>
  <div @click.stop="onClickInner">阻止冒泡</div>
  <a @click.prevent.stop="onLinkClick">阻止默认行为且阻止冒泡</a>
  <input @keyup.enter="onEnter" @keyup.esc="onEsc" />
  <button @click.once="init">只触发一次</button>
</template>
```

常用修饰符：`.prevent`（`event.preventDefault()`）、`.stop`（`stopPropagation()`）、`.once`、`.self`、按键类的 `.enter`/`.esc`/`.tab`，以及系统修饰键 `.ctrl`/`.alt`/`.shift`/`.meta`。

### 5.2.4 条件渲染：v-if 与 v-show

```vue
<template>
  <!-- v-if：惰性渲染，条件为假时元素根本不存在于 DOM -->
  <p v-if="type === 'a'">A</p>
  <p v-else-if="type === 'b'">B</p>
  <p v-else>其他</p>

  <!-- v-show：永远渲染，只是切换 display:none -->
  <p v-show="isVisible">我只是在隐藏</p>
</template>
```

**如何选择**：

- `v-if` 是真正的条件渲染，切换时有完整的创建/销毁开销；初始条件大概率不为真时用它（渲染得更少）。
- `v-show` 只切换 CSS，适合频繁切换显示状态的元素（比如 SelfBlog 返回顶部按钮 `BackToTop.vue` 那种滚动后才出现的控件）。
- `v-if` 可以用在 `<template>` 标签上做"无渲染包装"，`v-show` 不行（`template` 不产生真实 DOM）。

### 5.2.5 列表渲染 v-for 与 key

```vue
<script setup lang="ts">
import { ref } from 'vue'

interface Article {
  id: number
  title: string
  category?: string
}

const articles = ref<Article[]>([
  { id: 1, title: 'Vue 3 上手' },
  { id: 2, title: 'Pinia 入门', category: '前端' }
])
</script>

<template>
  <!-- 数组：(item, index) -->
  <li v-for="(article, index) in articles" :key="article.id">
    {{ index + 1 }}. {{ article.title }}
  </li>

  <!-- 对象：(value, key, index) -->
  <li v-for="(value, key) in siteSettings" :key="key">{{ key }}: {{ value }}</li>

  <!-- 数字：渲染 1..10 -->
  <span v-for="n in 10" :key="n">{{ n }} </span>
</template>
```

**`key` 的意义**：Vue 更新列表时默认采用"就地复用"策略，逐个比对同位置的节点。如果列表会插入、删除、排序，不带 key 或用 `index` 当 key，会导致节点错位复用——比如带输入框的列表删除中间一项后，输入框内容"串行"了。给每项一个**稳定且唯一**的 `key`（通常是后端 id），Vue 就能精确识别哪个节点应该移动/复用/删除。这是面试高频题，也是最容易踩的坑之一：**不要用数组下标当 key，除非列表纯静态渲染且永不重排**。

### 5.2.6 表单绑定 v-model

`v-model` 是"值绑定 + 事件监听"的语法糖：

```vue
<input v-model="text" />
<!-- 等价于 -->
<input :value="text" @input="text = ($event.target as HTMLInputElement).value" />
```

不同表单元素对应不同事件：`input`/`textarea` 用 `input` 事件，复选框/单选框/下拉框用 `change` 事件。修饰符：

```vue
<input v-model.lazy="draft" />     <!-- change 事件才同步，而非每次输入 -->
<input v-model.number="age" />     <!-- 自动转数字 -->
<input v-model.trim="username" />  <!-- 自动去首尾空格 -->
<textarea v-model="bio"></textarea>

<!-- 复选框绑定数组：勾选的值收进数组 -->
<input type="checkbox" value="vue" v-model="stack" />
```

**一个初学者常见误区**：`v-model` 绑定的变量必须是响应式的（`ref` 或 `reactive` 的属性）。如果直接绑定 props 上的值，Vue 会警告"Cannot mutate props"——正确做法见 5.4.4 节的组件 v-model。

### 5.2.7 综合示例：一个搜索框组件

把本节知识串起来，写一个 SelfBlog 风格的搜索输入组件：

```vue
<script setup lang="ts">
import { ref } from 'vue'

const keyword = ref('')
const history = ref<string[]>([])

function search(): void {
  const kw = keyword.value.trim()
  if (!kw) return
  history.value = [kw, ...history.value.filter((h) => h !== kw)].slice(0, 5)
  keyword.value = kw
}
</script>

<template>
  <form class="search" @submit.prevent="search">
    <input
      v-model.trim="keyword"
      type="search"
      placeholder="搜索文章…"
      @keyup.esc="keyword = ''"
    />
    <button type="submit" :disabled="!keyword">搜索</button>
  </form>
  <ul v-if="history.length" class="search-history">
    <li v-for="item in history" :key="item" @click="keyword = item">{{ item }}</li>
  </ul>
  <p v-else class="search-empty">暂无搜索历史</p>
</template>
```

---

## 5.3 组合式 API 核心

### 5.3.1 `<script setup>`：setup 语法糖

`<script setup>` 是 Composition API 在单文件组件里的编译期语法糖，比标准的 `setup()` 函数更简洁：

```vue
<script setup lang="ts">
import { ref } from 'vue'
import Tag from '@/components/common/Tag.vue'

const msg = ref('hello')
// 顶层声明的变量、函数、导入的组件——全部自动暴露给模板，不用 return
function reverse(): void {
  msg.value = msg.value.split('').reverse().join('')
}
</script>

<template>
  <p>{{ msg }}</p>
  <button @click="reverse">反转</button>
  <!-- 导入的组件直接用，不用注册 components -->
  <Tag variant="primary">标签</Tag>
</template>
```

它带来三重好处：一是不用写 `return { ... }`，二是导入的组件直接可用，三是编译器能做**更激进的静态分析**（比如把静态节点提升出渲染函数），性能更好。`lang="ts"` 则让整个组件获得完整类型检查。

### 5.3.2 ref 与 reactive：两种响应式数据

**`ref`** 可以包装任何类型的值（原始值、对象、函数都行），返回一个带 `.value` 属性的引用对象：

```ts
import { ref } from 'vue'

const count = ref(0)
const user = ref({ name: 'Zora', tags: ['vue'] })

count.value++              // 修改要通过 .value
user.value.name = 'Zora'   // 对象内部依然是响应式的（ref 内部用了 reactive）
```

**`reactive`** 只接受对象（含数组、Map/Set），返回原对象的 Proxy，访问时不需要 `.value`：

```ts
import { reactive } from 'vue'

const form = reactive({ title: '', tags: [] as string[] })
form.title = '新文章'      // 直接改
form.tags.push('vue')      // 数组方法也正常
```

**实践建议（SelfBlog 的实际取向）**：

- 统一优先用 `ref`。原因有二：其一，`ref` 能包装任意类型（数字、字符串、布尔、对象通吃）；其二，`ref` 可以**整体替换**（`state.value = newState`），而 `reactive` 不能替换引用（替换后就断了响应式）。SelfBlog 的 `stores/pages.ts` 里 `const pages = ref<Record<string, PublicPage>>({})`，每次加载清单后整体赋值 `pages.value = Object.fromEntries(...)`，就是靠整体替换实现的。
- `reactive` 适合"表单"这类字段固定的局部对象，且**绝不解构**（原因见下）。

**`reactive` 的头号大坑——解构失活**：

```ts
const state = reactive({ count: 0 })
let { count } = state        // ❌ 解构出的是普通数字，与原对象断开
count++                      // 不会触发任何更新

const { count } = toRefs(state)  // ✅ 用 toRefs 解构，得到 ref
```

### 5.3.3 ref 的解包规则

"解包"指访问 `ref` 时不需要写 `.value` 的场景。规则值得专门背下来：

```ts
// 1) 模板顶层 ref 自动解包
const count = ref(0)
// 模板里写 {{ count }} 而不是 {{ count.value }}

// 2) reactive 对象的属性位置会解包
const state = reactive({ count })
state.count++        // ✅ 不用 .value

// 3) 数组和集合里【不会】解包
const list = [count]
list[0].value++      // ⚠️ 必须 .value

// 4) 普通函数里、普通对象字面量里都不会自动解包
const obj = { count }        // obj.count 是 ref，要 obj.count.value
function logIt() {
  console.log(count.value)   // JS 代码里永远要 .value
}

// 5) 解构 reactive 会丢响应式；解构 ref 组成的对象需要 toRefs
```

初学者最常见的报错就是"值不更新"或"拿到的是 ref 对象而不是值"——九成是解包规则没理清。口诀：**JS 代码里 `.value`，模板与 reactive 属性位不用**。

### 5.3.4 toRef 与 toRefs

```ts
import { reactive, toRef, toRefs } from 'vue'

const state = reactive({ count: 0, name: 'Zora' })

// toRef：为某个属性创建一个与源保持同步的 ref
const countRef = toRef(state, 'count')
countRef.value++      // state.count 同步变成 1
state.count++         // countRef.value 同步变成 2

// toRefs：把整个 reactive 对象转成"全是 ref 的普通对象"，常用于安全解构
const { count, name } = toRefs(state)

// Pinia 里同样重要：store 返回的是 reactive 对象，
// 直接解构会失活，必须用 storeToRefs（见 5.7 节）
```

### 5.3.5 computed 计算属性

```ts
import { ref, computed } from 'vue'

const articles = ref<Article[]>([])
const keyword = ref('')

// getter：基于依赖自动缓存
const filtered = computed(() =>
  keyword.value
    ? articles.value.filter((a) => a.title.includes(keyword.value))
    : articles.value
)

// 可写 computed（setter）：很少用但要会
const fullName = computed({
  get: () => `${firstName.value} ${lastName.value}`,
  set: (value) => {
    [firstName.value, lastName.value] = value.split(' ')
  }
})
```

`computed` 与"用函数返回结果"的区别：**缓存**。只有依赖的响应式数据变化时才重新计算，模板里多处引用也只算一次；普通方法每次渲染都会执行。另外注意：`computed` 里不要做副作用（发请求、改别的状态），那是 `watch` 的职责。SelfBlog 的 `stores/site.ts` 里 `adminRoot = computed(() => '/' + adminPath.value)`、`isMusicEnabled = computed(() => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl)` 都是最典型的派生状态。

### 5.3.6 watch 侦听器

```ts
import { ref, watch } from 'vue'

const keyword = ref('')

// 侦听单个 ref
watch(keyword, (newVal, oldVal) => {
  console.log(`从 "${oldVal}" 变成 "${newVal}"`)
})

// 立即执行一次 + 防抖搜索
watch(keyword, async (kw) => {
  if (!kw) return
  await new Promise((r) => setTimeout(r, 300))
  const res = await searchApi.list({ keyword: kw })
  results.value = res.data
}, { immediate: true })

// 侦听 reactive 对象：隐式深度侦听，且 newVal/oldVal 不相等（同一引用）
const form = reactive({ title: '' })
watch(form, () => console.log('form 变了'), { deep: true })

// 侦听 getter：精准侦听某个字段
watch(() => form.title, (t) => console.log('title:', t))

// 一次侦听多个源
watch([keyword, page], ([kw, p]) => fetchList(kw, p))

// 停止侦听：watch 返回停止函数；组件卸载时同步创建的侦听器会自动停止
const stop = watch(keyword, onSearch)
stop()
```

关键选项：`immediate`（创建时立即执行一次回调）、`deep`（深度遍历对象内部变化）、`flush: 'post'`（回调延迟到 DOM 更新之后，回调里能拿到更新后的 DOM）。

**坑位提醒**：直接 `watch(reactiveObj)` 时回调里新旧值是同一个对象引用；想拿到"哪一项变了"，用 getter 形式 `watch(() => obj.field, ...)`。

### 5.3.7 watchEffect：自动追踪副作用

```ts
import { watchEffect, ref } from 'vue'

const keyword = ref('a')
const page = ref(1)

// 不写侦听目标，回调里用到谁就自动追踪谁；创建时立即执行一次
const stop = watchEffect(async (onCleanup) => {
  const kw = keyword.value
  const p = page.value
  const controller = new AbortController()
  onCleanup(() => controller.abort())   // 依赖再变时，取消上一次未完成请求
  const res = await fetch(`/api/post/list?keyword=${kw}&page=${p}`, {
    signal: controller.signal
  })
  results.value = await res.json()
})
```

**watch 与 watchEffect 怎么选**：

- 需要明确知道"谁变了"、需要新旧值对比、回调里只想对特定数据做反应 → `watch`。
- 多个依赖共同决定一个副作用、想要"立即执行 + 自动追踪" → `watchEffect`。
- 回调是异步函数时，`watchEffect` 的自动追踪只覆盖**同步执行的那一段**——异步之后的依赖不会被追踪。这也是上例把依赖先读进局部变量的原因。

### 5.3.8 生命周期钩子

Composition API 里通过 `onXxx` 函数注册钩子，必须**在 `setup` 同步执行期间**调用（不能放进 `setTimeout` 或普通函数里）：

```ts
import { onMounted, onUpdated, onUnmounted, onBeforeUnmount } from 'vue'

// <script setup> 中没有 beforeCreate / created，
// 它们的时机就是 <script setup> 本身的执行
onMounted(() => {
  console.log('DOM 已挂载，可以安全访问 ref / window')
})

onBeforeUnmount(() => {
  // 卸载前清理：移除全局监听、清定时器、断开 Observer
})

onUnmounted(() => {
  console.log('组件已卸载')
})
```

完整对照表：

| Options API | Composition API | 说明 |
| --- | --- | --- |
| beforeCreate / created | （`<script setup>` 本身） | 组件实例初始化前后 |
| beforeMount | onBeforeMount | 首次渲染前 |
| mounted | onMounted | 挂载完成，可访问 DOM |
| beforeUpdate | onBeforeUpdate | 响应式数据变了、DOM 更新前 |
| updated | onUpdated | DOM 更新完成 |
| beforeUnmount | onBeforeUnmount | 卸载前（做清理的地方） |
| unmounted | onUnmounted | 卸载完成 |
| activated / deactivated | onActivated / onDeactivated | 被 `<keep-alive>` 缓存的组件激活/失活 |

**最容易踩的坑**：在 `onMounted` 里给 `window`/`document` 注册了监听器却忘了在 `onBeforeUnmount` 里移除——组件反复进出路由后监听器越积越多，这就是内存泄漏。SelfBlog 的测试规范特别强调这一点：凡是挂载会注册全局监听器的组件，测试里要自己 `enableAutoUnmount(afterEach)`（见 5.13 节）。`stores/theme.ts` 也在 `destroyTheme()` 里专门移除 `matchMedia` 的 `change` 监听，并在重新初始化前先调用它清理旧监听。

---

## 5.4 组件基础

### 5.4.1 单文件组件 SFC 的三段结构

单文件组件（Single-File Component）把一个组件的**结构（template）、逻辑（script）、样式（style）**封装进一个 `.vue` 文件。以 SelfBlog 的 `components/common/Tag.vue` 为例（节选）：

```vue
<script setup lang="ts">
import { computed } from 'vue'

export interface TagProps {
  variant?: 'default' | 'primary' | 'success' | 'danger'
  size?: 'sm' | 'md'
  closable?: boolean
  disabled?: boolean
}

const props = withDefaults(defineProps<TagProps>(), {
  variant: 'default',
  size: 'sm',
  closable: false,
  disabled: false
})

const emit = defineEmits<{ close: []; click: [event: MouseEvent] }>()

const classes = computed(() => [
  'tag',
  `tag--${props.variant}`,
  { 'tag--closable': props.closable, 'tag--disabled': props.disabled }
])
</script>

<template>
  <span :class="classes" @click="emit('click', $event)">
    <span class="tag__content"><slot /></span>
    <button v-if="closable" class="tag__close" @click.stop="emit('close')">✕</button>
  </span>
</template>

<style scoped>
.tag { display: inline-flex; align-items: center; }
</style>
```

要点：

- `<style scoped>` 给样式加上"作用域"：编译后每个元素带 `data-v-xxx` 属性，选择器自动限定在当前组件，不会污染全局。需要穿透到子组件时用 `:deep(.child-class)`。
- 组件命名用 PascalCase（`Tag.vue`、`ArticleList.vue`），模板里既可写 `<Tag />` 也可写 `<tag />`，建议统一 PascalCase。

### 5.4.2 props：定义与校验

父子组件通信的第一通道。props 是**单向数据流**：父传子，子不应该直接改。

TypeScript 泛型写法（SelfBlog 全用这种）：

```ts
interface Props {
  postId: number
  title: string
  commentsEnabled?: boolean
}

// 仅类型声明（编译期检查）
const props = defineProps<Props>()

// 带默认值
const props = withDefaults(defineProps<Props>(), {
  commentsEnabled: true
})
```

运行时对象写法（能做更细的运行时校验，两种可以按需选用）：

```ts
const props = defineProps({
  postId: { type: Number, required: true },
  title: { type: String, default: '' },
  tags: {
    type: Array as PropType<string[]>,
    default: () => [],
    validator: (value: string[]) => value.length <= 20
  }
})
```

三条纪律：

1. **不要在子组件里改 props**。需要基于 props 派生，用 `computed`；需要"用户改过之后的副本"，用 `ref(props.modelValue)` 再通过事件交回父层。
2. 对象/数组类型的默认值必须是**工厂函数**：`default: () => []`，否则所有实例共享同一个数组。
3. props 名在模板里用 kebab-case（`post-id`），在 `defineProps` 里用 camelCase（`postId`）。

### 5.4.3 emits：自定义事件

子向父通信用事件。`defineEmits` 声明组件会抛出哪些事件（以及参数类型）：

```ts
const emit = defineEmits<{
  close: []
  click: [event: MouseEvent]
  change: [id: number, value: string]
}>()

emit('close')
emit('change', 1, 'new')
```

`Tag.vue` 里关闭按钮的写法 `@click.stop="emit('close')"` 中，`.stop` 很关键：不阻止冒泡的话，点关闭按钮会先触发自己的 `close`、又冒泡到外层触发 `click`，父组件的"点标签"逻辑就被误触发了。

使用方：

```vue
<Tag variant="primary" closable @close="removeTag(tag)">{{ tag }}</Tag>
```

### 5.4.4 在组件上使用 v-model：双向绑定组件

原生表单有 `v-model`，自定义组件也可以有。组件上的 `v-model="x"` 展开为：

```vue
<MyInput v-model="title" />
<!-- 等价于 -->
<MyInput :modelValue="title" @update:modelValue="(v) => (title = v)" />
```

所以组件内部要做两件事：接收 `modelValue` prop、抛出 `update:modelValue` 事件：

```vue
<script setup lang="ts">
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

function onInput(e: Event): void {
  emit('update:modelValue', (e.target as HTMLInputElement).value)
}
</script>
<template>
  <input :value="props.modelValue" @input="onInput" />
</template>
```

Vue 3.4 起有更省事的 `defineModel()`（SelfBlog 的 Vue 3.5 完全支持）：

```vue
<script setup lang="ts">
const title = defineModel<string>({ required: true })
</script>
<template>
  <input v-model="title" />
</template>
```

一个组件还可以支持**多个 v-model**（用参数区分）：

```vue
<PageEditor v-model:title="draft.title" v-model:summary="draft.summary" />
<!-- 组件内：defineProps 含 title/summary，emits 含 'update:title' / 'update:summary' -->
```

### 5.4.5 插槽：默认、具名与作用域

插槽让父组件向子组件的指定位置"注入"模板内容，是组件通用化的利器。

```vue
<!-- Modal.vue（子） -->
<template>
  <div class="modal">
    <header><slot name="title">默认标题</slot></header>
    <div class="modal-body"><slot /></div>
    <footer><slot name="footer"></slot></footer>
  </div>
</template>
```

```vue
<!-- 使用方（父） -->
<Modal show>
  <template #title>删除确认</template>
  <p>确定要删除这篇文章吗？</p>
  <template #footer>
    <button @click="cancel">取消</button>
    <button @click="confirm">确定</button>
  </template>
</Modal>
```

- 不带 `name` 的 `<slot />` 是**默认插槽**，父组件的剩余内容都进这里；`#title` 是 `v-slot:title` 的缩写，对应**具名插槽**。
- **作用域插槽**让子组件把内部数据"递"给插槽内容用：

```vue
<!-- ArticleList.vue（子） -->
<template>
  <ul>
    <li v-for="article in articles" :key="article.id">
      <slot :article="article" :index="index" />
    </li>
  </ul>
</template>
```

```vue
<!-- 父：v-slot 接收一个"插槽 props 对象" -->
<ArticleList :articles="list">
  <template #default="{ article }">
    <h3>{{ article.title }}</h3>
  </template>
</ArticleList>
```

作用域插槽的本质是"**内容由父组件定，数据由子组件给**"，渲染列表、表格、树这类通用组件时几乎必用。

### 5.4.6 依赖注入：provide / inject

当数据需要跨越很多层传递（比如主题配置从根组件传到深层叶子），逐层 props 会产生"props 钻透"（prop drilling）。`provide/inject` 让祖先直接供给、后代直接取用：

```vue
<!-- 祖先组件 -->
<script setup lang="ts">
import { provide, ref, type InjectionKey } from 'vue'

interface ThemeContext {
  isDark: Ref<boolean>
  toggle: () => void
}

// 用 InjectionKey 获得 / inject 两侧的类型安全
const ThemeKey: InjectionKey<ThemeContext> = Symbol('theme')

const isDark = ref(false)
provide(ThemeKey, { isDark, toggle: () => (isDark.value = !isDark.value) })
</script>
```

```vue
<!-- 任意深度的后代组件 -->
<script setup lang="ts">
import { inject } from 'vue'
const theme = inject(ThemeKey)
// theme?.toggle()
</script>
```

注意两点：注入值最好保持响应式（传 ref 或 reactive）；`inject` 可以带默认值 `inject(ThemeKey, defaultValue)` 防止忘记 provide 时拿到 `undefined`。SelfBlog 的主题、皮肤等**全局**状态走的是 Pinia（更适合全局单例），`provide/inject` 更多用在"组件树局部"的上下文（比如后台抽屉的弹层管理由 `AdminLayout` 统一用 `useDialog` 提供）。

---

## 5.5 组件通信全景

把常见通信方式放在一张表里，按"数据流向 + 适用距离"选型：

| 场景 | 方式 | 一句话要点 |
| --- | --- | --- |
| 父 → 子 | `props` | 单向数据流，子不改父 |
| 子 → 父 | `emits` 自定义事件 | `emit('update:x')` 配合 v-model 可双向 |
| 父 ↔ 子（表单类） | 组件 `v-model` | `modelValue` + `update:modelValue` |
| 父 → 子（模板注入） | 插槽 / 作用域插槽 | 内容父定、数据子给 |
| 祖先 → 深层后代 | `provide / inject` | 避免逐层传递，注意响应式 |
| 兄弟组件 | 共同父组件中转；或直接上 Pinia | 简单场景经父转发，复杂共享状态用 store |
| 任意组件 ↔ 任意组件 | **Pinia** | 全局单一数据源，见 5.7 |
| 跨页面/跨路由 | Pinia + localStorage/sessionStorage | 如 SelfBlog 的主题、token |
| 全局 API 层 | `src/api/` 模块函数 | 与组件树无关，谁需要谁调用 |

**何时该用 Pinia**，SelfBlog 给出了很好的判断标准——看状态是否需要"跨组件、跨路由、甚至跨会话共享"：

- `theme.ts`（暗色模式）：导航栏按钮和页面样式都要读 → 需要 store，且持久化到 `localStorage`。
- `user.ts`（登录态）：守卫、请求拦截器、后台页面都要读 → 必须 store。
- `pages.ts`（公开页面导航）：Navbar 和各页面都要读 → store。
- 一个按钮的 loading、一个弹窗开关 → 不需要 store，留在组件里。

反模式也要警惕：**不要把 store 当成"另一个组件"**——store 里只放状态与围绕状态的行为，不放 DOM 操作和视图细节；SelfBlog 的 `stores/music.ts` 是个例外中的例外，它是刻意设计的"唯一音频宿主"（见 5.12.4）。

---

## 5.6 Vue Router 4

### 5.6.1 安装与基本配置

```bash
npm install vue-router@4
```

```ts
// src/router/index.ts
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { setupGuards } from './guards'
import homeRoutes from './modules/home'
import authRoutes from './modules/auth'
import postRoutes from './modules/post'

const routes: RouteRecordRaw[] = [
  ...homeRoutes,
  ...authRoutes,
  ...postRoutes,
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/common/NotFound.vue'),
    meta: { title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, _from, savedPosition) {
    if (savedPosition) return savedPosition          // 浏览器前进后退：恢复原位置
    if (to.hash) return { el: to.hash, behavior: 'smooth' }  // 带锚点：平滑滚动到锚点
    return { top: 0 }                                 // 默认回顶部
  }
})

setupGuards(router)
export default router
```

两种 history 模式：`createWebHistory()` 是干净的 URL（`/post/1`），需要服务器把所有路径回退到 `index.html`（SelfBlog 后端的 `SpaController` 与服务端 SEO HTML 就承担了这个职责）；`createWebHashHistory()` 是 `/#/post/1`，部署最省事但不美观。**生产环境务必配置服务端回退**，否则刷新 `#` 后面的路径会 404。

路由组件用 `() => import(...)` 懒加载（按路由分包），SelfBlog 的 `router/helpers.ts` 提供了 `lazyLoad` 帮助函数统一这层写法。

### 5.6.2 动态路由匹配

```ts
{ path: '/post/:id', name: 'PostDetail', component: () => import('@/views/post/PostDetail.vue') }
{ path: '/tag/:id(\\d+)', name: 'TagPage', component: () => import('@/views/tag/TagPage.vue') }
{ path: '/report/:type(post|comment|user)/:id(\\d+)', name: 'Report' }
```

组件里通过 `useRoute()` 拿参数：`route.params.id`（注意它是 `string | string[]`，需要 `Number(route.params.id)` 转数字；SelfBlog 用正则约束 `:id(\d+)` 保证参数一定是数字串）。同一个组件在不同参数间跳转（`/post/1` → `/post/2`）**不会重新挂载**，组件实例被复用，需要用 `watch(() => route.params.id, ...)` 响应变化。

### 5.6.3 嵌套路由

```ts
// SelfBlog router/modules/admin.ts（节选）
export function createAdminRoutes(adminPath: string): RouteRecordRaw[] {
  const root = `/${adminPath}`
  const adminMeta = { requiresAuth: true, requiresAdmin: true, adminArea: true }

  return [
    {
      path: root,
      name: 'AdminEntry',
      component: lazyLoad(() => import('@/views/admin/AdminEntry.vue')),
      children: [
        { path: 'dashboard', name: 'AdminDashboard', component: lazyLoad(() => import('@/views/admin/Dashboard.vue')), meta: { ...adminMeta, title: '后台概览' } },
        { path: 'posts', name: 'AdminPosts', component: lazyLoad(() => import('@/views/admin/Posts.vue')), meta: { ...adminMeta, title: '文章管理' } },
        { path: 'write', name: 'AdminPostCreate', component: lazyLoad(() => import('@/views/post/PostEdit.vue')), meta: { ...adminMeta, title: '发布文章' } },
        { path: 'edit/:id', name: 'AdminPostEdit', component: lazyLoad(() => import('@/views/post/PostEdit.vue')), meta: { ...adminMeta, title: '编辑文章' } },
        { path: 'settings', name: 'AdminSettings', component: lazyLoad(() => import('@/views/admin/Settings.vue')), meta: { ...adminMeta, title: '网站设置' } }
      ]
    }
  ]
}
```

子路由渲染在父组件的 `<RouterView />` 里；父路由不写 `path` 的空字符串子路由即"默认子页"。注意 SelfBlog 的后台根路径**不是硬编码的 `/admin`**，而是服务端下发的 `adminPath` 动态拼出来的（见 5.11）。

### 5.6.4 编程式导航

```ts
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()   // 用来"跳转"
const route = useRoute()     // 用来"读当前路由信息"

router.push('/post/1')
router.push({ name: 'PostDetail', params: { id: 1 } })
router.push({ path: '/search', query: { keyword: 'vue' } })   // → /search?keyword=vue
router.replace('/login')   // 不留历史记录
router.back(); router.forward(); router.go(-2)

// 读参数
console.log(route.params.id, route.query.keyword, route.fullPath)
```

`push` 带 `query` 对象时**不要**自己拼字符串；读 `query` 时记住值可能是 `undefined`（`?keyword` 可以不带值），TypeScript 下要做空值处理。

### 5.6.5 路由元信息 meta 与导航守卫

`meta` 是每个路由可自带的任意数据，最常见用途是"这个页面需要登录吗"。SelfBlog 的守卫 `router/guards.ts` 是标准范本：

```ts
// frontend/src/router/guards.ts（节选，有删减）
export function setupGuards(router: Router): void {
  router.beforeEach(async (to, from) => {
    const userStore = useUserStore()
    const siteStore = useSiteStore()

    // 统一维护 document.title
    document.title = to.meta.title
      ? `${to.meta.title as string} - ${siteStore.settings.siteTitle}`
      : siteStore.settings.siteTitle

    // 需要登录而未登录 → 跳 adminRoot 并带上 redirect 查询参数
    if ((to.meta.requiresAuth || to.meta.requiresAdmin) && !userStore.isLoggedIn) {
      return { path: siteStore.adminRoot, query: { redirect: to.fullPath } }
    }

    // 已登录但 token 已过期（本地解析 JWT 的 exp）
    if (userStore.isLoggedIn && to.meta.requiresAuth) {
      const isValid = await userStore.validateToken()
      if (!isValid) {
        userStore.resetState()
        toast.warning('登录已过期，请重新登录')
        return { path: siteStore.adminRoot, query: { redirect: to.fullPath } }
      }
    }

    // requiresAdmin 但非管理员 → 弹回首页
    if (to.meta.requiresAdmin && userStore.isLoggedIn && !userStore.isAdmin) {
      toast.error('无管理员权限')
      return { name: 'Home' }
    }
  })

  router.onError((error) => {
    // 懒加载分包失败（发版后旧 chunk 404）→ 提示刷新
    if (error.name === 'ChunkLoadError' || error.message?.includes('Loading chunk')) {
      toast.error('页面加载失败，请刷新重试')
      router.push({ name: 'Home' })
    }
  })
}
```

要点解析：

- 守卫**返回值即导航决策**：返回 `false` 取消导航；返回路由地址对象则重定向；什么都不返回则放行。这比老式 `next()` 回调更直观。
- `meta.requiresAuth` / `requiresAdmin` 在 `router/modules/admin.ts` 里通过 `adminMeta` 对象批量铺到每个后台子路由上，守卫只读 `to.meta`，路由定义与鉴权逻辑解耦。
- `redirect` 查询参数记录"本来想去哪"，登录成功后可以跳回去——这是自研登录页最常忘的细节。
- 跳转目标写的是 `siteStore.adminRoot`（服务端下发的后台路径），**不是硬编码 `/admin`**。SelfBlog 规范明确要求"组件里需要登录时统一 `router.push('/login')`"，而 `/login` 路由本身是个重定向到 `adminRoot` 的转发器，两者配合避免任何地方写死后台路径。

守卫全家福：`beforeEach`（全局前置，最常用）、`beforeResolve`（解析完异步组件后）、`afterEach`（无 `next`，做埋点/标题）；路由独享的 `beforeEnter`；组件内的 `onBeforeRouteUpdate` / `onBeforeRouteLeave`（SelfBlog 的编辑页用 `onBeforeRouteLeave` 实现"未保存提醒"）。

### 5.6.6 动态注册路由 addRoute

路由不仅能在启动时静态声明，还能在**运行时**添加：

```ts
// frontend/src/router/index.ts（节选）
export function installAdminRoutes(adminPath: string): void {
  // 防御：如果后台路径被设成 pages，就不再占用 /pages/:slug 命名空间
  if (adminPath !== 'pages' && !router.hasRoute('ContentPage')) {
    router.addRoute({
      path: '/pages/:slug',
      name: 'ContentPage',
      component: () => import('@/views/pages/ContentPage.vue'),
      meta: { title: '内容页' }
    })
  }
  for (const route of createAdminRoutes(adminPath)) {
    router.addRoute(route)
  }
}
```

配套 API：`router.hasRoute(name)` 查存在性、`router.removeRoute(name)` 移除、`router.getRoutes()` 列出全部。**重复 addRoute 同名路由是"替换"而不是报错**，但先 `hasRoute` 判断能让意图更清晰。动态注册的典型场景：权限驱动的菜单路由、多租户路径、以及 SelfBlog 这种"后台路径由服务端配置决定"的设计——没有服务端响应之前，前端根本不知道要注册到哪个路径。

### 5.6.7 RouterLink 与活动状态

```vue
<RouterLink to="/">首页</RouterLink>
<RouterLink :to="{ name: 'PostDetail', params: { id: post.id } }">详情</RouterLink>
<!-- 激活态类：RouterLinkActive 两个类自动维护 -->
```

**测试提醒**（SelfBlog 实测结论）：单测里不要用 `stubs: { RouterLink: true }`——stub 会丢掉插槽内容，也表达不了 active 状态。应该用真实的 router：`createRouter({ history: createMemoryHistory(), routes })` 挂上后再 mount（见 5.13）。

---

## 5.7 Pinia 状态管理

### 5.7.1 为什么需要状态管理，为什么是 Pinia

当多个互不相邻的组件需要同一份数据（登录态、主题、导航配置），props/事件链会变成面条。状态管理库把这份数据放到**组件树之外的单例**里，谁都能读写，且读写过程可追踪。

Pinia 是 Vue 官方推荐（Vuex 的继任者）：API 极简（没有 mutation，action 里直接改）、完整 TypeScript 推导、天然支持 Composition API 写法、devtools 集成好、支持 SSR。SelfBlog 五个 store（site/theme/user/music/pages）全部用 Pinia 组合式写法。

### 5.7.2 defineStore 与组合式写法

```ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 组合式写法：id + setup 函数
export const useCounterStore = defineStore('counter', () => {
  // ref  → state
  const count = ref(0)
  // computed → getter
  const double = computed(() => count.value * 2)
  // 函数 → action
  function increment(): void {
    count.value++
  }
  return { count, double, increment }
})
```

对照的 options 写法（了解即可）：

```ts
export const useCounterStore = defineStore('counter', {
  state: () => ({ count: 0 }),
  getters: { double: (state) => state.count * 2 },
  actions: { increment() { this.count++ } }
})
```

组合式写法的好处：与 `<script setup>` 心智一致、TS 类型自动推导、可以在 store 里用其他 composable。唯一要求：**所有 state/getter/action 必须在 return 里返回**，漏返回就等于不存在。Pinia 要求 store id 全局唯一——SelfBlog 的 `user.ts` 里 store id 是 `'auth'`，导出的名字却是 `useUserStore`（兼容别名），这提示我们 **store id 与函数名是两回事**，devtools 里显示的是 id。

### 5.7.3 在组件里使用 store

```vue
<script setup lang="ts">
import { storeToRefs } from 'pinia'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// ❌ 直接解构 reactive 对象会丢失响应式
// const { user, isLoggedIn } = userStore

// ✅ state / getter 用 storeToRefs 解构
const { user, isLoggedIn } = storeToRefs(userStore)

// ✅ action / 普通函数直接解构没问题
const { logout } = userStore

// 模板里也可以直接 userStore.xxx 使用
</script>
```

`storeToRefs` 是初学 Pinia 必记的 API——它只把 state 和 getter 转成 ref，跳过函数。

### 5.7.4 $patch、$subscribe 与 $onAction

```ts
const userStore = useUserStore()

// $patch：批量修改，只触发一次订阅通知（比多次赋值高效）
userStore.$patch({ user: newUser, remember: true })
userStore.$patch((state) => {
  state.unreadNotifications += 3
})

// $subscribe：监听 state 变化（做日志/持久化）
userStore.$subscribe((mutation, state) => {
  localStorage.setItem('user-snapshot', JSON.stringify(state))
})

// $onAction：监听 action 调用（埋点、统一 loading）
userStore.$onAction(({ name, args, after, onError }) => {
  const start = Date.now()
  after((result) => console.log(`${name} 成功，耗时 ${Date.now() - start}ms`))
  onError((err) => console.error(`${name} 失败`, err))
})
```

### 5.7.5 在组件外使用 store

只要 **pinia 实例已被安装**（`app.use(pinia)` 之后），任何模块都可以直接调用 store。SelfBlog 的 `api/index.ts` 就在 Axios 拦截器里使用 store：

```ts
// 请求拦截器（api/index.ts 节选）
let token: string | null = null
try {
  const userStore = useUserStore()
  token = userStore.token
} catch {
  // pinia 尚未初始化时回退到 storage
  token = getToken(STORAGE_KEY_PREFIX + 'token')
}
```

两个工程细节值得学习：一是拦截器在应用启动早期可能执行，此时 pinia 还没装上，所以用 try/catch **优雅回退**；二是优先从 store 读 token（保持与 reactive 状态同步），storage 只是兜底。

如果需要在**应用启动前**（`app.use(pinia)` 之前）使用 store，可以显式传入 pinia 实例：SelfBlog 的 `main.ts` 里 `const siteStore = useSiteStore(pinia)` 就是这种写法——先 `createPinia()` 拿到实例，把它传给 store 调用，而不依赖 app 安装。

### 5.7.6 持久化

Pinia 本身不持久化，常见两条路：

1. 插件 `pinia-plugin-persistedstate`：`defineStore('id', { state, persist: true })`，配置灵活，适合大量 store。
2. **手动持久化**（SelfBlog 采用）：在需要的 action 里显式写 storage，配合编解码工具。好处是每个字段"存哪里、怎么加密、何时清"完全可控。

SelfBlog 的存储工具链：`utils/crypto.ts` 提供 `encodeData/decodeData`（对敏感值做编解码，避免 token 裸奔在 storage 里）、`safeGetItem/safeSetItem/safeRemoveItem`（封装 try/catch，隐私模式或 storage 被禁时不抛错）。`constants/index.ts` 里 `STORAGE_KEY_PREFIX = 'edu_'` 统一了所有键前缀。5.12 节会看到各 store 的具体持久化策略。

---

## 5.8 Axios 网络请求封装

### 5.8.1 基本用法

```ts
import axios from 'axios'

// GET
const res = await axios.get('/api/post/1')
// POST
await axios.post('/api/post', { title: '新文章' })
// 并发
const [a, b] = await axios.all([axios.get('/api/a'), axios.get('/api/b')])
// 取消
const controller = new AbortController()
axios.get('/api/slow', { signal: controller.signal })
controller.abort()
```

但业务项目**绝不直接用全局 axios**，而是 `axios.create()` 一个实例，统一配置并挂拦截器。

### 5.8.2 创建实例

```ts
// frontend/src/api/index.ts（节选）
const api: AxiosInstance = axios.create({
  baseURL: '/api',     // 所有请求自动带 /api 前缀
  timeout: 15000       // 15 秒超时
})
```

注意 `baseURL` 是**硬编码的 `/api`**——项目里的环境变量 `VITE_API_BASE_URL` 实际上没有任何代码消费，请求能否到达后端靠的是开发期 Vite 代理和生产期 `vercel.json` 反向代理（或同源单 JAR 部署）。这提醒我们：**配置项写了不等于生效**，审查时要追一下读取点。

### 5.8.3 响应拦截器：返回后端"信封"而非 AxiosResponse

后端统一返回 `{ code, message, data, timestamp }` 信封结构（`Result<T>`）。SelfBlog 的响应拦截器做了一件必须理解的事——**把 `response.data`（信封）直接作为拦截器返回值**：

```ts
api.interceptors.response.use(
  (response) => {
    logger.debug('Response success', { url: response.config.url, status: response.status })
    return response.data          // ← 关键：返回信封本身
  },
  async (error) => { /* 见下文 */ }
)
```

于是调用方拿到的类型要写对：

```ts
// ❌ 错误心智：以为拿到的是 AxiosResponse
const res = await api.get('/post/1')
// res.data.data  ← 多剥了一层，undefined

// ✅ 正确心智：api.get<T>() 解析出的就是 { code, message, data }
const res = await api.get<Post>('/post/1')
console.log(res.data)        // 这才是 Post 对象
console.log(res.message)     // 后端消息
```

`types/` 里的 `ApiResponse<T>` 就是对应的 TS 类型。**这一条是 SelfBlog 前端最容易踩的契约**，所有 API 模块（`api/post.ts`、`api/site.ts`……）与所有页面都遵守"拦截器已剥掉一层 HTTP 壳，`res.data` 即业务数据"的约定。

### 5.8.4 请求拦截器：注入鉴权与追踪头

```ts
api.interceptors.request.use((config) => {
  // 1) 优先从 store 读取 token（与响应式状态同步；pinia 未就绪时回退 storage）
  let token: string | null = null
  try {
    token = useUserStore().token
  } catch {
    token = getToken(STORAGE_KEY_PREFIX + 'token')   // decodeData(safeGetItem(...))
  }

  // 2) Token 指纹校验：检测 token 是否被复制到其他域使用
  if (token && !verifyTokenFingerprint(token)) {
    logger.error('Token fingerprint mismatch - possible theft attempt')
    useUserStore().resetState()
    throw new Error('Token security violation')
  }

  // 3) 注入 Authorization
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }

  // 4) CSRF 防护：X-XSRF-TOKEN（无则生成并保存）
  const xsrfToken = getXsrfToken() || generateXsrfToken()
  if (xsrfToken) config.headers['X-XSRF-TOKEN'] = xsrfToken

  // 5) 访客标识与日志追踪
  config.headers['X-Visitor-Id'] = getVisitorId()
  config.headers['X-Trace-Id'] = generateTraceId()

  return config
})
```

这一层把"每个请求都要带的东西"集中处理，业务代码从此不需要关心鉴权头。`getToken` 的实现也值得一读：`decodeData(safeGetItem(key)) || decodeData(safeGetItem(key, 'sessionStorage'))`——先试 `localStorage` 再试 `sessionStorage`，因为"记住我"勾选与否决定了 token 存在哪端。

### 5.8.5 401 与无感刷新：isRefreshing + failedQueue

Access Token 过期时（后端返回 401），最佳体验是**自动用 Refresh Token 换新 token 并重放原请求**，用户毫无感知。难点在于并发：一页上同时发了 5 个请求都撞上 401，不能刷新 5 次。SelfBlog 的解法是经典的"单飞（single-flight）+ 排队重放"：

```ts
// api/index.ts（节选，401 处理）
let isRefreshing = false
let failedQueue: Array<{ resolve: (token: string) => void; reject: (error: unknown) => void }> = []

// 刷新完成后，放行（或拒绝）排队中的所有请求
const processQueue = (error: unknown, token: string | null = null): void => {
  failedQueue.forEach((prom) => (error ? prom.reject(error) : prom.resolve(token!)))
  failedQueue = []
}

if (error.response?.status === 401 && !originalRequest._retry) {
  // 刷新接口自身 401 → refreshToken 也失效，直接登出
  if (originalRequest.url === '/user/refresh') {
    await handleAuthError()
    return Promise.reject(error)
  }

  // 已有刷新在进行 → 后来的 401 请求入队等待新 token
  if (isRefreshing) {
    return new Promise<string>((resolve, reject) => {
      failedQueue.push({ resolve, reject })
    }).then((token) => {
      originalRequest.headers.Authorization = `Bearer ${token}`
      return api(originalRequest)      // 用新 token 重放
    })
  }

  originalRequest._retry = true        // 防止重放再次 401 时死循环
  isRefreshing = true
  try {
    const refreshTokenValue = useUserStore().refreshToken ?? getToken(STORAGE_KEY_PREFIX + 'refreshToken')
    if (!refreshTokenValue) throw new Error('No refresh token')

    const response = await api.post('/user/refresh', null, {
      headers: { Authorization: `Bearer ${refreshTokenValue}` }
    })
    const { token: newToken, refreshToken: newRefreshToken } =
      (response as unknown as ApiResponse<{ token: string; refreshToken: string }>).data ?? {}

    // 双写：storage（编解码）+ store（响应式），并更新指纹
    const storage = useUserStore().remember ? localStorage : sessionStorage
    storage.setItem(STORAGE_KEY_PREFIX + 'token', encodeData(newToken)!)
    storage.setItem(STORAGE_KEY_PREFIX + 'refreshToken', encodeData(newRefreshToken)!)
    useUserStore().updateTokens(newToken, newRefreshToken)

    processQueue(null, newToken)                 // 放行队列
    originalRequest.headers.Authorization = `Bearer ${newToken}`
    return api(originalRequest)                  // 重放当前请求
  } catch (refreshError) {
    processQueue(refreshError, null)             // 队列全体失败
    await handleAuthError()
    return Promise.reject(refreshError)
  } finally {
    isRefreshing = false                         // 无论如何复位闸门
  }
}
```

理解这段代码的四个支点：

1. `_retry` 标记防止"重放又 401 → 再刷新"的无限循环。
2. `isRefreshing` 保证同一时刻只有一个刷新请求在飞。
3. `failedQueue` 把并发撞上 401 的请求挂起成 Promise，刷新成功后统一拿到新 token 重放；失败则统一 reject。
4. `finally` 里复位 `isRefreshing`，否则一次异常会让后续所有 401 永远排队。

### 5.8.6 handleAuthError：清理与跳转

刷新彻底失败（refreshToken 也无效）时，需要"登出"并送用户回登录页：

```ts
async function handleAuthError(): Promise<void> {
  // 1) 清空两端的认证数据（localStorage + sessionStorage）
  safeRemoveItem(STORAGE_KEY_PREFIX + 'token')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'refreshToken')
  safeRemoveItem(STORAGE_KEY_PREFIX + 'user')
  // ...sessionStorage 同理，clearSecurityStorage() 再清指纹与 xsrf

  // 2) 复位 Pinia（store 不可用时静默忽略）
  try {
    useUserStore().resetState()
  } catch { /* store may not be available */ }

  // 3) 跳回 adminRoot（动态后台路径），带 redirect 参数；路由不可用时整页跳转兜底
  const adminRoot = `/${(await import('@/stores/site')).useSiteStore().adminPath}`
  if (window.location.pathname !== adminRoot) {
    try {
      const { default: router } = await import('@/router')
      const redirectPath = window.location.pathname + window.location.search
      router.push({ path: adminRoot, query: { redirect: redirectPath !== adminRoot ? redirectPath : undefined } })
    } catch {
      window.location.href = adminRoot
    }
  }
}
```

注意 `await import(...)` 动态导入的用意：**延迟加载模块、避免循环依赖**（api ↔ store ↔ router 互相引用时，顶层 import 会形成环）。

### 5.8.7 网络错误重试与统一错误文案

响应拦截器还承担两类杂务：

- **网络层重试**：对"无响应"类错误（`Network Error`、`ECONNABORTED` 超时等，`RETRYABLE_CODES` 白名单 + 取消除外）自动重试，`MAX_RETRIES = 2` 次，退避间隔 `RETRY_DELAY * attempt`（1000ms × 次数）。重试计数挂在 `config._networkRetryCount` 上。**只有幂等的读请求适合自动重试**——SelfBlog 网络层重试针对的是无响应错误，写操作（POST 提交）在后端幂等设计前提下才安全，理解这一边界很重要。
- **`error.userMessage` 文案**：按 HTTP 状态映射中文提示（400「请求参数错误」、429「操作过于频繁」、403「没有访问权限」、404「请求的资源不存在」、500「服务器内部错误」），并优先采用后端 `response.data.message`。组件层拿到错误直接 `toast.error(err.userMessage)`，不必各自写一遍文案映射。

---

## 5.9 自定义指令与组合式函数

### 5.9.1 自定义指令 directives

当你要"直接操作 DOM 元素"（聚焦、滚动显隐、拖拽），指令比组件更贴切。指令是一个带生命周期钩子的对象：

```ts
import type { Directive } from 'vue'

interface RevealOptions { threshold?: number; once?: boolean }

const vScrollReveal: Directive<HTMLElement, RevealOptions | undefined> = {
  mounted(el, binding) {
    // el：绑定元素；binding.value：传给指令的值；binding.modifiers：修饰符
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          el.classList.add('visible')
          if (binding.value?.once !== false) observer.unobserve(el)
        }
      })
    }, { threshold: binding.value?.threshold ?? 0.1 })
    observer.observe(el)
  }
}
```

```vue
<template>
  <div v-scroll-reveal class="fade-in-up">滚动到可视区时浮现</div>
  <div v-scroll-reveal="{ threshold: 0.3 }">自定义阈值</div>
</template>
```

全局注册（SelfBlog 在 `main.ts` 中）：

```ts
import { vScrollReveal } from './composables/useScrollReveal'
app.directive('scroll-reveal', vScrollReveal)
```

`<script setup>` 里声明 `const vFocus = {...}` 则自动注册为局部指令 `v-focus`（变量名前缀 `v` 即指令名）。常用钩子：`created`、`beforeMount`、`mounted`、`beforeUpdate`、`updated`、`beforeUnmount`、`unmounted`——与组件生命周期同名但作用于"被绑定元素"。

### 5.9.2 组合式函数 composable

**组合式函数**是"用了 Composition API 的可复用函数"，约定以 `use` 开头。它是指令之外的另一种复用维度：复用的是**有状态的逻辑**（响应式数据 + 行为），而不是 DOM 操作。

```ts
// composables/useScrollState.ts（示意）
import { ref, onMounted, onBeforeUnmount } from 'vue'

export function useScrollState() {
  const scrolled = ref(false)
  const onScroll = () => (scrolled.value = window.scrollY > 10)
  onMounted(() => window.addEventListener('scroll', onScroll, { passive: true }))
  onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
  return { scrolled }
}
```

使用：

```vue
<script setup lang="ts">
const { scrolled } = useScrollState()
</script>
<template>
  <nav :class="{ 'nav--scrolled': scrolled }">…</nav>
</template>
```

SelfBlog 的 `composables/` 目录是一套很好的教科书：`useSeoMeta` / `usePageSeo`（页面 SEO 元信息）、`useMarkdownEnhance`（代码复制/折叠增强）、`usePageConfig`（逐页配置消费）、`useDialog`（弹层焦点、Esc 与滚动锁）、`useScrollReveal`（同时导出 composable 版本和指令版本 `vScrollReveal`）。

组合式函数三条纪律：

1. **在 `setup`/`<script setup>` 中同步调用**（内部要注册生命周期钩子）；
2. **输入输出尽量响应式**（参数收 `MaybeRef`，返回 ref/computed）；
3. **自清理**：内部注册的全局监听/Observer/定时器必须在 `onBeforeUnmount`/`onUnmounted` 里撤销，否则组件卸载后变成幽灵回调。

### 5.9.3 案例：useConfirm 的"函数式确认框"

SelfBlog 的 `composables/useConfirm.ts` 展示了一个精巧模式——**用 composable 返回"行为 + 渲染件"**：

```ts
export function useConfirm() {
  const isOpen = ref(false)
  const msg = ref('')
  const dialogTitle = ref('')
  let resolver: ((value: boolean) => void) | null = null

  onBeforeUnmount(() => {           // 组件卸载时未决的 Promise 兑现为 false
    resolver?.(false)
    resolver = null
  })

  const confirm = (message: string, title = '确认操作'): Promise<boolean> => {
    resolver?.(false)               // 已有未决确认 → 先按取消结算
    msg.value = message
    dialogTitle.value = title
    isOpen.value = true
    return new Promise<boolean>((resolve) => (resolver = resolve))
  }

  const ConfirmDialog = defineComponent({ /* 渲染 Modal + 确认/取消按钮，
      点击后 isOpen=false 并 resolver(true/false) */ })

  return { confirm, ConfirmDialog }
}
```

使用方式：

```vue
<script setup lang="ts">
import { useConfirm } from '@/composables/useConfirm'
const { confirm, ConfirmDialog } = useConfirm()

async function removePost(): Promise<void> {
  const ok = await confirm('确定删除这篇文章吗？')
  if (ok) await postApi.remove(id)
}
</script>
<template>
  <button @click="removePost">删除</button>
  <!-- 必须渲染到模板里，否则确认框不出现 —— SelfBlog 文档特别强调 -->
  <ConfirmDialog />
</template>
```

它把"命令式调用（`await confirm()`）"与"声明式渲染（`<ConfirmDialog />`）"缝合起来：调用侧代码干净得像原生 `window.confirm`，渲染侧又完全可控。注意卸载钩子里的 `resolver(false)`——组件被销毁时不能留下永远 pending 的 Promise，否则 `await` 之后的代码永远不会执行，这是异步生命周期管理的细节功力。

---

## 5.10 Vue 3 性能与进阶

### 5.10.1 v-memo：模板级记忆化

```vue
<!-- 仅当 item.id、item.text、selectedId 任一变化时才重新 diff 这个子树 -->
<div v-for="item in list" :key="item.id" v-memo="[item.id, item.text, item.id === selectedId]">
  <p>{{ item.text }}</p>
  <span>{{ item.id === selectedId ? '已选中' : '' }}</span>
</div>
```

`v-memo` 缓存一段模板的虚拟 DOM，依赖数组不变时整段跳过 diff。适合"超长列表中的少数项频繁变化"的场景。注意：**依赖数组必须覆盖子树里用到的所有响应式数据**，漏一个就会出现"界面不更新"的诡异 bug——它是高阶优化，默认不要到处加。

### 5.10.2 shallowRef 与 markRaw

Vue 3 的深度响应式有成本：大对象、第三方实例被 Proxy 包裹既慢又可能出问题。

```ts
import { shallowRef, markRaw, triggerRef } from 'vue'

// shallowRef：只有 .value 的【整体替换】触发更新，内部属性不追踪
const bigList = shallowRef<HugeItem[]>([])
bigList.value = [...bigList.value, item]   // ✅ 整体替换触发更新
bigList.value.push(item)                    // ❌ 不触发更新（内部未代理）
triggerRef(bigList)                         // 手动强制触发

// markRaw：标记一个对象永不转为响应式（放图表实例、地图实例等）
import * as echarts from 'echarts'
const chart = markRaw(echarts.init(el))
```

选择原则：数据量大且总是整体替换 → `shallowRef`；存放带复杂内部状态的外部实例（Chart、Editor、Audio）→ `markRaw`。SelfBlog 的 `stores/music.ts` 中 `let audio: HTMLAudioElement | null` 干脆用了**普通模块级变量**而非 ref——因为 audio 元素是外部宿主对象，其状态通过手动维护的 `playing/loading/currentTime` 等 ref 投影给 UI，这是"哪些该响应式、哪些不该"的清醒示范。

### 5.10.3 defineAsyncComponent 与路由懒加载

```ts
import { defineAsyncComponent } from 'vue'

const HeavyEditor = defineAsyncComponent({
  loader: () => import('@/components/admin/HeavyEditor.vue'),
  loadingComponent: Spinner,       // 加载中占位
  errorComponent: ErrorBox,        // 加载失败占位
  delay: 200,                      // 超过 200ms 才显示 loading（防闪烁）
  timeout: 30000                   // 超时视为失败
})
```

路由组件天然支持懒加载（`() => import(...)`），Vite 会自动按路由切分 chunk。SelfBlog 的 `vite.config.ts` 还用 `manualChunks` 手动分了组：`vendor: ['vue', 'vue-router', 'pinia']` 与 `markdown: ['marked', 'dompurify']`——把变更频率低的框架代码与 Markdown 库分离开，利于浏览器长期缓存。

### 5.10.4 Suspense：等待异步依赖

```vue
<template>
  <Suspense>
    <template #default>
      <Dashboard />   <!-- setup 是 async 的组件：内部 await 数据 -->
    </template>
    <template #fallback>
      <p>加载中…</p>
    </template>
  </Suspense>
</template>
```

`<script setup>` 可以写 `await`（组件变成异步组件），`Suspense` 负责在其就绪前展示 fallback。Suspense 仍属实验特性，生产使用要保持关注版本行为；SelfBlog 未使用它，而是采用"先挂载、数据在 `onMounted`/store action 里异步拉取"的模式，配合骨架屏，对初学者更直白。

### 5.10.5 Teleport：渲染到组件树之外

```vue
<template>
  <!-- 弹层 DOM 传送到 #overlay-root，但逻辑仍在本组件 -->
  <Teleport to="#overlay-root">
    <div v-if="show" class="modal">…</div>
  </Teleport>
</template>
```

价值：弹层样式不再被父级 `overflow: hidden`、`transform`、`z-index` 层叠上下文困住。SelfBlog 特别约定：模态层挂到主题根内的 `#overlay-root`，**不要传送到裸 `body`**——这样弹层仍在主题与皮肤的作用域内（`#selfblog[data-skin=…]` 的变量对它生效），暗色模式也不需要重复适配。

### 5.10.6 keep-alive：组件状态保鲜

```vue
<template>
  <RouterView v-slot="{ Component }">
    <KeepAlive :include="['PostList']" :max="10">
      <component :is="Component" />
    </KeepAlive>
  </RouterView>
</template>
```

被缓存的组件切换时不销毁，状态（滚动位置、表单草稿）原地保留；对应生命周期 `onActivated`/`onDeactivated` 取代 `onMounted`/`onUnmounted` 的部分职责（回到页面时刷新数据放在 `onActivated`）。`include/exclude` 按组件名过滤，`max` 限制缓存数量防内存膨胀。注意：一旦被缓存，`onUnmounted` 里的清理不会在切换时执行——全局监听若注册在 `onMounted`，对称的移除要考虑 `onDeactivated`。

### 5.10.7 过渡与动画：Transition 与 TransitionGroup

Vue 内置了两个过渡组件，用**声明式**的方式给元素和列表加动画，不需要手写监听器：

```vue
<template>
  <!-- 单元素/组件过渡：v-if、v-show 切换时自动应用过渡类 -->
  <Transition name="fade">
    <p v-if="visible">淡入淡出</p>
  </Transition>

  <!-- 列表过渡：配合 v-for，移除项有离场动画 -->
  <TransitionGroup name="list" tag="ul">
    <li v-for="item in items" :key="item.id">{{ item.text }}</li>
  </TransitionGroup>
</template>

<style scoped>
/* name="fade" 对应 fade-enter-* / fade-leave-* 六个类 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 列表：入场、离场、移动三种类 */
.list-enter-active {
  transition: all 0.3s ease;
}
.list-leave-active {
  transition: all 0.2s ease;
  position: absolute; /* 离场项脱离文档流，其余项才能平滑移动 */
}
.list-enter-from,
.list-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
.list-move {
  transition: transform 0.3s ease; /* 其余项"让位"的移动动画 */
}
</style>
```

工作原理：`Transition` 在元素插入/移除的时机自动追加 `v-enter-from → v-enter-active → v-enter-to`（入场）与 `v-leave-from → v-leave-active → v-leave-to`（离场）六个类，动画完成后移除。常见坑位：

1. **`TransitionGroup` 的子项必须有 `key`**，否则动画与复用都会错乱；
2. 离场动画要求元素"还占着位置"才能渐隐，配合 `position: absolute` 让其余项用 `.list-move` 平滑补位；
3. `mode="out-in"` / `mode="in-out"` 控制新旧元素交替顺序，路由过渡 `<RouterView v-slot="{ Component }"><Transition mode="out-in"><component :is="Component" /></Transition></RouterView>` 最常用 `out-in`，避免两个页面重叠闪跳；
4. 用 CSS `animation` 时写 `*-enter-active` 的 keyframes 即可；用 JS 钩子（`@before-enter`/`@enter`/`@leave`）可以做复杂序列动画，但别忘了在 `@enter`/`@leave` 里调用 `done()`。

SelfBlog 没有引入动画库，而是把常用动效沉淀成全局工具类（`.fade-in`、`.slide-up`、`.pulse`，见 `styles/`）配合 `v-scroll-reveal` 指令的 `.visible` 类切换——**CSS 过渡 + 类切换**已覆盖绝大多数场景，Transition 组件留给条件渲染和路由切换这类"元素生灭"的时机。

---

## 5.11 实战案例：剖析 SelfBlog 的启动流程

现在把所有知识串起来，逐行读 SelfBlog 的 `src/main.ts`——这个文件只有 57 行，但**每一行的顺序都是承重的**：

```ts
// frontend/src/main.ts（节选，保留顺序）
const app = createApp(App)
const pinia = createPinia()
app.use(pinia)

app.directive('scroll-reveal', vScrollReveal)          // 全局指令
app.config.errorHandler = (err, _instance, info) => {  // 全局错误兜底 → logger
  logger.error('Vue error', { error: (err as Error).message, info })
}
window.addEventListener('unhandledrejection', ...)     // 全局 Promise 兜底
window.onerror = (...)

async function bootstrap() {
  const siteStore = useSiteStore(pinia)        // ① 显式传 pinia：此时还没 app.mount
  await siteStore.loadPublicSettings()         // ② 等待站点设置（含 adminPath）
  installAdminRoutes(siteStore.adminPath)      // ③ 按服务端下发的路径注册后台路由

  useUserStore(pinia).restoreFromStorage()     // ④ 恢复登录态（必须在 use(router) 之前！）
  app.use(router)                              // ⑤ 安装路由 → 触发首次导航与 beforeEach
  app.mount('#app')                            // ⑥ 挂载

  void usePagesStore(pinia).loadManifest()     // ⑦ 可选的页面配置，异步、不阻塞
  logger.info('Application initialized', ...)
}

void bootstrap().catch((error) => {
  logger.error('Application bootstrap failed', { error: (error as Error).message })
  app.mount('#app')                            // 引导失败也要挂载：宁可显示默认站点
})
```

**为什么顺序不能乱？逐条拆解：**

1. **② 必须在 ③ 之前**：`loadPublicSettings()` 用 `fetch('/api/site/public')` 拉取站点设置，其中 `adminPath` 决定后台入口路径（默认 `admin`，站点设置页可改）。没拿到它就不知道要把后台路由注册到哪里。
2. **③ 必须在 ⑤ 之前**：后台路由**不在静态路由表里**，而是 `installAdminRoutes` 里循环 `router.addRoute(...)` 动态注册的。如果先 `app.use(router)`，路由器会立即执行**首次导航**——此时后台路由还不存在，作者直接访问 `/admin/dashboard` 就会 404。这就是"运行时动态注册路由"的实战代价：**addRoute 必须发生在首次导航前**。
3. **④ 必须在 ⑤ 之前**：`restoreFromStorage()` 从 localStorage/sessionStorage 解码出 token 与用户对象写进 Pinia。若在 `use(router)` 之后才恢复，首次导航执行 `beforeEach` 时 `userStore.isLoggedIn` 还是 `false`，守卫会把**其实已经登录的作者**重定向回后台入口（登录页）——"每次刷新都弹回登录页"这类经典 bug 的根源就在这个顺序。
4. **⑦ 放在 mount 之后且用 `void` 不 await**：页面配置（自定义导航、内容页）是**可选增强**，拉取失败时站点照常可用（`pages.ts` 的 `loadManifest` 内部 catch 吞掉错误）。它不该阻塞登录态恢复与首屏渲染；`void` 前缀则显式表达"我故意不等待这个 Promise"。
5. **bootstrap 的 catch 里兜底 mount**：即使站点设置拉取流程抛出意外，也要把 App 挂上去——SelfBlog 的 store 全部带默认值（`site.ts` 的 `defaults` 对象），断网时用默认站点标题与 `/admin` 后台路径照常渲染。

再看 `loadPublicSettings` 内部的健壮性处理，同样值得抄进自己的项目：

```ts
async function loadPublicSettings(): Promise<void> {
  try {
    const response = await fetch('/api/site/public', { headers: { Accept: 'application/json' } })
    if (!response.ok) return                      // 非 2xx：静默保留本地默认值
    const envelope = (await response.json()) as { data?: Partial<SiteSettings> }
    if (envelope.data) applySettings(envelope.data)   // 应用设置 + 同步 title/favicon/皮肤
  } catch {
    // Keep the local defaults available when the API is temporarily offline.
  }
}
```

启动期请求的特点是"失败不许炸启动"，所以这里用的是原生 `fetch` 而不是带拦截器的 `api` 实例——避免 401 刷新队列等机制在应用尚未就绪时被牵连。

### 5.11.1 启动顺序的"反证实验"：打乱会发生什么

理解顺序最有效的方法是推演"打乱后会坏"。把四种典型错误排列逐一想一遍：

```text
错误排列 A：先 app.use(router) 再 loadPublicSettings()
→ 首次导航发生时 adminPath 还是默认值。假设服务端配置的后台路径是 "console"，
  作者收藏夹里的 /console/dashboard 在首次导航时没有对应路由记录 → 404。
  随后 installAdminRoutes 把后台路由注册到了默认 /admin 下，
  与服务端期望彻底错位，刷新也无法自愈。

错误排列 B：先 app.use(router) 再 restoreFromStorage()
→ 首次 beforeEach 执行时 userStore.token 还是 null，isLoggedIn === false。
  守卫把 /admin/dashboard 重定向到 adminRoot 并附上 redirect 参数。
  紧接着 restoreFromStorage() 恢复了登录态——但导航已经判过了，
  作者被弹到后台入口页。这就是"每次刷新都要求重新登录"的经典 bug，
  而且很难排查：因为单独看守卫和 store 都"没有错"。

错误排列 C：await loadManifest() 放在 mount 之前
→ 页面配置接口慢（或后端恰好重启）时，整个应用白屏等它。
  它明明是"可选增强"，却拿到了"阻塞主流程"的权力。
  正确做法是 mount 之后 void usePagesStore(pinia).loadManifest()。

错误排列 D：bootstrap 里不 await loadPublicSettings()，直接继续
→ installAdminRoutes(siteStore.adminPath) 拿到的是默认 'admin'，
  等价于排列 A。注意这里"await 了"与"没 await 但代码顺序对"是两回事——
  JS 的 async 函数里，忘写 await 的调用会立刻返回 Promise，
  后续语句照常执行，顺序在物理上就乱了。
```

还有一个隐蔽的时序点：`bootstrap().catch(...)` 里的兜底 `app.mount('#app')`。如果 `loadPublicSettings` 或路由注册抛出意外（比如 fetch 实现异常、`crypto` 不可用），不兜底的话用户看到的就是**永久白屏**；兜底后至少默认站点能渲染。这体现了一条启动期工程原则：**启动链路上的每一环都应该有"降级但仍可用"的出口**——store 默认值、`!response.ok` 静默返回、catch 吞错，三层防线缺一不可。

最后补充一个初学者常问的问题：为什么 `useSiteStore(pinia)` 要把 pinia 当参数传？因为 store 的内部实现依赖"当前激活的 Pinia 实例"。在组件里调用时，Pinia 能从组件实例上自动推断出这个实例；但在 `bootstrap` 这种**组件树之外**的代码里，只能显式传入。`app.use(pinia)` 之后全局有一个"活动实例"，普通模块代码就可以不带参数调用了——`api/index.ts` 的拦截器里 `useUserStore()` 不传参，正是依赖了这一机制（并以 try/catch 兜住"pinia 还没装"的极端时序）。

---

## 5.12 实战案例：SelfBlog 的五个 Pinia Store

### 5.12.1 site.ts：站点设置与后台路径

```ts
export const useSiteStore = defineStore('site', () => {
  const settings = ref<SiteSettings>({ ...defaults })
  const adminPath = computed(() => settings.value.adminPath || 'admin')
  const adminRoot = computed(() => `/${adminPath.value}`)
  const isCommentsEnabled = computed(() => settings.value.commentsEnabled === 'true')
  const isMusicEnabled = computed(() => settings.value.musicEnabled === 'true' && !!settings.value.musicUrl)

  function applySettings(input: Partial<SiteSettings> | Record<string, string>): void {
    settings.value = { ...settings.value, ...input }
    applyAppearance()      // 同步皮肤 data-skin 与 --site-primary 变量到 #selfblog
    document.title = settings.value.siteTitle || defaults.siteTitle
    applyFavicon(settings.value.faviconUrl || '')   // 幂等注入/清理 <link rel=icon>
  }
  // loadPublicSettings 见 5.11
})
```

看点：**KV 型站点设置零迁移**——后端 `site_setting` 是 KV 表，前端用 `Partial<SiteSettings>` 合并，新增设置键不需要动数据结构；`adminPath/adminRoot` 用 computed 派生并处处复用（守卫、API 层、`/login` 重定向全都读它），保证"后台路径"只有一个事实来源。`isMusicEnabled` 这类布尔派生把 `'true'`/`'false'` **字符串**转成真布尔——KV 表存的都是字符串，转换收敛在 store 一处。

### 5.12.2 theme.ts：暗色模式与系统偏好

```ts
type ThemeMode = 'light' | 'dark' | 'system'

export const useThemeStore = defineStore('theme', () => {
  const isDarkMode = ref(false)
  const theme = ref<ThemeMode>('light')
  let mediaQuery: MediaQueryList | null = null
  let mediaHandler: (() => void) | null = null

  const currentTheme = computed<'light' | 'dark'>(() => {
    if (theme.value === 'system') {
      return (mediaQuery || window.matchMedia('(prefers-color-scheme: dark)')).matches
        ? 'dark' : 'light'
    }
    return theme.value
  })

  function initTheme(): void {
    let savedTheme: ThemeMode = 'light'
    try {
      savedTheme = (localStorage.getItem(STORAGE_KEY_PREFIX + 'theme') as ThemeMode) || 'light'
    } catch { /* storage unavailable */ }
    theme.value = savedTheme
    destroyTheme()                 // 先清理旧监听，防重复注册
    applyTheme()
    mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
    mediaHandler = () => { if (theme.value === 'system') applyTheme() }
    mediaQuery.addEventListener('change', mediaHandler)
  }

  function applyTheme(): void {
    isDarkMode.value = currentTheme.value === 'dark'
    document.documentElement.classList.toggle('dark-mode', isDarkMode.value)
  }

  function toggleTheme(): void {   // light → dark → system 三态循环
    theme.value = theme.value === 'light' ? 'dark' : theme.value === 'dark' ? 'system' : 'light'
    try { localStorage.setItem(STORAGE_KEY_PREFIX + 'theme', theme.value) } catch {}
    applyTheme()
  }
})
```

要点：持久化键是 `edu_theme`（`STORAGE_KEY_PREFIX + 'theme'`）；`system` 模式靠 `matchMedia('(prefers-color-scheme: dark)')` 的 `change` 监听跟随系统；暗色通过在 `<html>` 上挂 `.dark-mode` 类生效（`dark-mode.css` 会整体反转灰度梯度）。所有 storage 访问都包 try/catch——隐私模式下 `localStorage` 会抛异常，**偏好设置绝不能让应用崩溃**。`initTheme` 先调 `destroyTheme()` 清旧监听再注册新的，避免重复初始化造成监听器叠加。

### 5.12.3 user.ts：认证状态（store id 是 'auth'）

```ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const user = ref<User | null>(null)
  const remember = ref(false)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  function saveToStorage(): void {
    const storage = remember.value ? localStorage : sessionStorage
    const otherStorage = remember.value ? sessionStorage : localStorage
    // 清掉另一端的旧数据，防止切换"记住我"后残留旧 token
    otherStorage.removeItem(STORAGE_KEY_PREFIX + 'token')
    /* ... */
    storage.setItem(STORAGE_KEY_PREFIX + 'token', encodeData(token.value)!)
  }

  function restoreFromStorage(): void { /* 解码并恢复，失败则 clearStorage() */ }

  async function validateToken(): Promise<boolean> {
    // 只解析 JWT 的 payload.exp 做本地过期判断，不发网络请求
    const parts = token.value!.split('.')
    const payload = JSON.parse(atob(parts[1]!.replace(/-/g, '+').replace(/_/g, '/')))
    return !(payload.exp && payload.exp * 1000 < Date.now())
  }
  /* login / logout / updateTokens / detectTokenTheft ... */
})

// 兼容别名：全站引用的都是这个名字
export const useUserStore = useAuthStore
```

四个设计决策值得咀嚼：

1. **store id 是 `'auth'`，对外别名 `useUserStore`**——再次印证 id 与导出名是两回事；改 id 会让 devtools 里历史记录"搬家"，用别名保持 API 稳定。
2. **token 存取经过 `utils/crypto` 的 `encodeData/decodeData`**，不裸奔在 storage；"记住我"决定存 localStorage（跨会话）还是 sessionStorage（关标签即失效），并且两端互斥清理。
3. **`validateToken` 是纯本地校验**（base64 解 payload 看 `exp`），不请求网络——守卫里用它做"秒级过期判断"，真正的服务端鉴权交给每个请求的 401 → 刷新链路。剩余寿命小于 5 分钟时仅打日志"下次请求会刷新"。
4. **`detectTokenTheft` 与 API 层的指纹校验呼应**：storage 里的 token 与内存不一致即判定可能被篡改，直接重置状态。

### 5.12.4 music.ts：唯一音频宿主

```ts
export const useMusicStore = defineStore('music', () => {
  const source = ref(''); const title = ref('背景音乐')
  const playing = ref(false); const loading = ref(false); const error = ref('')
  const currentTime = ref(0); const duration = ref(0)
  const volume = ref(0.65); const loop = ref(true)
  let audio: HTMLAudioElement | null = null   // ← 外部宿主：不是 ref，markRaw 都不用

  function attach(element: HTMLAudioElement | null): void {
    // 把 <audio> 元素接到 store：注册 guarded 监听器（playing/pause/ended/
    // waiting/loadedmetadata/timeupdate/error），把媒体事件翻译成上面的 ref 状态
  }

  function configure(configuration: MusicConfiguration): void {
    const next = configuration.enabled && /^(https?:\/\/|\/uploads\/)/i.test(url) ? url : ''
    if (next === source.value) return          // 换页/重复下发不重置播放器
    attach(null); source.value = next
  }

  function setSuspended(value: boolean): void {
    if (suspended === value) return
    suspended = value
    if (value) pause()                          // 进入后台 → 暂停
  }
  /* play/pause/seek/setVolume/setLoop ... */
})
```

这是"store 管理外部资源"的高级范例：全站**只有一个 `<audio>` 元素**（常驻 `App.vue` 的 headless `MusicPlayer` 组件），Navbar 的播放控件、后台的启停设置都只是读写这个 store，不存在第二个音频实例打架；换路由时音乐连续播放（公开页），进入后台自动暂停（`setSuspended`）；音量/循环偏好持久化在 `selfblog-music-preferences`。它还演示了大量防御性细节：`revision` 计数让过期的 `play()` Promise 失效、`NotAllowedError`（浏览器自动播放限制）翻译成友好提示、storage 全程 try/catch。

### 5.12.5 pages.ts：只保存已发布页面配置

```ts
export const usePagesStore = defineStore('pages', () => {
  const pages = ref<Record<string, PublicPage>>({})
  const navigation = ref<PublicNavigationItem[]>([...defaultNavigation])
  let manifestRequest = 0   // 请求代际计数：只认最新一次清单请求

  async function loadManifest(): Promise<void> {
    const request = ++manifestRequest
    try {
      const response = await pageApi.getManifest()
      if (request !== manifestRequest) return   // 迟到的旧响应直接丢弃
      pages.value = Object.fromEntries(response.data.pages.map((page) => [page.id, page]))
      navigation.value = response.data.navigation
    } catch { /* 可选配置：失败保持现状 */ }
  }
  /* loadPage / loadSlug，同样带代际守卫 */
})
```

三条铁律：**只存已发布（published）配置，草稿绝不进公开 store**，也不写 localStorage（草稿只活在后台编辑器/预览上下文）；清单与单页请求都带**代际计数防竞态**——先发出的慢请求不能覆盖后发出的新数据；`defaultNavigation` 兜底，后端没配置导航时导航栏照样渲染"首页/归档/相册/关于"。

---

## 5.13 测试 Vue 组件（Vitest + @vue/test-utils）

SelfBlog 前端用 **Vitest 4** + **@vue/test-utils 2** + jsdom，维护着 **43 个测试文件 / 400 个测试**的基线。Vitest 与 Vite 共享同一套配置与转换管线，`vite.config.ts` 里的 `test` 字段就是它的配置：

```ts
// vite.config.ts（节选）
test: {
  globals: true,                        // describe/it/expect 不用 import
  environment: 'jsdom',                 // 模拟浏览器 DOM
  setupFiles: ['src/test/setup.ts'],    // 每个测试文件前执行
  include: ['src/**/*.{test,spec}.{js,ts,jsx,tsx}'],
  coverage: { provider: 'v8', include: ['src/**/*.{js,ts,vue}'] }
}
```

全局配置 `src/test/setup.ts` 刻意保持最小：

```ts
import '@testing-library/jest-dom'
import { config } from '@vue/test-utils'
import { vi } from 'vitest'

config.global.stubs = {}
config.global.mocks = {
  $route: { path: '/', params: {}, query: {} },
  $router: { push: vi.fn(), replace: vi.fn() }
}

// jsdom 没有 matchMedia，只桩这一个浏览器 API
Object.defineProperty(window, 'matchMedia', {
  writable: true,
  value: vi.fn().mockImplementation((query: string) => ({
    matches: false, media: query, onchange: null,
    addListener: vi.fn(), removeListener: vi.fn(),
    addEventListener: vi.fn(), removeEventListener: vi.fn(), dispatchEvent: vi.fn()
  }))
})
```

写一个组件测试（`BackToTop.test.ts` 是仓库里的真实例子）：

```ts
import { afterEach, describe, expect, it, vi } from 'vitest'
import { enableAutoUnmount, mount } from '@vue/test-utils'
import BackToTop from '../BackToTop.vue'

enableAutoUnmount(afterEach)                 // 每个用例后自动卸载组件
afterEach(() => vi.restoreAllMocks())

describe('back to top motion', () => {
  it('reduced motion 时不用平滑滚动', async () => {
    vi.spyOn(window, 'matchMedia').mockReturnValue({ matches: true } as MediaQueryList)
    const scroll = vi.spyOn(window, 'scrollTo').mockImplementation(() => {})
    const wrapper = mount(BackToTop)
    await wrapper.get('button').trigger('click')
    expect(scroll).toHaveBeenCalledWith({ top: 0, behavior: 'auto' })
  })
})
```

从 SelfBlog 的实践中沉淀下来的四条测试军规：

1. **挂载会注册全局监听器的组件，必须自己 `enableAutoUnmount(afterEach)`**。测试环境没有自动卸载，组件不卸载，`onMounted` 里挂到 `window` 的监听器就会泄漏到后面的用例，造成"单跑通过、全跑挂掉"的幽灵失败。
2. **路由优先用真实的 `createMemoryHistory()`**，不要 `stubs: { RouterLink: true }`。stub 会丢掉 RouterLink 的插槽内容，也表达不了 active 状态；内存 history 不改地址栏，是测试里跑真路由的标准姿势：

   ```ts
   const router = createRouter({ history: createMemoryHistory(), routes })
   await router.push('/') ; await router.isReady()
   mount(App, { global: { plugins: [router] } })
   ```
3. **jsdom 缺口只桩最小集合**：目前只有 `matchMedia`（主题监听、reduced-motion 检测都依赖它）。遇到 `IntersectionObserver`、`ResizeObserver` 等再按需在用例里补，不预装大而全的 polyfill，免得掩盖真实问题。
4. **异步渲染要 await**：改状态后 `await nextTick()` 或对触发用 `await wrapper.find('button').trigger('click')`；涉及 rAF 的动画逻辑不能桩成同步，要排队后手动 flush；改 `window.scrollY` 用 `Object.defineProperty(window, 'scrollY', { value: 100, configurable: true })` 再派发 `scroll` 事件。

Store 测试更简单：Pinia store 本身就是函数集合，`setActivePinia(createPinia())` 后直接调用 `useXxxStore()` 断言状态流转，不需要挂载任何组件（`stores/__tests__/theme.test.ts`、`music.test.ts` 都是这么写的）。

再补一个真实项目里绕不开的场景——**测试会注册全局监听的 store 逻辑**（以主题监听为例）：

```ts
import { describe, it, expect, vi, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useThemeStore } from '../theme'

afterEach(() => {
  vi.restoreAllMocks()
  setActivePinia(createPinia())   // 每个用例用全新的 pinia，避免状态串场
})

describe('theme store', () => {
  it('toggleTheme 在 light → dark → system 间循环并写入 storage', () => {
    setActivePinia(createPinia())
    const theme = useThemeStore()

    const storageSpy = vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {})
    theme.toggleTheme()
    expect(theme.theme).toBe('dark')
    theme.toggleTheme()
    expect(theme.theme).toBe('system')
    expect(storageSpy).toHaveBeenCalledWith('edu_theme', 'system')
  })
})
```

这里的两个细节都来自真实教训：`vi.spyOn(Storage.prototype, 'setItem')` 要 mock 成空实现，否则 jsdom 的 storage 行为可能在不同用例间留下脏数据；`setActivePinia(createPinia())` 放进 `afterEach`，因为 Pinia 的状态是模块级单例，上一个个用例改过的登录态会让下一个个用例"凭空已登录"。断言里直接写 `'edu_theme'` 而不是引用常量，是刻意为之——**测试要钉死的是对外可见的存储键名**，将来有人改常量导致键名漂移时，测试会第一时间报警。

---

## 5.14 常见错误与最佳实践清单

**响应式陷阱**

1. JS 里忘写 `.value`：`count++` 而不是 `count.value++`，界面纹丝不动且无报错。
2. 解构 `reactive` 对象丢响应式——用 `toRefs`（store 用 `storeToRefs`）。
3. 对 `reactive` 的数组/对象属性做整体替换后丢失追踪；需要整体替换请用 `ref`。
4. `watch` 直接侦听 `reactive` 对象时新旧值是同一引用；要字段级对比用 getter 形式。
5. 大数组高频更新卡顿：考虑 `shallowRef` + 整体替换，或 `v-memo`。

**模板与组件**

6. `v-for` 用 `index` 当 key，列表重排后状态错位；用稳定业务 id。
7. `v-if` 和 `v-for` 同标签连用（Vue 3 中 `v-if` 先执行，拿不到遍历变量）；先 `<template v-for>` 再内部 `v-if`。
8. 在子组件里直接改 props；正确姿势是 emit 或 `defineModel`。
9. 事件监听/定时器/Observer 在 `onMounted` 注册却不在 `onBeforeUnmount` 移除——内存泄漏。
10. `v-html` 渲染用户内容不过 DOMPurify——XSS。

**路由与状态**

11. 在 `<script setup>` 里用 `this.$router`——组合式 API 里用 `useRouter()/useRoute()`（且 `route` 不是响应式解构友好的，监听参数用 `watch(() => route.params.id, ...)`）。
12. 动态 `addRoute` 发生在 `app.use(router)` 之后——首次导航 404（SelfBlog 启动顺序专门为此设计）。
13. 刷新后登录态丢失：忘了在路由安装前 `restoreFromStorage()`，首个 `beforeEach` 把人弹回登录页。
14. Pinia 解构丢响应式：`const { x } = store` 要改 `storeToRefs(store)`。
15. store id 与导出名混为一谈（SelfBlog：id `'auth'`，导出 `useUserStore`），devtools 排查时注意。

**网络层**

16. 以为 `api.get()` 返回 AxiosResponse，多剥一层 `res.data.data`——SelfBlog 拦截器已返回信封，读 `res.data` 即可。
17. 并发 401 触发多次刷新：必须 `isRefreshing` + `failedQueue` 单飞排队；`finally` 里忘了复位 `isRefreshing` 是变体 bug。
18. 刷新重放没加 `_retry` 标记，401 → 刷新 → 重放 401 死循环。
19. 启动期请求（站点设置）复用业务 axios 实例，被 401 逻辑牵连炸掉启动——启动期用裸 `fetch` + 全静默兜底。

**工程化**

20. `tsconfig` 开 `noUncheckedIndexedAccess` 后 `arr[0].foo` 过不了类型检查——用 `arr[0]?.foo`，或 `replace` 替代 `split()[0]`（SelfBlog 的 `mimeForIcon` 是范例）。
21. storage 访问裸奔不包 try/catch，隐私模式直接崩；统一走 `safeGetItem` 一类封装并加键前缀。
22. 每次改代码后不跑闸门——`lint:check`、`format:check`、`type-check`、`test:run`、`build` 五项全绿再提交，守住"43 文件 / 400 测试"基线。

**最佳实践小结**

- 组合式 API + `<script setup>` + TypeScript 为默认风格；逻辑复用优先抽 composable（`use` 前缀、自清理）。
- 状态分层：组件私有状态留在组件；跨组件/跨路由/跨会话才进 Pinia；持久化字段明确"存哪端、怎么编码、何时清"。
- 网络层集中封装：信封契约、鉴权注入、刷新单飞、错误文案统一在 `api/index.ts` 一处完成。
- 启动顺序是契约：**拉设置 → 注册动态路由 → 恢复登录态 → 装路由 → 挂载 → 异步可选配置**。
- 测试守最小桩原则：jsdom 只补真实缺口，路由用真 router，监听器组件配 `enableAutoUnmount`。

---

## 本章总结

本章以 SelfBlog 真实源码为主线，完整过了一遍 Vue 3 全家桶：

1. **语言与工具**：Vue 3 渐进式框架、Composition API 与 Options API 的取舍、Vite 的开发体验与项目结构。
2. **模板语法**：插值、`v-bind`/`v-on` 及简写、`v-if`/`v-show`、`v-for` 与 key、`v-model` 与修饰符。
3. **组合式 API**：`ref`/`reactive` 及解包规则、`toRef`/`toRefs`、`computed`、`watch`/`watchEffect`、生命周期与清理责任。
4. **组件体系**：SFC 三段结构、props 校验、emits、组件 v-model、三种插槽、provide/inject，以及一张组件通信选型表。
5. **路由与状态**：Vue Router 4 的动态/嵌套路由、编程式导航、meta 守卫与动态 `addRoute`；Pinia 的组合式 store、`$patch`/`$subscribe`、组件外使用与持久化策略。
6. **网络层**：Axios 实例与拦截器、`{code,message,data}` 信封契约、`isRefreshing + failedQueue` 无感刷新、`handleAuthError` 清理跳转。
7. **复用与性能**：自定义指令、组合式函数、`v-memo`、`shallowRef`/`markRaw`、异步组件、Suspense、Teleport、keep-alive。
8. **实战剖析**：`main.ts` 承重的启动顺序及其原因；五个 store（site/theme/user/music/pages）各自的设计决策。
9. **质量保障**：Vitest + @vue/test-utils 的四条军规，以及 22 条常见错误清单。

下一章我们将继续深入前端工程化：构建优化、部署契约与 CI 闸门。

---

## 5.15 进阶专题：组合式函数（Composables）的设计模式

5.9 节入门了组合式函数，这一节把它讲成**体系**——这是 Vue 3 代码复用的核心形态，也是面试高频考点。

### 5.15.1 为什么是组合式函数：从 mixin 的失败说起

Vue 2 的 mixin 有三个致命伤：**来源不明**（组件里的 `this.loading` 从哪个 mixin 来的？）、**命名冲突**（两个 mixin 都有 `fetchData`）、**隐式耦合**（mixin 依赖组件里的某个 data，删了组件字段 mixin 就静默坏掉）。组合式函数把这三个问题全部消灭：

```typescript
// composables/useLike.ts——来源显式（import 进来的）、命名空间隔离（返回值解构改名）、
// 依赖显式（参数列表就是全部依赖）
export function useLike(postId: Ref<number>) {
  const liked = ref(false)
  const likeCount = ref(0)
  const submitting = ref(false)

  async function toggle() {
    if (submitting.value) return
    submitting.value = true
    const prev = liked.value
    // 乐观更新：先改 UI，失败回滚（SelfBlog LikeButton 的核心逻辑）
    liked.value = !liked.value
    likeCount.value += prev ? -1 : 1
    try {
      const res = await api.post(`/anonymous-likes/posts/${postId.value}`)
      liked.value = res.data.liked            // 服务端校准
      likeCount.value = res.data.likeCount
    } catch {
      liked.value = prev                       // 回滚
      likeCount.value += prev ? 1 : -1
    } finally {
      submitting.value = false
    }
  }

  return { liked, likeCount, submitting, toggle }
}
```

组件里三行接入：`const { liked, likeCount, toggle } = useLike(toRef(props, 'id'))`——**逻辑与 UI 分离**，点赞逻辑可以脱离组件单测。

### 5.15.2 设计规范五条（Vue 官方约定 + SelfBlog 实践）

1. **命名 `useXxx`**：约定即文档，看到 use 前缀就知道返回的是响应式状态 + 操作函数
2. **入参支持 ref 与裸值**：用 `toValue()`（Vue 3.3+）统一解包，`useLike(toRef(props,'id'))` 与 `useLike(123)` 都能用
3. **返回普通对象而非 reactive**：调用方解构不丢响应性（`useXxx()` 返回 reactive 对象再解构就全断了）；配合 `storeToRefs` 的原理一脉相承
4. **副作用必须在 onUnmounted 清理**：事件监听、定时器、EventSource——组合式函数里注册的，也要在组合式函数里卸载
5. **异步状态三件套齐备**：`data / loading / error` 缺一不可，调用方不用自己包 try-catch

### 5.15.3 输入输出范式：useXxx 的四种形状

```typescript
// 形状一：状态 + 操作（useLike）——交互逻辑
// 形状二：数据获取（useXxxQuery）——loading/error/data 三件套
export function usePostList(pageNum: Ref<number>) {
  const { data, loading, error } = useAsyncData(
    () => api.getPosts(pageNum.value),
    { watch: [pageNum] }               // pageNum 变化自动重新拉取
  )
  return { records: computed(() => data.value?.records ?? []), loading, error }
}

// 形状三：浏览器 API 封装（useDarkMode / useMediaQuery）——与环境打交道
export function useDarkMode() {
  const isDark = ref(matchMedia('(prefers-color-scheme: dark)').matches)
  const mq = matchMedia('(prefers-color-scheme: dark)')
  const handler = (e: MediaQueryListEvent) => { isDark.value = e.matches }
  onMounted(() => mq.addEventListener('change', handler))
  onUnmounted(() => mq.removeEventListener('change', handler))   // 规范第 4 条
  return { isDark }
}

// 形状四：全局单例（useUserStore 这类 Pinia store 本质是全局组合式函数）
// 何时需要「跨组件共享」：同一条数据被 3+ 个无父子关系的组件使用时，从 composable 升级为 store
```

**composable 与 store 的分界线**：`useLike` 是**组件级**的（每个文章详情页一份独立状态），`useUserStore` 是**应用级**的（全局唯一）。判断标准：「这份状态离开组件还有意义吗？」——登录态有（store），某页的点赞提交中有（composable）。

### 5.15.4 竞态防护：组合式函数的必修课

数据获取型 composable 必须处理**竞态**（race condition）：快速切换分页时，page=2 的慢响应可能覆盖 page=3 的快响应，展示错数据。SelfBlog 的 `stores/pages.ts` 用「代际号（manifestRequest）」防竞态，组合式函数版：

```typescript
export function useSearch(keyword: Ref<string>) {
  const results = ref<SearchItem[]>([])
  let requestSeq = 0                     // 代际号：每次请求自增

  async function run() {
    const seq = ++requestSeq             // 领号
    const res = await api.search(keyword.value)
    if (seq !== requestSeq) return       // 过号：已不是最新请求，丢弃
    results.value = res.data
  }
  watch(keyword, run)

  return { results }
}
```

**代际号的本质**：给每个异步操作发号，落账时验号——号不对说明自己已经过期。这比「取消上一个请求」（AbortController）更简单通用，SelfBlog 的 music store 用 revision 防「过期 Promise 回写」是同一个思想。

---

## 5.16 响应式原理：ref 为什么能「自动更新」

用了一整章的 `ref`/`reactive`，这一节掀开桌布看底牌——理解原理不是为了造轮子，而是为了**预判边界**（什么情况不会更新）。

### 5.16.1 Proxy：Vue 3 的响应式地基

Vue 2 用 `Object.defineProperty` 劫持属性的 get/set，有三个先天缺陷：新增/删除属性侦测不到（所以要有 `Vue.set`）、数组索引赋值侦测不到（所以 `arr[0] = x` 不更新）、初始化时递归全部属性（大对象初始化慢）。Vue 3 换用 **Proxy** 代理整个对象：

```typescript
const state = reactive({ count: 0 })
// 大致等价于（简化版）：
const proxy = new Proxy(target, {
  get(target, key) {
    track(target, key)        // 读取时：记录「谁在用我」
    return Reflect.get(target, key)
  },
  set(target, key, value) {
    Reflect.set(target, key, value)
    trigger(target, key)      // 写入时：通知所有「用我的人」
    return true
  }
})
```

两步闭环：**track（依赖收集）**——组件渲染函数读了 `state.count`，渲染函数就被记为 count 的依赖；**trigger（派发更新）**——写入 `state.count = 1` 时，通知所有依赖重新执行。`ref` 的原理同源，只是对基本类型没有对象可代理，包了一层 `{ value }` 再对这层对象做代理——这就是为什么必须 `.value`。

### 5.16.2 从原理预判「为什么不更新」的五种情况

| 场景 | 原因 | 正确写法 |
|------|------|---------|
| 解构 reactive 对象后用 | 解构取走的是**普通值**，脱离了 Proxy | `toRefs(state)` 或整体使用 |
| props 解构后用 | 同上，props 是 reactive | 4.x 响应式 Props 解构可用，或 `toRef(props, 'x')` |
| 替换整个 reactive 对象 | Proxy 挂在原对象上，换对象断链 | 逐字段赋值，或改用 `ref` |
| 直接改数组元素不更新 | （Vue 3 已修复——Proxy 全拦截，此条是 Vue 2 遗留认知） | Vue 3 放心用 `arr[i] = x` |
| ref 忘了 `.value` | 拿到的是包装对象不是值 | 模板里自动解包，JS 里必须 `.value` |

SelfBlog 的 `stores/pages.ts` 用 `manifestRequest` 代际号、`stores/music.ts` 用 revision，都是建立在「写入必触发更新」这一保证之上的——**原理清楚了，模式才用得笃定**。

### 5.16.3 computed 与 watch：两种响应的消费姿态

- **computed**：声明「值由什么算来」，**有缓存**——依赖不变不重算，模板里反复用也只算一次。适合「派生数据」（SelfBlog 的 `siteStore.adminRoot` 派生自 `adminPath`）。
- **watch**：声明「变化后做什么**副作用**」，无缓存、可异步。适合「请求、DOM 操作、localStorage 写入」（SelfBlog 的 `themeStore` watch 暗色状态写 `edu_theme`）。

判断口诀：**能用 computed 就不用 watch**——computed 是数据流（声明式、可缓存、无时序坑），watch 是事件流（命令式、有先后）。见到的「watch 里改另一个 ref 再被第三个 watch 监听」的链式写法，几乎都能用 computed 一条流收干净。

### 5.16.4 响应式的代价：性能心智

Proxy 拦截一切读写，单次开销微小但**遍历大列表渲染时会被放大**。三条节流纪律：大列表渲染前先用 computed 算好，避免模板里调用昂贵方法（每次重渲染都执行）；超长列表用虚拟滚动（6.8.10 的诚实评估）；**深层大对象只读不追踪**用 `shallowRef`/`markRaw`（SelfBlog 的 music store 存音频元数据对象时同理——原始 AudioBuffer 这类重型对象绝不该进深层响应式）。

理解原理的最高境界是**不再把它当魔法**：每个「为什么界面没刷新」的 bug，都能在 track/trigger 的模型里找到唯一解释。

---

## 参考资源

- [Vue 3 官方文档（中文）](https://cn.vuejs.org/)
- [Vue Router 4 官方文档](https://router.vuejs.org/zh/)
- [Pinia 官方文档](https://pinia.vuejs.org/zh/)
- [Vite 官方文档](https://cn.vitejs.dev/)
- [Axios 官方文档](https://axios-http.com/zh/docs/intro)
- [Vitest 官方文档](https://cn.vitest.dev/)
- [Vue Test Utils 官方文档](https://test-utils.vuejs.org/zh/)
- [TypeScript 官方文档](https://www.typescriptlang.org/docs/)
- [SelfBlog 项目源码](https://github.com/Xinghe-0203/SelfBlog)
- SelfBlog 前端关键源文件：`frontend/src/main.ts`、`frontend/src/api/index.ts`、`frontend/src/router/index.ts`、`frontend/src/router/guards.ts`、`frontend/src/stores/*.ts`、`frontend/vite.config.ts`、`frontend/src/test/setup.ts`
