import { describe, expect, it } from 'vitest'
import { fitNavigation } from '@/utils/navigationFit'
describe('navigation width budget', () => {
  it('does not reserve a more button if all links fit', () => {
    expect(fitNavigation([70, 70, 70, 70], 286, 68)).toBe(4)
  })
  it('reserves the more button and gaps rather than squeezing tool targets', () => {
    expect(fitNavigation([70, 70, 70, 200, 200], 286, 68)).toBe(3)
    expect(fitNavigation([200, 200], 180, 68)).toBe(0)
  })
})
