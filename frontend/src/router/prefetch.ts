/**
 * 公开路由的代码分块预取。
 *
 * 公开页面全部是懒加载 chunk（见 `router/modules/*.ts`）：第一次点进「归档」
 * 要现场下载那个 chunk，在移动网络上是能感觉到的等待。这里做两件事：
 *
 * 1. **空闲预取**：首屏渲染完成后，在浏览器空闲时段把公开页面的 chunk 全部拉回来。
 *    用户点导航时基本是零等待。
 * 2. **悬停预取**：鼠标悬停/键盘聚焦到站内链接时，只预取那个目标页面的 chunk。
 *    这一条比全量预取更早发生（用户还在犹豫要不要点），对「首屏后第一次点击」
 *    的收益最大。
 *
 * 三条约束：
 * - **只预取公开路由**。后台 chunk 只在登录后才有意义，提前下载等于把后台代码
 *   发给每一个未授权访客。
 * - **不与首屏争抢**。全量预取走 `requestIdleCallback`；悬停预取只在指针真正
 *   落到链接上时触发。
 * - **失败静默**。预取是纯优化：拉不到就等用户真的点进去时再拉，绝不报错。
 */

/** 一个公开页面对应的 chunk 加载器与路径匹配规则。 */
interface PublicChunk {
  /** 该 chunk 覆盖的站内路径（精确匹配或前缀匹配）。 */
  match: (path: string) => boolean
  /** 真正加载 chunk 的动态 import（必须与路由里用的是同一个模块）。 */
  load: () => Promise<unknown>
}

const PUBLIC_CHUNKS: PublicChunk[] = [
  { match: (path) => path === '/', load: () => import('@/views/Home.vue') },
  { match: (path) => path === '/archives', load: () => import('@/views/Archives.vue') },
  { match: (path) => path === '/about', load: () => import('@/views/About.vue') },
  { match: (path) => path === '/gallery', load: () => import('@/views/gallery/Gallery.vue') },
  { match: (path) => path === '/search', load: () => import('@/views/search/Search.vue') },
  { match: (path) => path === '/tags', load: () => import('@/views/TagsIndex.vue') },
  { match: (path) => path.startsWith('/post/'), load: () => import('@/views/post/PostDetail.vue') },
  { match: (path) => path.startsWith('/tag/'), load: () => import('@/views/tag/TagDetail.vue') },
  {
    match: (path) => path.startsWith('/pages/'),
    load: () => import('@/views/pages/ContentPage.vue')
  }
]

/** 同一个 chunk 只发起一次加载：预取与真正的路由加载共享同一份模块缓存。 */
const loading = new Map<PublicChunk, Promise<unknown>>()

function loadChunk(chunk: PublicChunk): void {
  if (loading.has(chunk)) return
  loading.set(
    chunk,
    chunk.load().catch(() => {
      // 预取失败（离线 / 404 / 构建产物不一致）不影响用户：真正点进去时
      // 路由自己的 lazyLoad 会再试一次并给出错误提示。
      loading.delete(chunk)
      return undefined
    })
  )
}

/** 按站内路径找到对应的公开 chunk；找不到（例如后台路径）返回 null。 */
export function chunkForPath(path: string): PublicChunk | null {
  const clean = path.replace(/[?#].*$/, '')
  return PUBLIC_CHUNKS.find((chunk) => chunk.match(clean)) ?? null
}

/** 预取某个站内路径对应的 chunk；不是公开页面时什么都不做。 */
export function prefetchPath(path: string): void {
  const chunk = chunkForPath(path)
  if (chunk) loadChunk(chunk)
}

/**
 * 空闲时预取全部公开页面 chunk。
 *
 * `requestIdleCallback` 在 Safari 上不存在，用 `setTimeout` 兜底；
 * 两者都不可用（极旧环境）时直接放弃预取 —— 它只是优化。
 */
export function prefetchPublicRoutes(): void {
  if (typeof window === 'undefined') return
  const run = () => {
    for (const chunk of PUBLIC_CHUNKS) loadChunk(chunk)
  }
  const idle = (window as Window & { requestIdleCallback?: (cb: () => void) => void })
    .requestIdleCallback
  if (typeof idle === 'function') idle(run)
  else window.setTimeout(run, 1200)
}

/**
 * 悬停 / 聚焦预取。
 *
 * 用事件委托而不是给每个链接绑监听：站内链接遍布页面，委托只需要一个监听器，
 * 且对后加的内容同样生效。
 *
 * 只处理**同源站内绝对路径**：外链（`https://…`）、协议相对（`//…`）与
 * 锚点（`#…`）都不该触发任何站内预取。
 */
export function installRoutePrefetch(): () => void {
  if (typeof document === 'undefined') return () => {}

  const onPointer = (event: Event): void => {
    const target = event.target
    if (!(target instanceof Element)) return
    const anchor = target.closest('a[href]')
    if (!(anchor instanceof HTMLAnchorElement)) return
    const href = anchor.getAttribute('href')
    if (!href || !href.startsWith('/') || href.startsWith('//')) return
    prefetchPath(href)
  }

  document.addEventListener('pointerenter', onPointer, true)
  document.addEventListener('focusin', onPointer, true)
  return () => {
    document.removeEventListener('pointerenter', onPointer, true)
    document.removeEventListener('focusin', onPointer, true)
  }
}
