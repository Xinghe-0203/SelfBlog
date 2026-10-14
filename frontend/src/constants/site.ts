/** Site identity, overridable through the frontend environment file. */
export const SITE_TITLE = import.meta.env.VITE_APP_TITLE?.trim() || '个人博客'

export const SITE_DESCRIPTION = '记录技术实践、阅读摘记与生活思考。'
