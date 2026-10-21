/**
 * 并发请求的「最新一次生效」令牌。
 *
 * 翻页、切标签、切文章都会让多个请求同时在飞：先发的请求后到，就会把新结果
 * 覆盖回旧内容（或把旧内容覆盖成新结果）。这里给每次请求领一个单调递增的
 * 令牌，只有令牌仍是当前值时才允许写状态，晚到的旧响应直接丢弃。
 *
 * 必须在组件 setup() 内调用；同一个组件里互不相关的数据（正文 / 留言）应各自
 * 调用一次，避免一个请求把另一个请求判成过期。
 */
export function useLatestRequest(): { begin: () => () => boolean } {
  let latest = 0

  /** 领取本次请求的令牌；返回的函数在「本次仍是最新」时返回 true。 */
  const begin = (): (() => boolean) => {
    const token = ++latest
    return () => token === latest
  }

  return { begin }
}
