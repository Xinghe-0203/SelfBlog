# 评论限流与会话安全实施计划

对应设计：`docs/superpowers/specs/2026-10-17-comment-session-security.md`。

- [ ] 修正路径段匹配及最长规则优先；新增单元测试与真实评论创建路径测试。
- [ ] JWT 增加密码绑定版本，登录绑定已验证快照，刷新继承旧版本并检查撤销。
- [ ] 禁止通用用户实体更新密码，专用条件更新覆盖改密、两种重置和旧样例密码初始化；登录只更新失败字段。
- [ ] 新增旧令牌、重启实例、并发凭据变化、三种密码操作及旧实体写回的回归测试。
- [ ] 更新 README、后端 README、部署说明及 CHANGELOG；运行针对性测试和全量后端测试，检查最终差异。

验证命令（PowerShell，工作目录 `backend/`）：

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd test "-Dskip.npm=true" "-Dskip.installnodenpm=true"
```

禁止修改现有验收断言以掩盖失败；仅调整因显式接口变动而必要的测试桩，并新增真实业务回归。测试库和临时 SQLite 数据与作者本机数据隔离。
