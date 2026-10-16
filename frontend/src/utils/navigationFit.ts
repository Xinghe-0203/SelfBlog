/** Count links that fit beside the fixed-size tools; reserve overflow control only when needed. */
export function fitNavigation(
  widths: number[],
  available: number,
  moreWidth: number,
  gap = 2
): number {
  const all = widths.reduce((sum, width) => sum + width, 0) + Math.max(0, widths.length - 1) * gap
  if (all <= available) return widths.length
  let used = moreWidth
  let count = 0
  for (const width of widths) {
    if (used + width + gap > available) break
    used += width + gap
    count++
  }
  return count
}
