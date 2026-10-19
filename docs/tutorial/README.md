# SelfBlog 全栈开发完全教程

以 [SelfBlog](https://github.com/Xinghe-0203/SelfBlog)（Spring Boot 3.3 + Java 21 + MyBatis Plus + Vue 3.5 + TypeScript + Pinia 真实上线项目）为标本的系统性全栈教程。

## 文件清单

| 文件 | 内容 | 约篇幅 |
|------|------|--------|
| [SelfBlog-全栈开发完全教程.md](./SelfBlog-全栈开发完全教程.md) | **全部内容合并版（推荐从头读这份）** | ≈117 万字节 / 25.6 万字 |
| [_front.md](./_front.md) | 前言、学习路径、目录 | — |
| [ch01-java-basics.md](./ch01-java-basics.md) | 第一章 Java 语言基础（语法/OOP/集合/异常/IO/Lambda/Maven/JUnit） | ≈6.2 万字符 |
| [ch02-java-advanced.md](./ch02-java-advanced.md) | 第二章 Java 进阶（并发实战/JVM/反射/JDBC/设计模式/虚拟线程） | ≈7.1 万字符 |
| [ch03-spring-boot.md](./ch03-spring-boot.md) | 第三章 Spring Boot（IoC/AOP/MVC/MyBatis Plus/事务/Security/缓存/事件机制） | ≈4.9 万字符 |
| [ch04-typescript.md](./ch04-typescript.md) | 第四章 TypeScript（类型思维/泛型/守卫/strict 配置/Java 对照/错误急救） | ≈6.4 万字符 |
| [ch05-vue3.md](./ch05-vue3.md) | 第五章 Vue 3（组合式 API/Router/Pinia/拦截器/组合式函数模式/启动顺序） | ≈7.6 万字符 |
| [ch06-frontend-engineering.md](./ch06-frontend-engineering.md) | 第六章 前端工程化（Vite/ESLint/Vitest/CI/浏览器与 HTTP 底层/部署/真实踩坑实录） | ≈5.6 万字符 |
| [ch07-project-practice.md](./ch07-project-practice.md) | 第七章 项目实战（全链路功能剖析/测试/部署/架构复盘/48 小时上手路线图） | ≈7.2 万字符 |
| [appendix-a-errors.md](./appendix-a-errors.md) | 附录A 常见错误排查手册（方法论 + 后端 30 条/前端 21 条/环境 10 条 + 错误码速查总表 + 六个完整排查实录） | — |
| [appendix-b-source-map.md](./appendix-b-source-map.md) | 附录B 源码导读地图（逐包导读+主题路线）+ 全栈术语表（190+ 条） | — |
| [appendix-c-interview.md](./appendix-c-interview.md) | 附录C 面试题精选 90 题 + 自测 35 题与答案要点 | — |
| [appendix-d-toolchain.md](./appendix-d-toolchain.md) | 附录D 工具链实战（环境搭建/IDEA/调试/SQLite 直查/Git 事故恢复） | — |

## 学习路径

完全新手：ch01 → ch02 → ch03 → ch04 → ch05 → ch06 → ch07 → 附录（预计 8–12 周）。
会 Java 学前端 / 会前端补后端的走法见前言「三种读者，三种走法」。

## 常见问题（FAQ）

**Q：一定要按章节顺序读吗？**
不必。ch01–ch03 是后端线，ch04–ch06 是前端线，两线只共享少量概念；ch07 与附录才是「合流点」。有基础的读者可按前言的「三种走法」跳跃阅读，每章开头有导学说明前置要求。

**Q：只看教程不跑项目，能学会吗？**
学不扎实。本教程所有案例都锚定在 SelfBlog 真实源码上——亲手把项目跑起来（第七章 §7.2 有 48 小时上手路线图）、跟着示例点进真实文件，效果是完全不同的。教程里的 ⚠️ 段落尤其值得对照源码验证。

**Q：教程里的代码可以直接抄进自己的项目吗？**
示例代码以 SelfBlog 真实源码为原型、为教学做了简化，能跑但未必是「组件化最完整」的形态。理解每段代码背后的决策（为什么这么拆、为什么加锁、为什么先消毒）比复制粘贴更有价值。

**Q：版本升级后教程会过时吗？**
技术栈主版本（Spring Boot 3.3 / Java 21 / Vue 3.5 / TS 6）在可见周期内稳定，教程侧重**长期成立的原理与决策**；个别 API 细节以仓库实际代码为准，阅读时以 `pom.xml` / `package.json` 与 `CHANGELOG.md` 为最新依据。

## 阅读约定

- 所有代码示例均以 SelfBlog 仓库真实源码为原型，标注 ⚠️ 的段落是真实踩坑记录。
- 教程配套项目版本 v2.0.26；行为以仓库 `CHANGELOG.md` 为准。
