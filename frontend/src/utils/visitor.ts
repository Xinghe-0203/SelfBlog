const VISITOR_ID_KEY = 'selfblog-visitor-id'

export function getVisitorId(): string {
  try {
    let visitorId = localStorage.getItem(VISITOR_ID_KEY)
    if (!visitorId) {
      visitorId = globalThis.crypto?.randomUUID?.().replace(/-/g, '')
      if (!visitorId) {
        const bytes = new Uint8Array(24)
        globalThis.crypto?.getRandomValues(bytes)
        visitorId = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
      }
      localStorage.setItem(VISITOR_ID_KEY, visitorId)
    }
    return visitorId
  } catch {
    return 'session-' + Math.random().toString(36).slice(2, 18)
  }
}
