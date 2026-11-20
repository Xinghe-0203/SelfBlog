import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * 样式入口的导入顺序是有语义的，不是随便排的：
 *
 * - `customize.css` 必须是**倒数第二个**（作者配置要压过皮肤与深色模式里同优先级的声明）；
 * - `print.css` 必须是**最后一个**（打印分支要能覆盖以上所有屏幕样式）。
 *
 * 这两条在普通浏览器里看不见（print 分支只在打印时生效，作者配置的覆盖面也无法肉眼验证），
 * 一旦被调换只会以「打印出来还是深色」「换了皮肤没生效」的形式出现。
 * 放在 `test:deployment` 里是因为这里能用 node 的 fs 直接读源码文本
 * （Vitest 的 `?raw`/`?inline` 对 `.css` 返回空串，读不到内容）。
 */
const here = dirname(fileURLToPath(import.meta.url))
const mainCssPath = join(here, '..', '..', 'src', 'styles', 'main.css')
const printCssPath = join(here, '..', '..', 'src', 'styles', 'print.css')

/** 从 main.css 里解析出 `@import './x.css';` 的文件名（保持原有顺序）。 */
async function importedModules() {
  const source = await readFile(mainCssPath, 'utf8')
  const matches = source.matchAll(/@import\s+'\.\/([^']+)';/g)
  return [...matches].map((match) => match[1])
}

test('main.css导入顺序：print.css 必须最后，customize.css 倒数第二', async () => {
  const names = await importedModules()
  assert.ok(names.length >= 15, `应导入全部样式模块，实际 ${names.length} 个`)
  assert.equal(names.at(-1), 'print.css', 'print.css 必须是最后一个导入')
  assert.equal(names.at(-2), 'customize.css', 'customize.css 必须紧挨在 print.css 之前')
  assert.equal(names[0], 'variables.css', '设计 token 必须最先导入')
  assert.equal(new Set(names).size, names.length, '同一个模块不能被导入两次')
})

test('print.css 的规则全部落在 @media print 内', async () => {
  const source = await readFile(printCssPath, 'utf8')
  // 去掉块注释与 @page（@page 本身只在打印时生效），剩下的必须整体位于 @media print 里。
  const withoutComments = source.replace(new RegExp('/\\*[\\s\\S]*?\\*/', 'g'), '')
  const pageBlock = withoutComments.match(/@page\s*\{[\s\S]*?\}/)
  assert.ok(pageBlock, 'print.css 应声明 @page')
  const outsideBlock = withoutComments
    .replace(new RegExp('@page\\s*\\{[\\s\\S]*?\\}'), '')
    .replace(new RegExp('@media print\\s*\\{[\\s\\S]*$'), '')
    .trim()
  assert.equal(outsideBlock, '', 'print.css 里不允许有落在 @media print 之外的规则')
})
