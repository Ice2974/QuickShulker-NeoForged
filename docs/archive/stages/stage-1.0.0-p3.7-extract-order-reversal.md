# stage-1.0.0-p3.7-extract-order-reversal

本阶段把潜影盒 `extract` 顺序统一改为从最后一格到第一格。

## 变更范围

- 单次右键放出物品的 `EXTRACT` 改为倒序提取。
- `mouse dragged` 批量放出物品的 `MOUSE_DRAG_EXTRACT` 继续使用同样的倒序提取。
- `insert`、`pickup insert`、`transfer`、拖拽收纳顺序、数据安全修复和创造模式修复均不改动。
- 不实现 Bundle、末影箱 bundling、`rightClickClose` 或 reopen inventory。

## 代码变更

- Forge / NeoForge 的 `handleExtractCore(...)` 统一改走 `extractLastStack(...)`。
- 继续保留 `common` 里的倒序 helper 和相关单元测试。
- 没有调整拖拽会话、守恒校验、creative cursor 同步或宿主锁定逻辑。

## 验证

- `git diff --check`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`

## 结果

- `git diff --check` 通过，只有工作区换行格式警告。
- `:forge-1.20.1:compileJava` 通过。
- `:neoforge-1.21.1:compileJava` 通过。
- `:common:test` 通过。
- `build` 通过。

## 未改变

- mouse dragged 数据安全逻辑未改。
- 创造模式修复未改。
- 单次右键收纳未改。
- 非空槽合并放出未新增。
- 潜影盒到潜影盒拖拽转移未新增。

## 待确认

- 无。
