/**
 * 剪贴板写入的薄封装。
 *
 * 浏览器的 `navigator.clipboard` 只在安全上下文（HTTPS / localhost）可用，
 * 而且会被用户权限拒绝；两个场景都要区别对待：
 *
 * - 成功 → 返回 true，调用方可以给「已复制」的正反馈；
 * - 不可用或被拒 → 返回 false，调用方**不要**假装成功。
 *
 * 非安全上下文下的兜底是 `document.execCommand('copy')`：它在旧浏览器上仍然可用，
 * 选中一个临时 textarea 再执行复制。这里刻意不吞异常：调用方需要知道失败。
 */
export async function copyText(text: string): Promise<boolean> {
  const value = String(text ?? '')
  if (!value) return false

  try {
    if (typeof navigator !== 'undefined' && navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(value)
      return true
    }
  } catch {
    // 权限被拒 / 非安全上下文：落到下面的 execCommand 兜底。
  }

  if (typeof document === 'undefined') return false
  try {
    const area = document.createElement('textarea')
    area.value = value
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.top = '-1000px'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    const ok = document.execCommand('copy')
    document.body.removeChild(area)
    return ok
  } catch {
    return false
  }
}
