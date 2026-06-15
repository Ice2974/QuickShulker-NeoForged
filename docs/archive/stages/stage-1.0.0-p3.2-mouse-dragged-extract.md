# stage-1.0.0-p3.2-mouse-dragged-extract

本阶段新增“鼠标携带单个潜影盒，右键按住拖过多个安全空槽，从潜影盒里依次批量放出物品”的 mouse dragged extract 路径。

## 实现内容

- `common` 新增 `ShulkerBundlingAction.MOUSE_DRAG_EXTRACT`，用于区分单次 `EXTRACT` 和拖拽批量放出。
- Forge 1.20.1 与 NeoForge 1.21.1 客户端拖拽状态机新增 `EXTRACT_FROM_CARRIED_SHULKER`。
- 首次右键点击安全空槽时，仍沿用现有单次 `EXTRACT`。
- 当 `supportsMouseDragged()` 和 `supportsBundlingExtract()` 同时开启时，后续拖过新的安全空槽会发送 `MOUSE_DRAG_EXTRACT`。
- 拖拽过程中继续复用 `DRAGGED_HOST_SLOTS`，避免同一 `HostSlotRef` 被重复处理。
- 鼠标松开、界面重建、非右键拖拽时，现有拖拽状态仍会清空。

## 服务端处理

- 双平台服务端均新增 `MOUSE_DRAG_EXTRACT` 分支。
- `MOUSE_DRAG_EXTRACT` 必须先检查：
  - `supportsMouseDragged()`
  - `supportsBundlingExtract()`
- 之后复用抽取出的 `handleExtractCore(...)`，不复制一套独立 extract 逻辑。
- 核心安全链路保持一致：
  - 服务端重新读取当前 `carried stack`
  - 服务端重新解析当前菜单和目标 `HostSlotRef`
  - 目标槽必须真实为空
  - 放出前调用 `HostSlotResolver.canSafelyReplace(player, hostSlot, extractedStack)`
  - 写回前再次确认目标槽仍为空
  - 写回时同时更新 carried shulker 和目标槽
- 当前 quick-open 宿主槽位仍拒绝作为目标槽。

## 当前支持的槽位

本阶段没有扩大 p3.1 的安全槽位 allowlist，仍与 `docs/stage-1.0.0-p3.1-bundling-menu-slots.md` 保持一致。

- 玩家主背包
- hotbar
- 副手
- 安全存储槽
- `InventoryMenu` 的 2x2 合成输入槽
- `CraftingMenu` 的 3x3 合成输入槽
- 铁砧输入槽
- 切石机输入槽

## 当前拒绝的槽位与场景

- 非空槽，不做合并
- 潜影盒到潜影盒拖拽转移
- 普通物品拖过多个潜影盒插入
- Bundle
- 末影箱 bundling
- `rightClickClose`
- `reopen inventory`
- 合成结果槽
- 熔炉结果槽
- 铁砧结果槽
- 交易结果槽
- 切石机结果槽
- 虚拟槽
- 未知第三方菜单槽位

## 修改文件

- `common/src/main/java/com/ice2974/quickshulkerneoforged/common/network/ShulkerBundlingAction.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `docs/stage-1.0.0-p3.2-mouse-dragged-extract.md`

## 验证命令与结果

- `git diff --check`
  - 通过，仅有 Git 对工作区行尾将转换为 `CRLF` 的提示，无 diff 格式错误
- `.\gradlew.bat :forge-1.20.1:compileJava`
  - 通过
- `.\gradlew.bat :neoforge-1.21.1:compileJava`
  - 通过

本阶段未运行 `:forge-1.20.1:build` 和 `:neoforge-1.21.1:build`，因为本次改动集中在 Java 源码层，按任务要求优先完成双平台 `compileJava` 验证。

## 待人工确认项

- Minecraft 游戏内实际拖拽表现
- 多人服务器实机测试结果
- 创造模式下连续拖拽 extract 的最终交互体验是否符合预期
- 本阶段新增 action 是否需要同步更新第三方来源说明，待维护者人工确认
