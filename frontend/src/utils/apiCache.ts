/**
 * 公开只读接口的**短期内存缓存**。
 *
 * 目的只有一个：让「回到上一页」这种高频动作不产生网络往返。
 * SPA 里从文章页退回首页、从归档页切到搜索再切回来，都会重新打同一组公开接口；
 * 这些接口的数据在几十秒内不会变，重新拉一遍只是白等一次 RTT。
 *
 * 三条边界，缺一不可：
 *
 * 1. **只缓存公开、只读、幂等的 GET**。带副作用的写操作、后台接口、
 *    以及**带用户身份差异**的接口（点赞状态、留言）一律不进缓存 ——
 *    缓存了会出现「A 看到 B 的数据」这种比慢更糟的问题。
 * 2. **必须有失效入口**。后台改了内容（发布/删除/改标签）要能清掉对应条目，
 *    否则作者会看到自己刚改完的旧列表。`invalidateApiCache` 就是这个入口。
 * 3. **失败不缓存**。请求抛错时既不写入也不复用，直接把错误抛给调用方。
 *
 * 缓存是**进程内**的（刷新页面即失效），因此不存在跨用户串数据的可能：
 * 每个浏览器标签页一份，互不影响。
 */

/** 缓存条目：值 + 过期时间戳。 */
interface CacheEntry<T> {
  value: T
  expiresAt: number
}

/** key → 条目。key 必须包含全部影响结果的参数（分页、关键词……）。 */
const store = new Map<string, CacheEntry<unknown>>()

/** key → 在途请求。用于并发去重：同一 key 的并发请求只发一次。 */
const inflight = new Map<string, Promise<unknown>>()

/** 清理过期条目。条目数量不大，但长时间停留的标签页不该一直留着它们。 */
function sweep(now: number): void {
  for (const [key, entry] of store) {
    if (entry.expiresAt <= now) store.delete(key)
  }
}

/**
 * 带缓存地读取一个公开接口。
 *
 * @param key 缓存键。必须能唯一确定返回内容。
 * @param ttlMs 缓存有效期（毫秒）。
 * @param fetcher 真正的取数函数（必须是 GET 语义）。
 */
export async function cachedGet<T>(
  key: string,
  ttlMs: number,
  fetcher: () => Promise<T>
): Promise<T> {
  const now = Date.now()
  const hit = store.get(key)
  if (hit && hit.expiresAt > now) return hit.value as T

  const pending = inflight.get(key)
  if (pending) return pending as Promise<T>

  const request = fetcher()
    .then((value) => {
      store.set(key, { value, expiresAt: Date.now() + ttlMs })
      if (store.size > 64) sweep(Date.now())
      return value
    })
    .finally(() => {
      inflight.delete(key)
    })

  inflight.set(key, request)
  return request
}

/**
 * 让指定前缀的缓存条目失效。
 *
 * 后台发布 / 删除 / 改标签 / 改设置之后调用一次，公开页面下一次读取就会重新取数。
 * 前缀匹配而不是全量清空：清全量也能工作，但会把「归档」这种几乎不变的数据
 * 一起丢掉，白白多打一次请求。
 */
export function invalidateApiCache(prefix: string): void {
  for (const key of store.keys()) {
    if (key.startsWith(prefix)) store.delete(key)
  }
}

/** 清空全部缓存（退出登录、切换站点等极端场景）。 */
export function clearApiCache(): void {
  store.clear()
}
