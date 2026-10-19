# 第四章 TypeScript 全面入门

> 「SelfBlog 项目学习教程」第四章。本章承接第三章的 Spring Boot 后端，正式进入 SelfBlog 前端（Vue 3.5 + Vite 6 + TypeScript 6.0.3 + Pinia + Axios）的 TypeScript 世界。所有示例除特别说明外，都可以直接对应到 `frontend/` 目录下的真实代码。

## 本章导学

学完本章，你应该能够：

1. 独立搭建 TypeScript 开发环境，看懂并修改 `tsconfig.json`；
2. 熟练使用 TS 的基础类型、接口、泛型来描述一个 API 的输入输出；
3. 理解 SelfBlog 前端为什么在 `tsconfig.json` 里开启 `strict` 和 `noUncheckedIndexedAccess`，以及在这套严格规则下如何正确写代码；
4. 读懂并仿写 SelfBlog 的 API 层（`frontend/src/api/index.ts` 的 Axios 封装），让「后端响应信封」在前端拥有完整的类型表达；
5. 遇到常见编译错误（TS2532、TS2307、TS2345……）时不再慌张，能自己定位并修复。

> **学习建议**：TypeScript 不是一个新语言，而是 JavaScript + 类型标注。你不需要先精通 JS 才能学 TS——反过来，在项目里写 TS 的过程，正是同时补齐两者最好的方式。本章所有代码都建议动手敲一遍，而不是只「看懂」。

---

## 4.1 TypeScript 简介与开发环境搭建

### 4.1.1 为什么需要 TypeScript？

先看一段每个 JS 开发者都写过的「事故代码」：

```ts
// user.js —— 纯 JavaScript
function getDisplayName(user) {
  return user.nikeName.toUpperCase() // 拼写错了：应该是 nickname
}

getDisplayName({ nickname: '张三' })
// 运行时才炸：TypeError: Cannot read properties of undefined (reading 'toUpperCase')
```

这段代码的问题不在于「写错拼写」——谁都会犯错——而在于**错误的暴露时机**：它要等到代码真正跑起来、用户真的点到那个按钮，才会崩溃。而且崩溃的位置（浏览器控制台里某一行的 `undefined`）往往离真正的病因（拼写错误）十万八千里。

JavaScript 是**动态类型**语言：变量的类型在运行时才确定，编译器完全不关心 `user` 里有没有 `nickname`。就像两个人口头约好「你把资料发我」，结果你发过去他打不开——出了事，谁也说不清当初到底约定了什么。

TypeScript 的解法是把口头约定变成**白纸黑字的合同**：

```ts
// user.ts —— TypeScript
interface User {
  nickname: string
}

function getDisplayName(user: User): string {
  return user.nikeName.toUpperCase()
  //              ~~~~~~~~ 编辑器立刻画红线：
  //              Property 'nikeName' does not exist on type 'User'. Did you mean 'nickname'?
}
```

**还没运行，错误就已经被拦下来了。** 这就是 TypeScript 的核心价值：

| 维度 | 纯 JavaScript | TypeScript |
|------|--------------|------------|
| 错误暴露时机 | 运行时（用户可能先撞上） | 编译时（保存文件的那一刻） |
| 重构体验 | 改一处字段名，全项目 grep 碰运气 | 改类型定义，所有不兼容处立刻报错 |
| 编辑器体验 | 补全靠猜 | 精确补全属性、方法、参数 |
| 团队协作 | 读代码猜结构 | 类型即文档 |
| 学个新库 | 翻文档看参数 | `.d.ts` 类型文件就是最权威的文档 |

一句话总结：**TypeScript 不改变你的程序怎么运行，它改变的是你「发现错误有多早」。**

### 4.1.2 TypeScript 和 JavaScript 的关系

三句话讲清楚：

1. **TS 是 JS 的超集**：所有合法 JS 代码都是合法 TS 代码，你可以把 `.js` 改名为 `.ts` 一行不改，项目照样能跑（只是没有类型保护）。在这个基础上，TS 只做加法：类型标注、接口、泛型、枚举……
2. **类型会被「擦除」**：TS 编译器最终把代码转译回纯 JS，所有 `interface`、类型标注、泛型参数统统消失。运行时只有 JS——**类型只存在于编译期，运行时不存在**。这句话后面理解很多现象（比如为什么 Vue 的 props 类型挡不住运行时传错值）都靠它。
3. **TS 不负责提速**：它检查代码、转译代码，但不优化运行性能。性能优化交给 Vite/esbuild/浏览器。

SelfBlog 前端的技术栈里，TypeScript 的版本是 **6.0.3**（见 `frontend/package.json` 的 `"typescript": "^6.0.3"`），类型检查由 **vue-tsc 3.3.3** 完成（它是给 TypeScript 加上「认识 `.vue` 文件」能力的官方包装）。

### 4.1.3 安装与第一次编译

TypeScript 编译器命令叫 `tsc`（TypeScript Compiler）。在一个空目录里体验一下：

```bash
# 安装（作为开发依赖，不进生产包）
npm install --save-dev typescript

# 确认安装成功，打印版本号
npx tsc --version
```

写第一个文件 `hello.ts`：

```ts
const author: string = 'Zora'
const postCount: number = 42

console.log(`作者 ${author} 已发布 ${postCount} 篇文章`)
```

编译并运行：

```bash
npx tsc hello.ts        # 生成 hello.js（类型标注被擦除）
node hello.js           # 输出：作者 Zora 已发布 42 篇文章
```

打开生成的 `hello.js`，你会看到 `: string`、`: number` 全都不见了——这就是「类型擦除」。开发时把两个命令挂成 watch 模式更省心：

```bash
npx tsc hello.ts --watch   # 文件一保存就重新检查、重新编译
```

### 4.1.4 tsconfig.json：给整个项目立规矩

真实项目不会一个文件一个文件地编译，而是用一个 `tsconfig.json` 描述「这个目录下所有 TS 文件怎么检查」。运行 `npx tsc --init` 可以生成一份带全量注释的模板——注释多到吓人，其实常用的就十来项：

```jsonc
{
  "compilerOptions": {
    "target": "ES2020",          // 编译成哪个年代的 JS
    "module": "ESNext",          // 生成什么模块格式（import/export）
    "lib": ["ES2020", "DOM"],    // 可用的内置 API 声明（浏览器 + ES2020）
    "outDir": "./dist",          // 编译产物输出目录
    "strict": true,              // 打开严格模式全家桶（强烈推荐）
    "moduleResolution": "bundler", // 模块解析策略：配合打包器
    "esModuleInterop": true,     // 兼容 CommonJS 的默认导出
    "skipLibCheck": true,        // 跳过 node_modules 里 .d.ts 的检查（提速）
    "sourceMap": true            // 生成 sourceMap 方便调试
  },
  "include": ["src"]             // 检查哪些文件
}
```

常用选项速查表：

| 选项 | 作用 | 不设会怎样 |
|------|------|-----------|
| `target` | 产物的 JS 语法版本（ES5/ES2020/ESNext） | 旧浏览器可能看不懂新语法 |
| `module` | 产物模块格式（CommonJS/ESNext） | 与打包器/Node 运行方式不匹配 |
| `lib` | 类型层面的「内置 API 清单」 | 用 `Promise`、`Map` 等会报「找不到名称」 |
| `strict` | 一整套严格检查的总开关 | 大量潜在错误被放行 |
| `outDir` / `noEmit` | 产物放哪 / 只检查不产出 | —— |
| `paths` | 路径别名（如 `@/` → `src/`） | 只能写相对路径 |
| `include` / `exclude` | 检查范围 | 默认包含所有 `.ts` 文件 |

有一个初学者最容易困惑的点要提前说破：**在 Vue + Vite 项目里，`tsc`（实际是 `vue-tsc`）只当「质检员」，不当「流水线工人」**。SelfBlog 的 `package.json` 里：

```json
"build": "vue-tsc --noEmit && vite build"
```

`--noEmit` 意思是「只检查类型，不要产出任何 JS 文件」；真正把 TS 转译成浏览器能跑的 JS 的，是 Vite 内置的 esbuild。质检员和工人分工明确：

- **`vue-tsc`（质检员）**：仔细读类型，发现问题就拒绝放行（报错退出）；
- **esbuild（工人）**：只管快速把代码转译、打包，不做类型检查。

所以「类型报错」永远发生在 `npm run type-check` 或 `npm run build` 时，而 `npm run dev` 开发服务器为了速度通常只转译不检查——**别以为 dev 页面能跑就万事大吉，类型闸门在 type-check 和 build 那里等着呢。**

---

## 4.2 基础类型

### 4.2.1 类型标注与类型推断

给变量声明类型的最基本语法是「冒号 + 类型」：

```ts
let title: string = 'SelfBlog 学习教程'
let views: number = 1024
let published: boolean = true
```

但真正的高手写 TS，反而很少写这种标注，因为 TS 的**类型推断**足够聪明——声明时直接赋值，类型自动确定：

```ts
let title = 'SelfBlog 学习教程'   // 推断为 string
let views = 1024                  // 推断为 number
views = '很多'                    // ❌ Type 'string' is not assignable to type 'number'
```

经验法则：**局部变量靠推断，函数参数和返回值必须显式标注**。函数签名是「合同」，合同必须白纸黑字：

```ts
// 参数 + 返回值都标注：这就是这个函数的「使用说明书」
function formatReadingTime(minutes: number): string {
  return `${minutes} 分钟`
}
```

### 4.2.2 string / number / boolean 与数组

三个原始类型大家最熟悉，重点说数组。数组类型有两种等价写法：

```ts
// 写法一：T[]（推荐，简短）
const posts: string[] = ['第一篇', '第二篇']

// 写法二：Array<T>（泛型写法，类型参数一长时更易读）
const tags: Array<{ id: number; name: string }> = []
```

SelfBlog 的真实代码里（`frontend/src/views/Home.vue`）：

```ts
const posts = ref<Post[]>([])   // 一个装 Post 对象的响应式数组
const tags = ref<Tag[]>([])
```

数组开箱自带一条陷阱：**访问不存在的下标，JS 返回 `undefined`，TS 默认假装不知道**。`arr[5].name` 默认能通过类型检查，运行时却可能炸——SelfBlog 用 `noUncheckedIndexedAccess` 关掉了这个漏洞，4.10 节重点讲。

### 4.2.3 元组（Tuple）：长度和每个位置都固定的数组

数组是「一堆同类型的东西」，元组是「一排位置各司其职」：

```ts
// 数组：任意多个数字
const scores: number[] = [90, 85, 77]

// 元组：恰好两个元素，第一个是 number，第二个是 string
const postSummary: [number, string] = [42, 'TypeScript 入门']

postSummary[0].toFixed()  // ✅ TS 知道[0]是 number
postSummary[1].length     // ✅ TS 知道[1]是 string
```

典型用途是函数一次返回多个值：

```ts
function parseJwt(token: string): [header: string, payload: string, signature: string] | null {
  const parts = token.split('.')
  if (parts.length !== 3) return null
  return [parts[0], parts[1], parts[2]]
}
```

注意元组支持**给每个位置命名**（`header`、`payload`……），编辑器会在调用处提示名字，比裸的 `[string, string, string]` 可读性强得多。

### 4.2.4 枚举（Enum）与它的替代品

枚举把一组相关的常量收进一个命名空间：

```ts
enum PostStatus {
  Draft = 0,     // 未发布
  Published = 1  // 已发布
}

function getStatusText(status: PostStatus): string {
  return status === PostStatus.Published ? '已发布' : '草稿'
}
```

- **数字枚举**：不写值则从 0 自增；反过来可以从数字反查名字（`PostStatus[1] === 'Published'`）。
- **字符串枚举**：每个成员显式赋字符串，调试时日志可读，更推荐：

```ts
enum Theme {
  Light = 'light',
  Dark = 'dark'
}
```

不过要说明：**SelfBlog 前端大量使用「字符串字面量联合类型」而不是枚举**：

```ts
// frontend/src/types/common.ts —— 真实代码
export type SortOrder = 'asc' | 'desc'

// post.ts 里的 PostStatus 也是 type 定义的联合类型
```

两种写法都能表达「值只能取这几个」，取舍如下：

| | enum | 字面量联合 `'a' | 'b'` |
|---|------|----------------------|
| 编译产物 | 生成真实的 JS 对象（有运行时代价） | 什么都不生成，纯类型 |
| 运行时反查 | 支持 | 不支持 |
| 与 JS 生态互操作 | 需要导入枚举对象 | 字符串直接用 |
| 类型安全 | 好 | 好 |

`enum` 是 TS 独有的运行时构造，在「类型擦除」哲学下显得有点特殊；字面量联合更轻、更贴近 JS。新代码没有特殊理由时，**优先字面量联合**。

### 4.2.5 any：关掉烟雾报警器的类型

`any` 表示「放弃类型检查，随便吧」：

```ts
let data: any = JSON.parse(text)
data.foo.bar.baz          // 编译通过，运行时生死有命
data().anything()         // 编译也通过！
```

`any` 能通过任何检查、赋给任何类型、从任何类型赋来——它不是「万能类型」，而是**检查盲区**。比喻：`any` 就像把家里的烟雾报警器拆了，世界确实清净了，但起火也没人报信。JSON.parse、第三方无类型库、历史遗留代码……用到 `any` 的地方应该被逐个消灭，团队约定是**每次写 `any` 前先问自己三遍：我真的拿不到类型吗？**

### 4.2.6 unknown：诚实的 any

`unknown` 是 `any` 的安全版：**接受任何值，但用之前必须先「验明正身」（收窄）**：

```ts
function parse(text: string): unknown {
  return JSON.parse(text)
}

const data = parse('{"title":"hello"}')
data.title            // ❌ 'data' is of type 'unknown'
data = 123            // ✅ unknown 可以被赋任何值

if (typeof data === 'object' && data !== null && 'title' in data) {
  console.log((data as { title: string }).title)  // 收窄后才能用
}
```

这个「必须先检查才能用」的约束正是它的价值：**接收方被迫面对「数据可能不是我想的那样」这个事实**。SelfBlog 的 API 层里，处理「后端返回的原始信封」时就用了这个思路（4.13 节实战）。经验法则：**值从「不可信来源」进来（网络、JSON.parse、用户输入），类型标注用 `unknown`，不用 `any`。**

### 4.2.7 void 与 null / undefined

- **`void`**：函数「没有有意义的返回值」。约定俗成：回调/副作用函数标注 `void`。

```ts
function log(msg: string): void {
  console.log(msg)
  // 没有 return，或 return; —— 不允许 return 一个值
}
```

- **`null`**：「有意的空」（这里明确没有值）；
- **`undefined`**：「还没有值」（压根没赋过 / 属性不存在 / 函数没返回）。

在 `strict` 模式（含 `strictNullChecks`）下，`null` 和 `undefined` 不再是「任何类型的隐藏成员」：

```ts
let name: string = 'Zora'
name = null   // ❌ strict 模式下报错：Type 'null' is not assignable to type 'string'

let nickname: string | null = null   // 想允许空，就显式写进联合类型
nickname = '星河'
```

这是 strict 模式最大的价值之一：**「可能为空」这件事在类型上无处遁形**。你之后会反复看到 SelfBlog 代码里的 `user.value?.role`、`posts.value = []` 这样的写法，全是在和「可能为空」打交道。

### 4.2.8 never：永不发生的类型

`never` 表示「这个位置根本不会有值」：

- 永远抛错的函数；
- 永远不会执行到结尾的函数（死循环）；
- 类型系统在穷尽检查后判定「不可能」的分支。

它最实用的场景是**穷尽检查**——配合字面量联合，给 `switch` 上保险：

```ts
type Shape = 'circle' | 'square'

function area(shape: Shape): number {
  switch (shape) {
    case 'circle': return 1
    case 'square': return 1
    default:
      // 如果将来有人给 Shape 加了 'triangle' 却忘了改这里，
      // 这一行就会编译报错：'triangle' 不能赋给 never
      const _exhaustive: never = shape
      return _exhaustive
  }
}
```

原理：所有成员都被处理完后，`default` 分支里 `shape` 的类型被收窄为「剩下的成员」，即 `never`；只要还有漏网之鱼，它就不是 `never`，赋值就会报错。**用类型系统逼着未来的自己把新增情况处理完整**，这是 TS 优雅之处。

---

## 4.3 接口与类型别名

### 4.3.1 interface：给对象画结构图

`interface` 描述「一个对象长什么样」——有哪些属性、什么类型：

```ts
interface User {
  id: number
  username: string
  nickname: string
  avatar: string
  role: 'admin' | 'user'
}
```

SelfBlog 前端 `frontend/src/types/` 目录就是这么组织类型的：按业务域拆成 `user.ts`、`post.ts`、`comment.ts` 等文件，再由 `types/index.ts` 统一 re-export。写后端对接代码前，先翻这个目录——**类型定义就是前后端的「合同文本」**。

### 4.3.2 可选属性与只读属性

```ts
interface Post {
  id: number
  title: string
  summary?: string        // 可选：可以有，可以没有（类型自动含 undefined）
  readonly createdAt: string  // 只读：初始化后不许改
}

const post: Post = { id: 1, title: 'TS 入门' }        // ✅ summary 没写，OK
post.createdAt = '2026-01-01'                        // ❌ Cannot assign to 'createdAt'
console.log(post.summary?.length)                    // 可选属性访问要用 ?.
```

`summary?: string` 等价于 `summary: string | undefined`。读它的地方必须处理「不存在」的情况——`?.` 或先判断。

### 4.3.3 索引签名：属性名不固定的对象

有些对象的键在写类型时根本列不全，比如 SelfBlog 后端的 `site_setting` 表就是一张 KV 表（`setting_key` + `setting_value`）。前端把它建模成键值对就要用**索引签名**：

```ts
interface SiteSettings {
  [key: string]: string     // 任意 string 键 → string 值
}

const settings: SiteSettings = {
  siteName: 'SelfBlog',
  footer: '© 2026'
}
```

更常用的是内置工具类型 `Record`，语义完全相同：

```ts
const settings: Record<string, string> = { /* 同上 */ }
```

注意：**索引签名一旦声明，所有「具名」属性的类型都必须兼容它**：

```ts
interface Mixed {
  [key: string]: string
  port: number   // ❌ number 不兼容索引签名的 string
}
```

### 4.3.4 接口里的函数类型

接口可以描述「这个对象上有个函数」，两种写法等价：

```ts
interface Fetcher {
  // 方法简写
  get(url: string): Promise<string>
  // 属性 + 函数类型（可以重命名复用）
  post: (url: string, body: unknown) => Promise<string>
}
```

专门的函数类型建议先起名再使用，可读性好：

```ts
type Comparator<T> = (a: T, b: T) => number

function sortPosts(posts: Post[], by: Comparator<Post>) { /* ... */ }
```

### 4.3.5 extends：接口的继承

```ts
interface Entity {
  id: number
  createdAt: string
}

interface Post extends Entity {
  title: string
}

// 相当于 Post = Entity 的所有成员 + title
```

可以同时继承多个接口，用逗号分隔。SelfBlog 的类型体系大量用到这种「基础类型 + 扩展」的模式，例如分页：先定义「分页壳」，再套任意业务数据（见 4.6 泛型一节）。

### 4.3.6 type：类型别名

`type` 给**任何类型**（不只是对象）起名字：

```ts
// 联合类型——interface 表达不了
type ID = number | string

// 元组
type Point = [number, number]

// 从现有类型推导
type PostTitles = Post['title']

// 条件类型（4.8 节）
type Maybe<T> = T | null | undefined
```

### 4.3.7 interface vs type：怎么选？

两者在「描述对象结构」上几乎完全等价，可以互相替换，但有差异：

| 能力 | interface | type |
|------|-----------|------|
| 描述对象/函数 | ✅ | ✅ |
| 联合、元组、字面量、条件类型 | ❌ | ✅ |
| extends 继承 | ✅（可继承多接口） | ✅（交叉 `&`） |
| 合并声明（同名自动合并） | ✅（同名 interface 自动合并成员） | ❌（重名直接报错） |
| 给「已存在的库」补充成员 | ✅ 靠声明合并 | ❌ |

**SelfBlog 的约定（也是社区主流约定）**：

- 对象/类的「形状」用 `interface`——报错信息里 interface 的名字会保留，可读性更好，也留了扩展余地；
- 联合、元组、工具类型、从现有类型推导的「计算型类型」用 `type`；
- 团队内保持一致即可，不必教条。

### 4.3.8 实战拆解：SelfBlog 的通用类型层

`frontend/src/types/common.ts` 是全项目类型的「地基」，全文不长，值得逐行读懂：

```ts
/** API 统一响应格式 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/** 分页请求参数 */
export interface PaginationParams {
  pageNum?: number
  pageSize?: number
}

/** 分页响应数据 */
export interface PaginatedData<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

/** 分页 API 响应 */
export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>

/** 排序方式 */
export type SortOrder = 'asc' | 'desc'

/** 通用 ID 类型 */
export type ID = number | string

/** 通用键值对 */
export type Recordable<T = unknown> = Record<string, T>

/** 可为空 */
export type Nullable<T> = T | null

/** 可选 */
export type Optional<T> = T | undefined
```

看点：`ApiResponse<T = unknown>` 里的 `= unknown` 是**默认类型参数**——不关心业务数据时写 `ApiResponse` 即可，等价于 `ApiResponse<unknown>`；`PaginatedResponse<T>` 展示了类型组合的威力：**分页响应 = 信封 + 分页壳 + 业务数据**，三块积木拼出来的类型在编辑器里可以一路点进去看定义。`types/index.ts` 再把它们与各业务域类型统一导出，项目里任何地方 `import type { ... } from '@/types'` 就够了。

---

## 4.4 函数与类型

### 4.4.1 参数、返回值与推断

```ts
function add(a: number, b: number): number {
  return a + b
}
```

返回值类型通常可以省略——函数体简单时 TS 会推断；但**导出给别人的公共函数建议显式写返回值**：一是当作文档，二是防止函数体改动后返回类型「悄悄变了」却没人发现。

### 4.4.2 可选参数、默认参数、剩余参数

```ts
// 可选参数：必须排在必选参数后面
function greet(name: string, prefix?: string): string {
  return `${prefix ?? '你好'}，${name}`
}

// 默认参数：自带默认值，调用方可省略
function page(pageNum = 1, pageSize = 10) { /* ... */ }

// 剩余参数：把多余的实参收成数组
function sum(...nums: number[]): number {
  return nums.reduce((acc, n) => acc + n, 0)
}
```

注意可选参数 `prefix?: string` 的类型是 `string | undefined`，函数体内直接 `prefix.length` 会报错——老老实实用 `??` 或判空。**TS 的严格模式和现实生活是一致的：没给的东西，用之前先确认有。**

### 4.4.3 函数类型表达式与调用签名

```ts
// 函数类型表达式：描述「一个函数」
type Mapper = (value: string, index: number) => string

// 对象「可被调用」：调用签名
interface ApiClient {
  (url: string, config?: RequestInit): Promise<Response>
  timeout: number   // 还能带属性——既是函数又是对象
}
```

后者听起来冷门，其实你天天在用：Axios 实例 `api` 本身「可调用」（`api(config)`），又挂着 `get`/`post` 属性和 `interceptors`，Axios 的类型定义正是这么写的。

### 4.4.4 函数重载

同一个函数名，按**不同的参数组合**有不同的返回类型——TS 用「重载签名 + 一个实现」表达：

```ts
// 重载签名（可以有多个，只描述，不实现）
function formatDate(date: string): string
function formatDate(date: Date): string
function formatDate(date: number): string

// 实现签名：参数类型取所有重载的并集，内部自己收窄
function formatDate(date: string | Date | number): string {
  return new Date(date).toLocaleDateString('zh-CN')
}

formatDate('2026-01-01')  // ✅
formatDate(new Date())    // ✅
formatDate(true)          // ❌ 没有任何重载匹配 boolean
```

一个真实世界的例子就是 Axios 的泛型方法签名（简化）：

```ts
get<T = any, R = AxiosResponse<T>>(url: string, config?): Promise<R>
```

两个类型参数 `T`（业务数据类型）与 `R`（最终返回类型）配合，让「我发一个请求，拿到一个信封」这种模式能被精确描述——4.13 节实战会用到这个细节。

### 4.4.5 this 与 noImplicitThis

`this` 是 JS 的老大难：指向谁取决于「怎么被调用」而不是「写在哪」。TS 在 strict 模式下开启 `noImplicitThis`：**凡是 TS 推断不出 `this` 指向的地方，直接报错**，把运行时事故变成编译期错误：

```ts
const counter = {
  count: 0,
  up() { this.count++ },          // ✅ 对象方法，this 是 counter
  delayed() {
    setTimeout(function () {
      this.count++                // ❌ noImplicitThis：这里的 this 是 unknown
    }, 1000)
  },
  delayedFixed() {
    setTimeout(() => {
      this.count++                // ✅ 箭头函数捕获外层 this
    }, 1000)
  }
}
```

实践守则三条：

1. 对象方法里要用 this，就用普通函数语法（`up() {...}`），让 TS 能推断；
2. 回调里需要外层 this，用**箭头函数**；
3. 实在复杂的场景（事件处理器、类），宁可先 `const self = this` 或显式绑定，也别留 `any` 的 this。

### 4.4.6 void 返回值的一个特殊规则

把「返回值是 void 的函数类型」赋给「有返回值的函数」是允许的：

```ts
type Handler = () => void

const h: Handler = () => 123   // ✅ 允许！调用方约定「我不看返回值」
```

语义是「调用方声明不消费返回值」，所以实现者返回什么都行。这个规则让 `arr.forEach(...)` 之类的回调既能返回值又不破坏约定。反过来（把「必返回 string」赋给「void」位置之外的地方）则不行。

---

## 4.5 类

### 4.5.1 传统写法回顾

JS 的 class 语法 TS 全盘继承：

```ts
class Post {
  title: string          // 字段声明
  views = 0              // 带初值，类型推断为 number

  constructor(title: string) {
    this.title = title
  }

  summarize(): string {
    return `${this.title}（${this.views} 次浏览）`
  }
}
```

TS 在此之上的增强，才是本章重点。

### 4.5.2 访问修饰符：public / private / protected

```ts
class Account {
  public username: string      // 默认就是 public，一般不写
  private passwordHash: string // 只类内部可访问
  protected role: string       // 类内部 + 子类可访问

  constructor(username: string, passwordHash: string, role: string) {
    this.username = username
    this.passwordHash = passwordHash
    this.role = role
  }
}

const acc = new Account('zora', 'xxx', 'admin')
acc.username          // ✅
acc.passwordHash      // ❌ Property 'passwordHash' is private
```

要点：

- 这是**编译期检查**，编译产物里没有任何「私有」——运行时想访问照样能访问。真想运行时也拦住，用 JS 原生的 `#` 私有字段（`#passwordHash`），TS 同样支持；
- `private` 不是安全机制，是**沟通机制**：告诉同事「这个字段是内部实现，别碰」。

### 4.5.3 readonly：只读成员

```ts
class Config {
  readonly baseUrl: string
  constructor(baseUrl: string) {
    this.baseUrl = baseUrl
  }
  update() {
    this.baseUrl = '/api2'   // ❌ Cannot assign to 'baseUrl' because it is read-only
  }
}
```

初始化（声明处或构造器内）之外不许赋值。适合放配置、ID 这类「生下来就不该变」的数据。

### 4.5.4 参数属性：一行顶四行

构造器参数前加修饰符，TS 会**自动生成同名字段并赋值**：

```ts
class Config {
  constructor(
    public readonly baseUrl: string,
    private timeout: number
  ) {}   // 不用手写 this.baseUrl = baseUrl
}
```

等价于 4.5.2 里手写的完整版。`readonly` 和 `public/private/protected` 可以组合。SelfBlog 这类函数式风格为主的 Vue 前端里类写得不多，但后端 Java 世界全是类——学这一节顺便理解 Java 的字段封装，一举两得。

### 4.5.5 存取器 get / set

```ts
class Temperature {
  private celsius = 0
  get fahrenheit(): number { return this.celsius * 1.8 + 32 }
  set fahrenheit(value: number) { this.celsius = (value - 32) / 1.8 }
}
```

getter/setter 让「字段」和「计算属性」对调用方长得一样，内部实现随便换。

### 4.5.6 抽象类：半成品图纸

`abstract` 类不能直接 `new`，只能被继承；它声明「子类必须实现什么」：

```ts
abstract class StorageAdapter {
  abstract get(key: string): string | null   // 只声明，不实现
  abstract set(key: string, value: string): void

  // 也可以有已实现的公共逻辑，子类直接继承
  remove(key: string): void {
    this.set(key, '')
  }
}

class LocalStorageAdapter extends StorageAdapter {
  get(key: string): string | null { return localStorage.getItem(key) }
  set(key: string, value: string): void { localStorage.setItem(key, value) }
}
```

比喻：普通类是「施工图」，照着就能盖房子；抽象类是「半成品图纸」——框架结构画好了，几间房留白，标着「继承者必须自己设计」。SelfBlog 后端 Spring 里大量 Service 实现继承同一个 `ServiceImpl` 基类，就是同样的思想。

### 4.5.7 implements：类必须满足某个接口

`extends` 是「继承实现」，`implements` 是「立军令状」——我保证长这个样：

```ts
interface Serializable {
  serialize(): string
}

class Post implements Serializable {
  constructor(public title: string) {}
  serialize(): string { return JSON.stringify({ title: this.title }) }
  // 如果忘写 serialize，编译立刻报错
}
```

可以同时 `extends Xxx implements A, B`。**接口负责「对外承诺」，抽象类负责「共享实现」**，两者经常配合使用。

### 4.5.8 一个相关配置：useDefineForClassFields

SelfBlog 的 `tsconfig.json` 里有一行 `"useDefineForClassFields": true`。它控制类字段用 ES2022 的 `Object.defineProperty` 语义（「定义」）还是老式的赋值语义（「设置」）。差别在「子类字段覆盖父类同名字段」「字段与 getter/setter 同名」这类边角场景才显现，日常写业务几乎感知不到——知道它是「让类字段行为符合最新 JS 标准」的开关即可，别删它，Vue 3 依赖这个行为。

---

## 4.6 泛型

### 4.6.1 为什么需要泛型：模具思想

先看一个不够用的函数：

```ts
function identity(value: number): number {
  return value
}
```

只支持 number。想支持 string 就再写一个？想支持所有类型就上 `any`？——`any` 会把「进去是什么类型、出来还是那个类型」这条信息抹掉：

```ts
function identityAny(value: any): any {
  return value
}
const n = identityAny(42)
n.toFixed()   // ❌ 不报错！n 是 any，检查全关。运行时万一是 string 就炸了
```

泛型解决的就是这个问题：**类型参数化**——把类型当参数传进去，进出关系被完整保留：

```ts
function identity<T>(value: T): T {
  return value
}

const a = identity(42)        // T 被推断为 number，a: number
const b = identity('hello')   // T 被推断为 string，b: string
```

比喻：`any` 版函数是「黑箱子，扔进啥出啥，出来是不是原来的东西不保证」；泛型函数是「模具」——扔进圆形的料，出来的保证是圆形。**泛型的本质：让「输入类型决定输出类型」这条链路在类型系统里活下来。**

### 4.6.2 泛型函数的常见形态

```ts
// 多个类型参数
function pair<A, B>(first: A, second: B): [A, B] {
  return [first, second]
}

// 类型参数不必出现在参数里，可显式指定
function typedArray<T = string>(length: number): T[] {
  return Array.from({ length })
}

// 约束（下一小节）
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key]
}
```

SelfBlog 里你能找到的真实泛型函数是 `utils/crypto.ts` 的 `decodeData<T>`（`stores/user.ts:103` 的用法）：

```ts
// 解码存储数据时显式告诉 TS：解开之后应当是什么类型
const decodedUser = decodeData<User>(savedUser)
```

### 4.6.3 泛型接口

这是 SelfBlog 前端最核心的泛型实践——统一响应信封：

```ts
// frontend/src/types/common.ts
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}
```

后端所有接口都返回这个信封，变化的只有 `data`：

```ts
const res1: ApiResponse<Post> = await postApi.getPostById(1)
//   res1.data 的类型是 Post，编辑器补全 title/content/...

const res2: ApiResponse<void> = await postApi.deletePost(1)
//   res2.data 是 void（删除操作没有业务数据）
```

配套的分页壳也是泛型：

```ts
export interface PaginatedData<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>
```

`PaginatedResponse<Post>` 一层套一层读出来就是：`{ code, message, data: { records: Post[], total, ... } }`。**泛型的可组合性**在这里体现得淋漓尽致。

### 4.6.4 泛型类

```ts
class ResultBox<T> {
  constructor(private value: T) {}
  map<U>(fn: (v: T) => U): ResultBox<U> {
    return new ResultBox<U>(fn(this.value))
  }
  get(): T { return this.value }
}

const box = new ResultBox(42).map(n => n.toString())
box.get().toUpperCase()   // ✅ get() 返回 string
```

`map` 返回 `ResultBox<U>`：链条上每一步的类型变换都被记录。Spring 后端的 `Result<T>` 响应类与之同构。

### 4.6.5 泛型约束 extends：给类型参数划底线

裸的 `<T>` 上，TS 只知道「是某种类型」，什么都不许做。约束（constraint）告诉它「至少要有这些成员」：

```ts
function logLength<T extends { length: number }>(value: T): T {
  console.log(value.length)   // ✅ 有约束，TS 知道一定有 length
  return value
}

logLength('hello')      // ✅ string 有 length
logLength([1, 2, 3])    // ✅ 数组有 length
logLength(42)           // ❌ number 没有 length
```

最常见的约束搭档是 `keyof`——「T 的键的联合」，保证传入的 key 一定合法：

```ts
function getProperty<T, K extends keyof T>(obj: T, key: K): T[K] {
  return obj[key]
}

const post: Post = { id: 1, title: 'TS', /* ... */ }
getProperty(post, 'title')   // ✅ 返回 string
getProperty(post, 'titles')  // ❌ 'titles' 不是 Post 的键，编译期就被抓住
```

`T[K]` 叫**索引访问类型**，返回「T 里 K 这个键对应的值类型」——参数类型和返回类型联动起来了。

### 4.6.6 常用泛型工具类型（必须背下来的六个）

TS 内置了一批工具类型，全部基于泛型实现。以下六个在业务代码里出现频率最高：

**Partial\<T>** —— 所有属性变可选。典型场景：「更新接口只传要改的字段」：

```ts
interface Post { id: number; title: string; content: string; tags: string[] }

type PostPatch = Partial<Post>
// { id?: number; title?: string; content?: string; tags?: string[] }

function updatePost(id: number, patch: PostPatch) { /* ... */ }
updatePost(1, { title: '新标题' })   // ✅ 只传一个字段也合法
```

**Required\<T>** —— 反过来，所有属性变必填（把 `?:` 摘掉）。

**Pick\<T, K>** —— 从 T 里**挑**出 K 里的键：

```ts
type PostPreview = Pick<Post, 'id' | 'title'>
// { id: number; title: string }
```

列表页只需要缩略字段时用它，别把整个 Post 抱着到处跑。

**Omit\<T, K>** —— 从 T 里**剔除** K 里的键，剩下全要：

```ts
type CreatePostInput = Omit<Post, 'id' | 'views' | 'createdAt'>
// id/views/createdAt 由服务端生成，创建时本来就不该传
```

`Pick` 和 `Omit` 互为镜像：键少用 `Pick`（白名单），键多用 `Omit`（黑名单）。

**Record\<K, V>** —— 键集合为 K、值类型为 V 的字典：

```ts
const statusText: Record<number, string> = {
  0: '草稿',
  1: '已发布'
}
// 更妙的是键也可以是字面量联合，逼你写全：
type PostStatus = 'draft' | 'published'
const map: Record<PostStatus, string> = { draft: '草稿', published: '已发布' }
// 少写一个键都会报错——Record + 字面量联合 = 「必须写全的映射表」
```

**ReturnType\<F>** —— 拿到函数 F 的返回类型：

```ts
function createDefaultPost() {
  return { title: '', content: '', tags: [] as string[] }
}
type NewPost = ReturnType<typeof createDefaultPost>
// { title: string; content: string; tags: string[] }
```

`typeof` 在这里取「值的类型」，`ReturnType` 再从函数类型里抽出返回值类型。**当类型可以「从实现推导」时，别手抄一遍**——实现改了，类型自动跟着变。

**先声明类型还是先写实现？** 经验：对外契约（API 层、组件 props、store 的公开状态）**先写类型**；内部工具函数**先写实现，再用工具类型导出**。两种方向都要会。

---

## 4.7 类型收窄与类型守卫

联合类型（`string | number`、`Post | null`）给了表达力，但也带来问题：拿到一个 `Post | null`，什么都能做吗？当然不能——**收窄（narrowing）**就是 TS 根据你写的检查逻辑，自动把联合类型「缩小」到具体分支的能力。这是 strict 模式下写代码的基本功。

### 4.7.1 typeof 收窄

```ts
function format(value: string | number): string {
  if (typeof value === 'string') {
    return value.trim()        // 这个分支里 value: string
  }
  return value.toFixed(2)      // 这个分支里 value: number
}
```

TS 分析控制流，`if` 里 `typeof` 判断过后，类型就按判断结果收窄；进入 `else`（或 if 之后），则收窄为剩下的联合成员。这套「控制流分析」同样作用于 `return`、`throw`、`break`：

```ts
function get(id: number): Post | null {
  const post = cache.get(id)
  if (post === null) throw new Error('文章不存在')
  return post       // ✅ 已排除 null，这里就是 Post
}
```

### 4.7.2 真值收窄与可选链

`if (value)` 会把 `null`、`undefined`、`''`、`0`、`false`、`NaN` 全部排除——方便但危险：`0` 和 `''` 是合法业务值时会被误伤。**有明确语义就用明确判断**：

```ts
// SelfBlog stores/user.ts 的真实写法
const isAdmin = computed(() => user.value?.role === 'admin')
```

`?.`（可选链）在 `user.value` 为 `null/undefined` 时短路返回 `undefined`，而不是抛错。它和 `??`（空值合并：仅在前值为 `null/undefined` 时取后值）是天作之合：

```ts
const nickname = user.value?.nickname ?? user.value?.username ?? '匿名'
```

对比一个典型误用：`count?.toString() ?? '0'`，当 `count` 为 `0` 时是安全的（`0?.toString()` 正常返回 `'0'`），但如果写成 `count || '0'`，`0` 也会被替换——语义不同，选择要谨慎。

### 4.7.3 相等与字面量收窄

```ts
type Status = 'loading' | 'success' | 'error'

function render(status: Status): string {
  if (status === 'loading') return '加载中…'
  if (status === 'success') return '成功'
  return '失败'    // 剩下的只可能是 'error'
}
```

字面量联合在比较中被逐个排除。这就是 4.2.4 说「字面量联合优于枚举」的底气之一：**收窄分析对它完全友好**。

### 4.7.4 in 与 instanceof

```ts
// in：检查属性存在性
interface Bird { fly(): void }
interface Fish { swim(): void }

function move(animal: Bird | Fish) {
  if ('fly' in animal) animal.fly()
  else animal.swim()
}

// instanceof：检查类的原型
function handleError(err: unknown) {
  if (err instanceof AxiosError) {
    console.log(err.response?.status)   // 收窄为 AxiosError，有 response 属性
  }
}
```

### 4.7.5 自定义守卫：`is` 谓词

当收窄逻辑复杂或需要复用时，抽成一个函数——但普通的 boolean 返回值**不会**让 TS 收窄，需要类型谓词 `value is Type`：

```ts
// 普通函数：TS 不知道 true 意味着什么
function isPost(value: unknown): boolean {
  return typeof value === 'object' && value !== null && 'title' in value
}

// 类型守卫：true ⇒ value 收窄为 Post
function isPost(value: unknown): value is Post {
  return typeof value === 'object' && value !== null && 'title' in value
}

function show(data: unknown) {
  if (isPost(data)) {
    console.log(data.title)   // ✅ 收窄为 Post
  }
}
```

`is` 谓词本质是「**我向编译器承诺：这个函数返回 true 时，参数一定是这个类型**」——承诺是否兑现，编译器不验证（函数体里的逻辑是你自己的事），所以守卫函数要写得老老实实。

SelfBlog 的 `api/index.ts:93` 里有个近亲——用结构类型参数代替 `is`：

```ts
function isRetryableError(error: { code?: string; response?: unknown; message?: string }): boolean {
  if (error.code === 'ERR_CANCELED') return false
  return !error.response && (/* ... */)
}
```

它不收窄类型，只复用「判断逻辑」；调用方传进来的 AxiosError 天然满足这个结构。什么时候用 boolean、什么时候用 `is`？**需要收窄时用 `is`，只需要复用判断逻辑时用 boolean**。

### 4.7.6 可辨识联合（Discriminated Union）：状态机建模神器

如果联合的每个成员都有一个**字面量类型的公共键**（discriminant，判别键），TS 的收窄会精确到「一键切换分支」：

```ts
type RequestState<T> =
  | { status: 'idle' }
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; message: string }

function view<T>(state: RequestState<T>): string {
  switch (state.status) {
    case 'idle':    return '待开始'
    case 'loading': return '加载中'
    case 'success': return `数据：${JSON.stringify(state.data)}`  // 只有这里有 data
    case 'error':   return state.message                          // 只有这里有 message
  }
}
```

`state.status` 取值互斥，每个分支里 TS 只给你该分支的成员——`loading` 分支里写 `state.data` 直接编译报错。**把「不同状态携带不同数据」建模成可辨识联合，非法状态在类型上就不可能存在**，这比「一个对象上 data 和 error 都可空，用时再各查一遍」安全得多。SelfBlog 相邻文章接口的返回（`prev/next` 各自可为 `null`，见 `api/post.ts:49`）就是靠 `| null` 明确表达「可能没有」，配合 `?.` 安全消费。

---

## 4.8 高级类型

这一节的工具平时用不上，读别人的类型定义（Vue、Axios 的 `.d.ts`）时全靠它们。目标是**看懂**，其次才是写。

### 4.8.1 联合类型与交叉类型

- **联合 `|`**：「或」——`string | number`，取值可以是其一；
- **交叉 `&`**：「且」——`A & B`，同时具备 A 和 B 的全部成员：

```ts
interface Loggable { log(): void }
interface Serializable { serialize(): string }

type Service = Loggable & Serializable
```

交叉类型常被当成「interface 的 extends 的 type 版本」；两个接口有同名但类型不同的属性时，交叉结果该属性变成 `never`，算是一个警告信号。

### 4.8.2 字面量类型：把值当类型

字符串/数字字面量本身可以当类型，只允许完全相等的值：

```ts
type Direction = 'up' | 'down' | 'left' | 'right'
type Port = 8825            // SelfBlog 开发后端端口
type HttpCode = 200 | 401 | 403 | 404 | 429 | 500

function goto(dir: Direction) { /* ... */ }
goto('up')    // ✅
goto('Up')    // ❌ 大小写都不放过
```

单看没多大用，组合起来威力巨大——`Record<PostStatus, string>` 必须写全映射、可辨识联合的判别键、模板字面量类型的原料，全是它。

### 4.8.3 索引访问类型

在**类型层面**做下标操作，取出子类型：

```ts
type PostTitle = Post['title']            // string
type PostTagList = Post['tags']           // string[]

// 对数组/元组用 number 索引，取出元素类型
type PostListItem = Post[][number]        // Post

// SelfBlog 真实用例（api/index.ts:97）：
// RETRYABLE_CODES 是一个 as const 的字符串数组常量，
// (typeof RETRYABLE_CODES)[number] 取出「它的所有元素构成的联合类型」
const codes: (typeof RETRYABLE_CODES)[number][] = ['ECONNABORTED', 'ETIMEDOUT']
```

`(typeof X)[number]` 这个组合值得背下来：**从常量数组推导出「合法值」的联合类型**，常量加一项，类型自动跟着变。

### 4.8.4 条件类型

类型层面的三元表达式：

```ts
type IsString<T> = T extends string ? true : false

type A = IsString<'hello'>   // true
type B = IsString<42>        // false
```

裸类型参数上的条件类型是**分布式**的——联合类型会被拆开逐个判断再重新合并：

```ts
type ToArray<T> = T extends any ? T[] : never
type R = ToArray<string | number>   // string[] | number[]（不是 (string|number)[]！）
```

这个特性可以被利用（把联合「洗」成想要的形状），也可以用方括号包起来禁用：`[T] extends [any]`。

### 4.8.5 infer：在条件类型里「捕获」类型

`infer` 在 `extends` 的模式里声明一个「待捕获」的占位符，匹配成功时捕获到的类型可以在 true 分支使用：

```ts
// 内置 ReturnType 的简化实现
type MyReturnType<T> = T extends (...args: any[]) => infer R ? R : never

// 捕获 Promise 的内部类型
type Awaited<T> = T extends Promise<infer V> ? V : T
type Data = Awaited<Promise<Post>>   // Post
```

读法：「如果 T 长得像一个函数（或 Promise），就把它的返回值（内部值）命名为 R（V），然后……」。所有内置工具类型的源码都是条件类型 + infer 的组合，读懂这一对，`type-fest` 这类类型库就不再是天书。

### 4.8.6 映射类型与 as 重映射

「对类型的每个键做同样的变换」：

```ts
type Optional<T> = { [K in keyof T]?: T[K] }        // 这就是 Partial 的实现
type ReadonlyVersion<T> = { readonly [K in keyof T]: T[K] }

// as 子句：映射时改键名
type Getters<T> = {
  [K in keyof T as `get${Capitalize<string & K>}`]: () => T[K]
}
type PostGetters = Getters<Pick<Post, 'id' | 'title'>>
// { getId: () => number; getTitle: () => string }
```

### 4.8.7 模板字面量类型

类型层面的字符串拼接，由字面量联合驱动：

```ts
type EventName = 'click' | 'focus'
type HandlerName = `on${Capitalize<EventName>}`
// 'onClick' | 'onFocus'

// 约束路由路径
type ApiPath = `/post/${number}` | `/tag/${number}`
function fetchApi(path: ApiPath) { /* ... */ }
fetchApi('/post/1')    // ✅
fetchApi('/user/1')    // ❌
```

Vue 源码里大量使用它约束组件事件名与插槽名。日常业务写得少，认得出即可。

---

## 4.9 装饰器

### 4.9.1 装饰器是什么

装饰器（Decorator）是一种「附加到类/方法/属性/参数上的特殊语法」，在**声明处**给目标添加元数据或包装行为：

```ts
function readonly(target: unknown, key: string) { /* ... */ }

class Article {
  @readonly
  id = 1
}
```

要启用 TS 的传统装饰器（ SelfBlog 这种 Vue 项目里其实不开，见 4.9.4），需要在 `tsconfig.json` 配置：

```jsonc
{
  "compilerOptions": {
    "experimentalDecorators": true,
    "emitDecoratorMetadata": true   // 设计元数据（配合 reflect-metadata，Spring 风格依赖注入需要）
  }
}
```

带 `experimental` 前缀是因为它当年基于还没定稿的提案。TS 5.0 起也支持了 ECMAScript 正式标准的装饰器（Stage 3），两者语法与能力有差异，本文按国内后端生态（NestJS/Spring 风格）常用的**实验性装饰器**讲解。

### 4.9.2 四种装饰器速览

**类装饰器**——拿到类构造函数，可以修改/替换它：

```ts
function Controller(path: string): ClassDecorator {
  return (target) => {
    // 把路由前缀记录到类的元数据里
    Reflect.defineMetadata('basePath', path, target)
  }
}

@Controller('/post')
class PostController {}
```

**方法装饰器**——拿到原型、方法名、属性描述符，可以包一层（日志、权限、限流）：

```ts
function Log(): MethodDecorator {
  return (target, key, descriptor) => {
    const original = descriptor.value as (...args: unknown[]) => unknown
    descriptor.value = function (...args: unknown[]) {
      console.log(`调用 ${String(key)}`, args)
      return original.apply(this, args)
    }
  }
}

class PostService {
  @Log()
  delete(id: number) { /* ... */ }
}
```

**属性装饰器 / 参数装饰器**——分别拿到「挂在哪个类的哪个属性/第几个参数」，通常只用来记录元数据（配合 `emitDecoratorMetadata`，供 IoC 容器反射使用）：

```ts
class PostService {
  @Autowired
  private mapper!: PostMapper       // 属性装饰器：标记「这里要注入」

  list(@Param('id') id: number) {}  // 参数装饰器：标记参数来源
}
```

### 4.9.3 和 Spring 注解对照

如果你完成了第三章，下面的对照表会让你恍然大悟——**Spring 的注解和 TS 装饰器解决的是同一类问题：在不污染业务代码的前提下，把横切关注点（路由、鉴权、日志、注入）声明上去**：

| Spring（Java 注解，运行时反射） | NestJS/TS（装饰器） | 作用 |
|------|------|------|
| `@RestController("/post")` | `@Controller('post')` | 声明控制器与路由前缀 |
| `@GetMapping("/list")` | `@Get('list')` | 方法级路由 |
| `@Autowired` / 构造器注入 | `@Inject()` | 依赖注入 |
| `@Transactional` | `@Transactional()` | 事务/切面 |
| `@Cacheable("hotTags")` | `@Cacheable('hotTags')` | 缓存 |

差别在于实现机制：Java 注解是**运行时反射**读取的；TS 装饰器是**编译期**执行的真实 JS 函数（配合 reflect-metadata 在运行时存取元数据）。

### 4.9.4 SelfBlog 前端为什么不用装饰器？

这是个重要的架构决定，理由有四：

1. **Vue 3 的 Composition API 时代，装饰器已非主流**。Vue 2 + class-component 时代装饰器（`@Component`、`@Prop`）流行过；Vue 3 的 `<script setup>` 用**编译器宏**（`defineProps`、`defineEmits`）替代了装饰器的角色——宏是编译器内置的，无需任何 flag，类型推断还更好（4.12 节）。
2. **状态管理用 Pinia 的 setup store**（函数 + `ref`/`computed`），不需要 class + `@Inject` 式的依赖注入。
3. **装饰器仍是 `experimental`**，标准与实验版并存，混用有迁移成本；项目没有需要装饰器才能解决的场景。
4. **可读性**：装饰器把行为「藏」进声明处，对不熟悉的读者是隐式魔法；显式代码（组合式函数、普通函数调用）更符合 SelfBlog 前端「一眼看懂」的风格。

结论：**前端不用，但要看得懂**——一是将来读 NestJS 式的 TS 后端、二是理解 Spring 注解的「声明式」思想时，装饰器都是绕不开的一课。

---

## 4.10 tsconfig 深入：SelfBlog 的严格模式实战

现在是本章分量最重的一节。先完整看一遍 SelfBlog 前端的 `frontend/tsconfig.json`：

```jsonc
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "ignoreDeprecations": "6.0",

    /* Bundler mode */
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",

    /* Linting */
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedIndexedAccess": true,

    /* Path aliases */
    "baseUrl": ".",
    "paths": { "@/*": ["src/*"] },

    /* Vue */
    "types": ["vite/client"]
  },
  "include": [
    "src/**/*.ts", "src/**/*.tsx", "src/**/*.vue", "src/**/*.d.ts", "env.d.ts"
  ],
  "exclude": ["node_modules", "dist"]
}
```

### 4.10.1 strict 全家桶

`"strict": true` 不是单一检查，而是一个开关组，打开后默认启用以下项目（了解每个的名字，报错时能对号入座）：

| 成员 | 作用 | 一句话 |
|------|------|--------|
| `noImplicitAny` | 禁止悄悄的 any | 推断不出的地方必须显式标注 |
| `strictNullChecks` | null/undefined 不再混入其他类型 | 「可能为空」必须显式建模 |
| `strictFunctionTypes` | 函数参数双向协变 → 严格逆变 | 回调参数类型更严谨 |
| `strictBindCallApply` | bind/call/apply 参数检查 | 不许乱传参 |
| `strictPropertyInitialization` | 类字段必须初始化 | 构造器里没赋值的非可选字段报错 |
| `noImplicitThis` | this 类型不明时报错 | 见 4.4.5 |
| `alwaysStrict` | 产物顶层加 "use strict" | —— |
| `useUnknownInCatchVariables` | catch 的 err 是 unknown 而非 any | 处理错误前必须收窄 |

`strictPropertyInitialization` 常坑新手：

```ts
class User {
  name!: string        // ! 非空断言：我保证构造后（或靠装饰器）一定赋值
  age?: number         // 声明为可选也行
  constructor() {
    // name 若既没赋值也没 ! 或 ?，这里报 TS2564
  }
}
```

`useUnknownInCatchVariables` 值得单独点赞：`catch (e)` 里的 `e` 不再是 any，而是 unknown——你被迫先 `e instanceof Error` 收窄再读 `message`，避免「以为捕获的是 Error 结果是字符串」的经典事故。

### 4.10.2 重点：noUncheckedIndexedAccess

SelfBlog 在 strict 之外**额外**开启了这一项（它不在 strict 组里）。它改变的是一条最容易被忽视的规则：**通过索引（下标/键）访问得到的值，类型自动加上 `| undefined`**。

先看默认行为（不开这个选项）有多坑：

```ts
const arr = ['a', 'b', 'c']
arr[10].toUpperCase()
// 默认：✅ 编译通过（TS 认为 arr[10] 是 string）
// 运行时：💥 Cannot read properties of undefined (reading 'toUpperCase')
```

TS 的默认假设是「下标一定有效」——这显然是谎言。开启 `noUncheckedIndexedAccess` 后，类型系统终于说了实话：

```ts
const arr: string[] = ['a', 'b', 'c']
const item = arr[0]
// item: string | undefined   ← 不再骗你

item.toUpperCase()         // ❌ TS18048 / TS2532：'item' is possibly 'undefined'
arr[0].foo                 // ❌ 同理，arr[0].foo 编译不过！
```

它对**对象索引**同样生效：

```ts
const map: Record<string, number> = { a: 1 }
const v = map['b']        // number | undefined（键可能不存在）
```

而循环遍历**不受影响**——`for...of` / `for...of index` 遍历到的是「确实存在的元素」：

```ts
for (const s of arr) console.log(s.length)   // ✅ s: string，没有 undefined
arr.forEach((s) => console.log(s.length))    // ✅ 同理
```

**正确写法清单**（SelfBlog 仓库里都能找到原型）：

```ts
// ① 可选链：允许「没有就 undefined」继续传递
const name = posts[0]?.title

// ② 先守卫再使用
const first = posts[0]
if (first) {
  console.log(first.title)
}

// ③ 空值合并给默认值
const page = Number(route.query.page ?? 1)

// ④ 断言前先自己验证——SelfBlog stores/user.ts 的真实代码：
const parts = token.value.split('.')
if (parts.length === 3) {
  // 已经验证了长度，这里的 ! 非空断言才有底气
  const payload = JSON.parse(atob(parts[1]!.replace(/-/g, '+').replace(/_/g, '/')))
}
```

注意第 ④ 条：`!`（non-null assertion）不是「消除问题」，而是「我对编译器发誓这里不是 undefined」——发誓前必须先有验证逻辑，否则就是把运行时崩溃换了个地方。**能用收窄（①②③）就别用断言（④）**。

这个选项初学时会觉得「处处报错、寸步难行」，但坚持一周后你会发现：项目里「Cannot read properties of undefined」这类运行时崩溃几乎绝迹。SelfBlog 全仓库在 `type-check` 闸门下保持零 `undefined` 事故，这个开关功不可没。

### 4.10.3 其他 Linting 开关

```jsonc
"noUnusedLocals": true,             // 声明了没用的局部变量报错（TS6133）
"noUnusedParameters": true,         // 声明了没用的函数参数报错（想保留用 _ 前缀）
"noFallthroughCasesInSwitch": true  // case 穿透报错（漏写 break）
```

三个都是「把粗心扼杀在编译期」的消防栓，配合 ESLint 形成双保险。

### 4.10.4 Bundler 模式相关

```jsonc
"moduleResolution": "bundler",      // 按「打包器视角」解析模块：支持 package.json exports、
                                    // 无扩展名导入等 Vite 时代的新玩法
"allowImportingTsExtensions": true, // 允许 import xxx from './a.ts'（仅限 noEmit 项目）
"isolatedModules": true,            // 每个文件必须能被单独转译——这是 esbuild 模式的要求：
                                    // 所以跨模块「只导类型」必须写 import type（见 4.11）
"moduleDetection": "force",         // 所有文件都当模块处理，避免全局变量互相污染
"noEmit": true,                     // tsc 只检查不产出（转译交给 Vite）
```

`isolatedModules` 与 `import type` 的关系要刻进肌肉记忆：esbuild 转译单个文件时**看不到其他文件**，无法知道 `import { Post } from '@/types'` 里的 Post 是「类型」还是「真值」；写 `import type` 明确告诉它「编译时删掉这一行」。SelfBlog 所有类型导入都遵循此规范（如 `api/post.ts` 顶部的 `import type { ApiResponse, ... } from '@/types'`）。

### 4.10.5 路径别名与 include

```jsonc
"baseUrl": ".",
"paths": { "@/*": ["src/*"] }       // '@/stores/user' → 'src/stores/user'
```

别名让深层 import 不再 `../../..` 地爬楼梯。注意 `paths` 只影响 **tsc/vue-tsc** 的类型解析；运行时路径替换由 **Vite** 的 `resolve.alias` 负责——两边都要配，缺一边就会出现「编辑器补全正常、构建报错」或反之的诡异现象。`"types": ["vite/client"]` 则把 Vite 的全局类型（`import.meta.env` 等）纳入检查，`include` 把 `.vue` 文件圈进类型检查范围——这正是 `vue-tsc` 存在的理由。

---

## 4.11 模块、命名空间与声明文件

### 4.11.1 ES 模块与 type-only 导入

现代前端模块体系就是 ES Module：`export` / `import`。TS 只加了两个语法糖：

```ts
// 导出一个类型
export interface Post { /* ... */ }

// 只导入类型：编译后整行消失
import type { Post } from '@/types'

// 混合导入：类型和值各归各
import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from 'axios'
//            ^^^^^^^^ 值导入        ^^^^ 类型导入
```

这正是 `api/index.ts:5` 的第一行。`import type` 不是风格洁癖，而是 `isolatedModules` 环境下的**正确性要求**（4.10.4 已解释）。

### 4.11.2 命名空间：基本退役

`namespace` 是 ES 模块普及前 TS 的代码组织方案（`namespace Utils { export function a() {} }`），在模块化项目里**没有用武之地**——模块本身就是隔离单元。今天它只活在这些地方：旧代码迁移、`declare global` 声明全局类型、以及某些以全局脚本方式加载的库的 `.d.ts` 里。SelfBlog 全项目零 namespace，见到时知道是「老古董」即可。

### 4.11.3 声明文件 .d.ts：类型的外交部

`.d.ts` 是**只含类型、不含实现**的文件。三种典型场景：

**① 给 JS 库补类型**。npm 包普遍自带类型；若某个库没有（控制台警告 `Could not find a declaration file for module 'xxx'`），可以装社区维护的 `@types/xxx`，实在没有就自己写：

```ts
// src/types/untyped-lib.d.ts
declare module 'untyped-lib' {
  export function render(el: HTMLElement, options?: { theme?: string }): void
}
```

**② 声明非 JS 资源模块**。SelfBlog 的 `frontend/env.d.ts` 里就有真实例子：

```ts
declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<Record<string, never>, Record<string, never>, unknown>
  export default component
}
```

它告诉 TS：「凡是 `import Xxx from './Xxx.vue'`，都当作一个 Vue 组件处理」。有了这段，`.vue` 导入才不报 TS2307。

**③ 扩充全局类型与环境类型**。`env.d.ts` 的后半部分给 `import.meta.env` 上了户口：

```ts
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_TARGET?: string
  readonly VITE_APP_TITLE: string
  readonly VITE_APP_VERSION: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

之后代码里写 `import.meta.env.VITE_APP_TITLE`，编辑器精确补全这四个键；敲一个不存在的 `VITE_API_PORT` 立刻报错——**环境变量这种纯字符串配置也能被类型保护**。

### 4.11.4 三斜线指令与 declare

- **三斜线指令**：`.d.ts` 文件顶部的特殊注释，声明对其他类型包的依赖。`env.d.ts` 第一行 `/// <reference types="vite/client" />` 的意思是「把 vite/client 包的类型引入本文件」。常见指令就三种：`types`、`path`、`lib`，日常只需要认识它们。
- **`declare`**：声明「这个东西存在，但实现不在这」——`declare const VERSION: string`（全局变量）、`declare module 'x'`（模块形状）、`declare namespace`（老式全局对象）。它是「对外部世界的类型描述」的万能动词。

记忆锚点：**`.ts` 描述「我们的代码」，`.d.ts` 描述「别人的代码/环境」**。

---

## 4.12 TS 与 Vue 3 结合

SelfBlog 前端全部使用 `<script setup lang="ts">`。这一节把 Vue 3 里所有和类型打交道的 API 过一遍，全部用项目真实代码。

### 4.12.1 defineProps：泛型写法是唯一正解

声明 props 的类型，不用运行时对象（`{ title: String }`），用**编译器宏 + 泛型**：

```vue
<!-- components/pages/MarkdownContent.vue（真实代码，简化） -->
<script setup lang="ts">
defineProps<{ source: string }>()
</script>
```

需要默认值时配合 `withDefaults`——`ArticleList.vue:7` 的真实签名：

```ts
const props = withDefaults(
  defineProps<{
    posts: Post[]
    featured?: boolean
    heading?: 'h2' | 'h3'      // 字面量联合：连「用哪个标题层级」都被约束
  }>(),
  {
    featured: false,
    heading: 'h2'
  }
)
```

也可以把 props 类型抽成独立 interface（`Button.vue` 的 `ButtonProps`），复杂组件更整洁。要点：

- 泛型参数里写的**就是 TS 类型**：`Post[]`、`'h2' | 'h3'` 随便用；
- 可选属性 `featured?: boolean` 对应「父组件可以不传」；
- 父组件传错类型，`vue-tsc` 直接报错——模板里的类型检查也是真的；
- **类型检查替代不了运行时校验**：TS 类型编译后即消失，运行时传进来的还是 JS 值。涉及外部输入（URL 参数、localStorage）时，组件内仍需防御性检查。

### 4.12.2 defineEmits：把事件签名写成函数类型

```vue
<!-- components/common/ImagePreview.vue（真实代码） -->
<script setup lang="ts">
const emit = defineEmits<{ close: [] }>()
// close: [] —— 事件名 close，参数为空元组
</script>

<!-- components/layout/NavbarMusicControl.vue（真实代码） -->
<script setup lang="ts">
const emit = defineEmits<{
  toggle: [trigger: HTMLElement]   // 事件 toggle，携带一个 HTMLElement 参数
  close: []
}>()
</script>
```

这个「事件名: 参数元组」的写法是 Vue 3.3+ 的新语法，比旧的「函数签名风格」更直观。收益：`emit('togle')` 拼错立刻报错；父组件模板里 `@toggle="handler"` 的 handler 参数类型自动推断为 `HTMLElement`。

### 4.12.3 ref / computed / 响应式数据的类型

`ref` 会**自动推断**，但初始值是 `null/undefined` 时必须显式给类型参数：

```ts
// views/Home.vue（真实代码）
const posts = ref<Post[]>([])           // Ref<Post[]>，类型可省略（初值已表达）

// stores/user.ts（真实代码）
const token = ref<string | null>(null)  // 必须写 <string | null>，否则推断成 Ref<null>
const user = ref<User | null>(null)
```

`.value` 之后的使用，就回到 4.7 的收窄世界：`user.value?.role`、`posts.value[0]?.title`。`computed` 的类型从返回值自动推断，一般不用标注：

```ts
const isLoggedIn = computed(() => !!token.value && !!user.value)  // ComputedRef<boolean>
```

`reactive` 则要求**直接给初始值**（不能只给类型），类型由初值推断；需要「空壳 + 后填充」的模式时，让每个字段都容纳空值（`null` 或 `undefined`），别用 `as` 硬拗。

### 4.12.4 模板中的类型

`<template>` 里的表达式同样被 `vue-tsc` 检查：

```vue
<script setup lang="ts">
const props = defineProps<{ posts: Post[] }>()
</script>

<template>
  <!-- v-for 变量自动是 Post 类型 -->
  <article v-for="post in posts" :key="post.id">
    <!-- post.title 有补全；写成 post.titel 会报错 -->
    <h2>{{ post.title }}</h2>
    <!-- noUncheckedIndexedAccess 在模板里同样生效：posts[0] 是 Post | undefined -->
    <p>{{ posts[0]?.title }}</p>
  </article>
</template>
```

模板事件、插槽、组件 props 的类型都来自组件定义——类型定义写得越准，模板里白嫖的检查越多。

### 4.12.5 Pinia setup store 的类型

`stores/user.ts` 的 store 是「函数 + 响应式状态」构成，类型完全自动：

```ts
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(null)
  const user = ref<User | null>(null)
  const isAdmin = computed(() => user.value?.role === 'admin')  // ComputedRef<boolean>

  function updateTokens(t: string, r: string) { /* ... */ }

  return { token, user, isAdmin, updateTokens }
})
export const useUserStore = useAuthStore   // 历史别名
```

调用方 `const store = useUserStore()` 之后，`store.token`、`store.isAdmin`、`store.updateTokens` 全部有精确类型——**setup store 几乎不需要手写类型，这是它优于 options store 的地方之一**。注意给 store 补类型时（如 `decodeData<User>(savedUser)`），引用的是 `@/types` 里的业务类型，而不是在 store 里重新发明。

### 4.12.6 vue-tsc：让类型检查覆盖 .vue

`tsc` 不认识 `.vue` 单文件组件，Vue 官方的 **vue-tsc** 是包了一层、能解析 SFC 的 TypeScript 编译器。SelfBlog 把它做成了两个 npm 闸门：

```bash
npm run type-check    # vue-tsc --noEmit：纯类型检查
npm run build         # vue-tsc --noEmit && vite build：先检查再打包
```

CI 和本地开发都以 `type-check` 全绿为底线。配合 `vitest`（测试也用 TS 写）、ESLint（typescript-eslint 规则），形成完整的前端质量闸门。

---

## 4.13 实战：用 TS 重写 SelfBlog 的 API 层

前面所有知识在这一节汇成一次完整的实战。目标：理解并仿写 `frontend/src/api/` 的类型体系——这也是本项目「TS 到底带来了什么」的最佳展品。

### 4.13.1 先立合同：后端统一响应信封

SelfBlog 后端（第三章）所有接口统一返回 `Result<T>{code, message, data, timestamp}` 信封：`code` 为 200 表示业务成功，非 200 业务失败；分页时 `data` 里是 `{records, total, pageNum, pageSize, pages}`。

前端的「合同文本」就是 `frontend/src/types/common.ts`：

```ts
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PaginatedData<T> {
  records: T[]
  total: number
  pageNum: number
  pageSize: number
  pages: number
}

export type PaginatedResponse<T> = ApiResponse<PaginatedData<T>>
```

> 细节讨论：后端信封其实还有 `timestamp` 字段，前端 `ApiResponse` 没有声明它。TS 对「对象多出来的字段」是宽容的（结构化类型：声明了三个字段，实际对象多带一个 `timestamp` 完全合法），所以不声明不影响编译；但「合同」上写明会更完整——你也可以试着给它加上 `timestamp?: number`，体会「前端类型是后端契约的镜像」这句话。

### 4.13.2 核心契约：拦截器把 AxiosResponse 拆成信封

这是 SelfBlog API 层**最容易踩坑、也最能体现类型价值**的一条契约。看 `api/index.ts:174` 的响应拦截器：

```ts
api.interceptors.response.use(
  (response) => {
    logger.debug('Response success', { url: response.config.url, status: response.status })
    return response.data        // ★ 返回的是 AxiosResponse 的 data——后端信封本身！
  },
  /* 错误分支略 */
)
```

`response` 原本类型是 `AxiosResponse`（含 `status`、`headers`、`data` 等），拦截器把它**拆包**，只返回 `response.data`——即后端信封 `{code, message, data}`。这带来一个调用层的重要事实：

```ts
// ❌ 错误认知（Axios 默认行为）：
const res = await api.get('/post/1')
res.data.data.title    // 不是这样！res 已经是信封了

// ✅ 正确用法（SelfBlog 契约）：
const res = await api.get('/post/1')
res.data.title         // res 是信封 ApiResponse<Post>，res.data 才是业务数据
```

用类型语言重述：拦截器把 `Promise<AxiosResponse<T>>` 变成了 `Promise<ApiResponse<T>>`。**运行时行为变了，如果不改类型，类型就成了谎言**——接下来看类型怎么跟上。

### 4.13.3 Axios 泛型签名与「上下文推断」

Axios 的 `get`/`post` 等方法有个双泛型签名（简化）：

```ts
get<T = any, R = AxiosResponse<T>>(url: string, config?: AxiosRequestConfig): Promise<R>
```

`T` 是业务数据类型，`R` 是方法最终解析出的类型，**默认** `R = AxiosResponse<T>`。关键机制：`R` 支持**上下文推断**——当你显式声明了函数/变量的返回类型，`R` 会被反推为那个类型，而不是用默认值：

```ts
function getPostById(id: number): Promise<ApiResponse<Post>> {
  return api.get(`/post/${id}`)   // T 从 URL 侧拿不到，R 被上下文推成 ApiResponse<Post>
}
```

这行能编译通过且类型正确，靠的正是这个细节：拦截器已经在运行时改写了返回值，类型声明再由调用方补上，**运行时与类型系统重新对齐**。这也是一个通用的工程教训：**凡是「中间件/拦截器改写了返回形状」的地方，类型必须有人显式接住**，否则 TS 只能按默认假设撒谎。

### 4.13.4 带类型的请求方法：api/post.ts 精读

`frontend/src/api/post.ts` 是这个模式的完整落地，逐行精读：

```ts
import api from './index'
import type {
  ApiResponse, PaginatedResponse, Post,
  CreatePostRequest, UpdatePostRequest, PostListParams, PostSearchParams, Draft
} from '@/types'

export const postApi = {
  /** 创建文章：入参是「创建请求」类型，出参信封里装着 Post */
  createPost(data: CreatePostRequest): Promise<ApiResponse<Post>> {
    return api.post('/post', data)
  },

  updatePost(id: number | string, data: UpdatePostRequest): Promise<ApiResponse<Post>> {
    return api.put(`/post/${id}`, data)
  },

  /** 删除没有业务数据：ApiResponse 默认 T = unknown */
  deletePost(id: number | string): Promise<ApiResponse> {
    return api.delete(`/post/${id}`)
  },

  getPostById(id: number | string): Promise<ApiResponse<Post>> {
    return api.get(`/post/${id}`)
  },

  /** 相邻文章：内联定义嵌套结构，prev/next 都可能不存在 */
  getAdjacentPosts(id: number | string): Promise<
    ApiResponse<{
      prev: { id: number; title: string } | null
      next: { id: number; title: string } | null
    }>
  > {
    return api.get(`/post/${id}/adjacent`)
  },

  /** 列表：分页信封 */
  getPostList(params: PostListParams): Promise<PaginatedResponse<Post>> {
    return api.get('/post/list', { params })
  },

  saveDraft(data: CreatePostRequest): Promise<ApiResponse<Draft>> {
    return api.post('/post/draft', data)
  },

  getLatestDraft(): Promise<ApiResponse<Draft>> {
    return api.get('/post/draft/latest')
  },

  advancedSearch(params: PostSearchParams): Promise<PaginatedResponse<Post>> {
    return api.get('/post/search/advanced', { params })
  }
}
```

拆出三条设计规律，写任何项目的 API 层都适用：

1. **请求入参建类型**（`CreatePostRequest`、`PostListParams`）——「这个接口要什么」是契约的一半；
2. **返回值声明到信封的 `data` 泛型**（`ApiResponse<Post>`、`PaginatedResponse<Post>`）——「会还给你什么」是另一半；
3. **方法体一行**：URL 拼接 + 转发，逻辑全部沉淀在拦截器层，API 模块保持「类型密集、逻辑稀薄」。

### 4.13.5 教学版重写：把拦截器的类型讲透

真实 `api/index.ts` 有 396 行（token 刷新排队、指纹校验、重试、访客 ID……），不适合教学。下面抽出一个保留类型骨架的教学版，你可以对照源文件读：

```ts
import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig
} from 'axios'
import type { ApiResponse } from '@/types'

const api: AxiosInstance = axios.create({
  baseURL: '/api',        // 硬编码 /api：开发靠 Vite 代理、生产靠 JAR 内嵌/vercel.json 转发
  timeout: 15000
})

// ── 请求拦截器：注入鉴权头 ────────────────────────────────
api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = readToken()                       // 省略：store 优先、storage 兜底
  if (token && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers['X-Visitor-Id'] = getVisitorId() // 匿名点赞/留言的访客标识
  config.headers['X-Trace-Id'] = generateTraceId()
  return config
})

// ── 响应拦截器：拆包 + 错误加工 ───────────────────────────
api.interceptors.response.use(
  (response) => response.data,                    // ★ 拆包：返回后端信封
  async (error: AxiosError<ApiResponse<unknown>>) => {
    // 给错误对象挂上「给用户看的文案」，类型上用声明合并扩展 AxiosError
    error.userMessage =
      error.response?.data?.message ?? fallbackMessage(error.response?.status)
    return Promise.reject(error)
  }
)

export default api
```

两个类型细节值得展开：

**① 扩展 AxiosError**。拦截器给 `error` 挂了 `userMessage` 属性，但 `AxiosError` 类型上没有它。真实项目里调用方仍要读它，于是需要一个类型扩展。最小方案是接口声明合并：

```ts
// types/axios.d.ts
import 'axios'

declare module 'axios' {
  export interface AxiosError {
    userMessage?: string
  }
}
```

之后全项目 `error.userMessage` 都有类型，编辑器可补全。这就是 4.3.7 表格里「interface 声明合并」的用武之地。

**② 401 刷新队列的类型**。Token 过期时，并发请求要排队等新 token——`failedQueue` 是一个「等待 promise 被外界 resolve」的队列，它的类型直接描述了这个协议：

```ts
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (error: unknown) => void
}> = []

const processQueue = (error: unknown, token: string | null = null): void => {
  failedQueue.forEach((prom) => {
    if (error) prom.reject(error)
    else prom.resolve(token!)   // processQueue(null, token) 时 token 必非空，! 有过程性保证
  })
  failedQueue = []
}
```

`resolve`/`reject` 的参数类型就是「这个 promise 最终交付什么」的合同——`resolve: (token: string) => void` 一眼看懂「队首等待者拿到的是新 token」。刷新成功后 `processQueue(null, newToken)`、失败 `processQueue(refreshError, null)`，两条路径的类型都严丝合缝。

### 4.13.6 错误处理类型：让 catch 里的世界也有类型

调用方怎么安全地处理错误？先看类型，再给范式：

```ts
// ① 后端业务失败（HTTP 200 但 code != 200）：
const res = await postApi.getPostById(1)
if (res.code !== 200) {
  showToast(res.message)     // res.message: string，有类型
  return
}
render(res.data)             // 走到这里 res.data: Post

// ② HTTP 层错误（4xx/5xx/网络失败）走 reject 分支：
try {
  const res = await postApi.getPostList({ pageNum: 1 })
} catch (err) {
  // err 是 unknown（strict + useUnknownInCatchVariables），先收窄：
  if (axios.isAxiosError(err)) {
    showToast(err.userMessage ?? err.message)   // isAxiosError 是官方类型守卫
  } else {
    showToast('未知错误')
  }
}
```

`axios.isAxiosError(err)` 内部就是一个 `err is AxiosError` 的自定义守卫（4.7.5 的官方范例）。而 `err.userMessage` 之所以有类型，靠的是 4.13.5 的声明合并。**错误处理链路上每个环节——信封 code、拦截器加工的 userMessage、catch 里的收窄——都有类型参与**，这就是「API 层被 TS 重写后」该有的样子。

### 4.13.7 调用方视角：类型如何改变组件代码

最后站在消费端看完整收益（`Home.vue` 思路）：

```ts
import { postApi } from '@/api/post'
import type { Post } from '@/types'

const posts = ref<Post[]>([])

onMounted(async () => {
  const res = await postApi.getPostList({ pageNum: 1, pageSize: 10 })
  posts.value = res.data.records     // ✅ res.data 是 PaginatedData<Post>
  // 如果这里手滑写成 res.data.data.records：
  // ❌ TS2339：Property 'records' does not exist on type 'unknown'
  // 类型系统当场告诉你「你多拆了一层信封」
})
```

把 4.13.2 的契约再压缩成一句给未来的自己：**在 SelfBlog 前端，「await 拿到的」就是信封，`.data` 之后才是业务数据；少一层会拿到信封本身，多一层会拿到 unknown。写错任何一边，vue-tsc 都会拦下你。**

---

## 4.14 常见编译错误与排查

最后是一份「急救手册」：SelfBlog 前端开发中最常遇到的编译错误、错误信息原文与修复方法。遇到报错先来这张表对号入座。

**TS2532 / TS18048 —— 「可能是 undefined/null」**

```text
error TS2532: Object is possibly 'undefined'.
error TS18048: 'user.value' is possibly 'undefined'.
```

最常见错误，几乎总是 `strict` + `noUncheckedIndexedAccess` 在尽职（4.10.2）。修复优先级：`?.` 可选链 > 显式守卫 `if (x)` > `??` 默认值 > 有验证逻辑后的 `!` 断言。

```ts
const title = posts[0]?.title                    // ①
if (first) { use(first.title) }                  // ②
const page = Number(route.query.page ?? 1)       // ③
```

**TS2339 —— 属性不存在**

```text
error TS2339: Property 'records' does not exist on type 'unknown'.
error TS2339: Property 'nikeName' does not exist on type 'User'. Did you mean 'nickname'?
```

两种成因：类型错了（拼错、用错层级，如 4.13.7 的 `res.data.data`）或类型缺了（接口里没这个字段）。注意 `Did you mean` 提示——TS 的纠错建议通常就是答案。若确信字段存在但类型没跟上（比如后端新加了字段），去 `src/types/` 把契约补上。

**TS2307 —— 找不到模块**

```text
error TS2307: Cannot find module './PostCard.vue' or its corresponding type declarations.
error TS2307: Cannot find module '@/stores/user'.
```

`.vue` 导入报错：检查 `env.d.ts` 的 `declare module '*.vue'` 是否还在（4.11.3）；`@/` 别名报错：检查 `tsconfig.json` 的 `baseUrl`/`paths` 与 Vite 的 `resolve.alias` 两边是否一致。路径大小写也要与磁盘一致，Windows 不区分大小写但 TS 区分。

**TS2304 —— 找不到名称**

```text
error TS2304: Cannot find name 'ref'.
```

忘了 `import { ref } from 'vue'`。和 TS2307 的区别：TS2307 是「模块找不到」，TS2304 是「标识符找不到」。

**TS7006 —— 隐式 any**

```text
error TS7006: Parameter 'item' implicitly has an 'any' type.
```

回调参数在上下文推断失效（如对 `any[]` 遍历、解构了无类型对象）时出现。修复：给源头补类型（数组用 `T[]` 标注），而不是给参数补 `any`——治本不治标。

**TS2345 —— 参数类型不匹配**

```text
error TS2345: Argument of type 'string | undefined' is not assignable to
parameter of type 'string'.
```

联合类型传给了更窄的参数。说明调用方可能传来空值——先收窄（`?.`、守卫、`??`），而不是急着 `!`。

**TS2322 —— 类型不可赋值**

```text
error TS2322: Type 'string' is not assignable to type 'number'.
```

赋值两侧类型对不上，从报错里「来源类型 → 目标类型」两端各自排查。若来自 props/接口响应，往往意味着契约（类型定义）和实际数据不一致，修契约不修编译器。

**TS2564 —— 类字段未初始化**

```text
error TS2564: Property 'title' has no initializer and is not definitely
assigned in the constructor.
```

`strictPropertyInitialization` 的功劳。修复：构造器里赋值，或 `title?: string`，或确有保证时 `title!: string`。

**TS6133 —— 未使用的变量**

```text
error TS6133: 'x' is declared but its value is never read.
```

`noUnusedLocals`/`noUnusedParameters` 的功劳。删掉它；参数确需占位时命名成 `_` 开头。

**TS7016 —— 模块缺少类型声明**

```text
error TS7016: Could not find a declaration file for module 'untyped-lib'.
```

第三方库没带类型。按顺序：找 `@types/untyped-lib` → 自己写最小 `declare module 'untyped-lib'`（4.11.3）→（下策）`// @ts-expect-error` 并留注释。

**TS2769 —— 重载都不匹配**

```text
error TS2769: No overload matches this call.
```

泛型函数/重载函数的参数组合对不上号。展开报错详情看「候选签名们」期望什么，最常见于 axios 调用（config 对象字段写错名）。

**通用排查流程**：

1. 读错误码 + 错误文本，先查本表；
2. 把鼠标悬停报错处，看编辑器给出的「期望类型 vs 实际类型」；
3. 怀疑类型定义时，`Ctrl+点击` 跳进类型定义看原文；
4. 报错指向 `.vue` 模板内部时，用 `npm run type-check` 拿完整输出（dev 模式不检查类型）；
5. 最后的手段才是 `as`/`@ts-expect-error`——每次使用都要能向 code review 解释为什么类型系统在这「失明」。

---

## 4.16 后端类型对照：为什么 Java 程序员学 TS 又快又慢

SelfBlog 恰好是一座「双语桥梁」：后端 Java 21，前端 TS 6。两边类型系统的同与异，值得专门对照一次——会 Java 的人学 TS 常常「三天上手，三个月才真正转弯」。

### 4.16.1 相同的心智，直接迁移

| 概念 | Java | TypeScript | 迁移提示 |
|------|------|-----------|---------|
| 接口约定 | `interface GuestCommentRequest { String nickname; }` | `interface GuestCommentRequest { nickname: string }` | 几乎一一对应 |
| 泛型 | `Result<T>`、`List<TagVO>` | `Result<T>`、`TagVO[]` | TS 泛型擦除方式不同但用法同 |
| 可选性 | `@Nullable` / Optional | `field?:` / `T \| undefined` | TS 的可选是**编译期强制的**，比注解真 |
| 联合场景 | sealed interface + pattern matching（Java 21） | 联合类型 + 收窄 | 4.7 节守卫 ↔ Java switch pattern |
| 访问控制 | private/public | private/public/#私有字段 | 语义相近 |

`SelfBlog 后端 Result<T> 信封`与前端 `api.get<Result<T>>` 能严丝合缝，正是因为两边泛型思维一致——**同一个类型在两次编译里各管一段**。

### 4.16.2 必须转弯的四个观念

1. **TS 类型只活在编译期**。Java 的泛型擦除至少反射还在；TS 编译成 JS 后**类型完全消失**，运行时没有任何痕迹。所以「运行时校验」必须另做（SelfBlog 用 zod 式手工守卫与后端 Bean Validation 各管一段）——类型再严也挡不住网络上来的脏数据。
2. **结构类型（structural typing）取代名义类型**。Java 里两个类字段完全相同也不能互赋（名义类型）；TS 里只要**形状匹配**就能赋值。这带来灵活性（POJO 互转免转换），也带来「鸭子类型的意外匹配」——`isDeleted` 字段都有的两个接口可能悄悄互通，改字段名要三思。
3. **联合类型是平面的**。Java 表达「成功或失败」要 sealed interface + 两个 record；TS 一个 `Result = Ok | Err` 就够，收窄靠 `if ('err' in r)`。写多了你会发现 **TS 的代数数据类型（ADT）比 Java 的类层级更轻**。
4. **null 语义两套体系**。Java 的 null 是运行时陷阱（NPE），靠 Optional 自律；TS 在 `strictNullChecks` 下把 null/undefined **纳入类型系统**，`T | undefined` 直接写进签名。后端 VO 里的可空字段（如 `post.summary`），到前端类型里应该显式 `string | null` 而不是 `string?`——前者逼调用方处理，后者容易漏。

### 4.16.3 一个贯穿两端的实战：状态码

后端 Java：`BusinessException` 携带 int code；前端 TS 需要同一套常量。SelfBlog 的做法是在 `types/` 里**收窄为字面量联合**而非裸 number：

```typescript
// 后端语义：40101 未登录 / 40301 无权限 / 40401 资源不存在（节选）
type BizCode = 200 | 400 | 401 | 403 | 404 | 429 | 500 | 40101 | 40301 | 40401

function isAuthError(code: number): code is 401 | 40101 {   // 类型守卫同时收窄值域
  return code === 401 || code === 40101
}
```

裸 `number` 时 `code === 40102` 这种手滑编译器不吭声；字面量联合让「拼错的状态码」直接编译报错。**把后端的魔法数字在前端建模成字面量联合**，是两端共享类型心智最便宜的一招——这也回答了一个常见疑问：有了运行时校验还需要类型吗？类型管「我写的代码对不对」，校验管「别人发来的数据对不对」，各司其职。

---

## 本章总结

这一章我们从「为什么需要类型」一路写到「重写整个 API 层」，把 SelfBlog 前端的 TypeScript 实践完整拆了一遍。用一张图回顾知识地图：

```text
类型基础            组合与抽象           类型体操（认读为主）      工程实战
────────           ─────────           ─────────────────       ─────────
原始类型/数组/元组   interface/type      条件类型/infers         tsconfig 严格模式
enum vs 字面量联合  extends/implements  映射类型/模板字面量       noUncheckedIndexedAccess
any vs unknown     泛型函数/接口/类      可辨识联合/守卫 is       模块与 .d.ts
void/never         工具类型六大件        装饰器（读得懂）          Vue3 类型集成
null/undefined     索引访问类型          ——                     API 层信封类型
```

必须带走的十句话：

1. **TS 是开发期的质检员**：类型擦除后运行时只有 JS，「可能为空」要靠类型建模而非祈祷。
2. **函数签名必须显式标注**，局部变量放心交给推断。
3. **`unknown` 替代 `any`**：不可信数据先验明正身再使用。
4. **对象用 interface，联合/计算类型用 type**，别教条但要一致。
5. **泛型保住「进出类型一致」的链路**；`ApiResponse<T>`/`PaginatedData<T>` 是 SelfBlog 的地基。
6. **可辨识联合把非法状态挡在类型之外**；穷尽检查用 `never` 兜底。
7. **`noUncheckedIndexedAccess` 让 `arr[0]` 诚实地返回 `T | undefined`**——用 `?.`、守卫、`??`，别滥用 `!`。
8. **Vue 3 里 `defineProps`/`defineEmits` 泛型写法 + `vue-tsc`** 才是完整的类型检查链。
9. **SelfBlog API 层的契约**：拦截器拆包后 `await` 拿到的就是 `{code, message, data}` 信封，调用方读 `res.data`，不是 `res.data.data`。
10. **类型是合同**：后端改字段，前端先改 `types/`——契约先行，编译器帮你把所有失联点找出来。

下一章（第六章《前端工程化》）将把镜头拉远：Vite 的构建流水线、ESLint/Prettier 闸门、Vitest 测试体系如何围绕本章的类型体系协同运转。

## 4.15 类型在真实项目中的组织：从单文件到类型层

小项目把类型随手写在组件里没问题；SelfBlog 的规模（5 个 store、30+ 组件、40+ 接口）已经需要**类型层**的自觉。这一节讲类型文件怎么组织、怎么演进。

### 4.15.1 types/ 目录的分层原则

```
frontend/src/types/
├── common.ts        # 全站地基：Result<T>、分页结构、通用枚举
├── post.ts          # 文章域：PostVO、PostDetail、PostRequest...
├── comment.ts       # 评论域
└── ...              # 一个业务域一个文件，与 api/ 目录平行对应
```

三条纪律：

1. **common.ts 只放跨域契约**：`Result<T>` 信封、`PageResult<T>`（records/total/size/current/pages）、时间戳类型别名。域类型往上放会导致循环依赖
2. **api 文件与类型文件一一对应**：`api/post.ts` 用的类型全部来自 `types/post.ts`——看 API 文件的 import 就知道它依赖哪些契约
3. **类型与后端同步演化**：后端 VO 加字段 → 前端类型同步加（可选字段用 `?` 标注过渡）→ `npm run type-check` 把所有失联点找出来。**类型是前后端之间的正式合同**

### 4.15.2 泛型工具的「自建层」：从 Result<T> 派生一切

SelfBlog 的 `types/common.ts` 展示了一个重要模式——**用泛型派生减少重复**：

```typescript
// 地基：后端信封的镜像
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 派生：分页数据是「信封里的 data」的固定形状
export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

// 使用侧：一个接口的类型 = Result<业务数据>，分页接口 = Result<PageResult<业务数据>>
export function getPostDetail(id: number) {
  return http.get<Result<PostVO>>(`/posts/${id}`)
}
export function getPostPage(pageNum: number) {
  return http.get<Result<PageResult<PostSummaryVO>>>('/posts', { params: { pageNum } })
}
```

注意 `Result<T = unknown>` 的**默认泛型参数**：忘了填 T 时退化为 unknown 而不是 any——忘了写类型会得到严格模式的保护，而不是静默放弃检查。这是「让错误发生在编译期」哲学的小小体现。

### 4.15.3 何时用 unknown 收敛边界：外部数据的检疫站

类型系统的铁律：**编译期类型只约束「你写的」，不约束「运行时进来的」**。localStorage、HTTP 响应、URL 参数都是外部数据，进系统前要过检疫：

```typescript
// ❌ 直接断言：后端改了结构，这里就是定时炸弹
const cfg = JSON.parse(localStorage.getItem('config')!) as SiteConfig

// ✅ 检疫模式：unknown 进 → 运行时验证 → 受信类型出
function isSiteConfig(v: unknown): v is SiteConfig {
  return (
    typeof v === 'object' && v !== null &&
    'siteName' in v && typeof (v as { siteName: unknown }).siteName === 'string'
  )
}
const raw: unknown = JSON.parse(localStorage.getItem('config') ?? 'null')
const cfg: SiteConfig | null = isSiteConfig(raw) ? raw : null   // 检疫失败给默认值
```

SelfBlog 的 `stores/site.ts` 在后端挂掉时页面照常渲染，靠的就是这个思路——**外部数据永远可能缺失或畸形，类型建模必须把「不可信」表达出来，而不是用 `as` 把它藏起来**。

### 4.15.4 从 JS 项目渐进迁移 TS 的路线图

很多读者面对的不是新项目而是存量 JS 项目。渐进迁移的顺序（SelfBlog 的骨架天然支持）：

1. **`allowJs: true` + `checkJs: false`**：TS 项目先接纳 .js 文件共存，不检查
2. **入口层先行**：`api/`、`types/`、`utils/` 这些被所有组件依赖的底层先 TS 化——它们的类型收益最大（调用方全部自动获得检查）
3. **`strict: false` 起步，逐项打开**：先 `strictNullChecks`（收益最大），再 `noImplicitAny`，最后 `noUncheckedIndexedAccess`（最严但最吵）
4. **Vue SFC 逐个转**：`<script setup lang="ts">` 一个个改，`vue-tsc --noEmit` 逐步消灭红色
5. **CI 闸门锁死**：type-check 进闸门后，TS 化的部分永远不会再退回 JS

**心法**：迁移的目标不是「100% TS」，而是「**边界类型 100% 准确**」。内部实现可以慢慢来，但 API 层、store 状态的形状必须先行——边界准确了，内部重构是安全的；边界含糊，内部再严谨也是裸奔。

## 参考资源

- **TypeScript 官方手册**（中文版）：https://www.typescriptlang.org/zh/docs/handbook/intro.html —— The Basics、Narrowing、Generics、Decorators 各章与本章一一对应
- **TSConfig 参考**：https://www.typescriptlang.org/tsconfig —— `strict`、`noUncheckedIndexedAccess`、`isolatedModules` 的权威说明
- **TypeScript 深水区：类型体操**：https://github.com/type-challenges/type-challenges —— 想精通条件类型/infer/模板字面量，从 easy 刷起
- **Vue 3 与 TypeScript**：https://cn.vuejs.org/guide/typescript/overview.html —— `defineProps`/`defineEmits` 泛型写法官方指南
- **vue-tsc 与 Vue 语言工具**：https://github.com/vuejs/language-tools —— `.vue` 类型检查的实现方
- **Axios 类型文档（源码即文档）**：https://github.com/axios/axios/blob/v1.x/index.d.ts —— `get<T, R>` 双泛型与 `isAxiosError` 守卫的原始出处
- **Total TypeScript 免费教程**：https://www.totaltypescript.com/tutorials —— Matt Pocock 的交互式练习，收窄与泛型章节尤其好
- **本项目参照代码**：`frontend/src/types/common.ts`、`frontend/src/api/index.ts`、`frontend/src/api/post.ts`、`frontend/tsconfig.json`、`frontend/env.d.ts` —— 本章所有真实例子的出处，读文档不如读自家代码
