# stage-1.0.0-p3.6-mouse-dragged-extract-order

本阶段只调整 `mouse dragged` 批量放出物品的提取顺序。

## 变更范围

- `MOUSE_DRAG_EXTRACT` 改为从潜影盒内容最后一格向前扫描并放出物品。
- 单次 `EXTRACT` 继续保持原有从第一格开始的提取顺序。
- 拖拽收纳顺序、单次右键收纳、数据安全修复、创造模式修复、宿主锁定和 session 处理均不改动。
- 不实现 Bundle、末影箱 bundling、`rightClickClose` 或 reopen inventory。

## 代码变更

- `common` 新增 `ShulkerBundlingRules.extractLastStack(...)`，专供 mouse dragged extract 使用。
- Forge / NeoForge 的 `handleMouseDragExtract(...)` 切换到倒序提取 helper。
- 保留现有 `EXTRACT` 调用链，避免改变单次右键放出行为。
- 新增规则测试覆盖倒序提取返回最后一个非空槽位。

## 验证

- `git diff --check`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`

## 结果

- `git diff --check` 通过，只有工作区换行格式警告。
- `:forge-1.20.1:compileJava` 通过。
- `:neoforge-1.21.1:compileJava` 通过。

## 未改变

- 鼠标拖拽收纳顺序未变。
- 单次右键收纳未变。
- 单次 `EXTRACT` 未改为倒序。
- 任何非空槽合并放出未新增。
- 潜影盒到潜影盒拖拽转移未新增。

## 待确认

- 无。
