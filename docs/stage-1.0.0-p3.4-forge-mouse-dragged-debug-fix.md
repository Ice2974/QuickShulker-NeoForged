# 阶段 3.4 Forge 1.20.1 mouse dragged 调试修复

## 查看日志

本次重点查看：

- `D:\Project\QuickShulkerNeoForged\.minecraft\versions\1.20.1\logs\debug.log`
- 对照查看：`D:\Project\QuickShulkerNeoForged\.minecraft\versions\1.21.1\logs\debug.log`

检索关键词包括：

- `quickshulker`
- `MOUSE_DRAG`
- `dragId`
- `containerId`
- `carried`
- `setCarried`
- `getCarried`
- `slot`
- `duplicate`
- `processed`
- `conservation`
- `reject`
- `broadcast`
- `exception`
- `warn`
- `error`

## 1.20.1 debug.log 关键线索

1. 13:09 和 13:16 附近，Forge 1.20.1 日志中能看到多次普通 bundling 请求，例如 `PICKUP_INSERT`、`EXTRACT`。
2. 同一批日志中没有看到客户端发送或服务端处理 `MOUSE_DRAG_PICKUP_INSERT` / `MOUSE_DRAG_EXTRACT`，说明当前代码把拖拽延续路径挡掉了，这与“1.20.1 无法使用拖拽功能”的反馈一致。
3. 普通 bundling 请求后服务端出现 `Forge creative bundling end_mouse_drag cleared server-only carried`，说明上一轮修复已经恢复了 `END_MOUSE_DRAG` 收尾，creative server-only carried 不再明显跨拖拽残留。
4. 日志中多次出现 `Failed to unwrap creative slot wrapper for shulker bundling.`，说明 Forge 1.20.1 creative slot wrapper 解析仍有噪声；本次没有把它作为拖拽不可用的直接根因。

## 请求顺序还原

基于当前日志和源码，可以还原出本轮失败顺序：

1. 客户端右键按下，发送一次普通 `PICKUP_INSERT` 或 `EXTRACT`。
2. 客户端进入本模组拖拽状态，并为本次操作生成 `dragId`。
3. `MouseDragged.Pre` 被 `suppressBundlingMouseDragUntilRelease` 直接取消，所以拖过后续槽位时没有发送 `MOUSE_DRAG_*` continuation。
4. 释放右键时发送 `END_MOUSE_DRAG`，服务端清理 server-only carried。
5. 结果是单次按下的槽位可能生效，但拖拽经过的后续槽位完全不处理，表现为 Forge 1.20.1 拖拽功能不可用。

## 实际根因

上一轮为了避免 Forge 1.20.1 快速拖拽复制 / 消失，临时把客户端 `MouseDragged.Pre` 全部取消，并且服务端拒绝所有 `MOUSE_DRAG_*`。这确实绕开了高风险逐槽逻辑，但也直接禁用了潜影盒拖拽收纳 / 放出。

本轮继续查看最新 1.20.1 `debug.log` 后，确认恢复拖拽 continuation 后仍有一个 Forge 创造模式专属数据安全根因：

Forge 1.20.1 创造模式下，客户端每个 mouse dragged continuation 包都会携带当前界面看到的 `screenCarried` 潜影盒快照。快速拖拽时，这个 payload 可能仍是拖拽开始时的旧潜影盒；如果服务端每个 continuation 都重新信任该 payload，就会用旧潜影盒反复计算。

- 拖拽收纳时，后一个槽位基于旧潜影盒写回，会覆盖前一个槽位刚写入的内容，表现为目标物品被扣减但潜影盒只增加最后一次或部分内容。
- 拖拽放出时，后一个槽位仍基于旧潜影盒提取，会把同一份内容放到多个空槽，而潜影盒最终只减少一次或部分内容。

因此最终修复不能只依赖 `dragId + processedSlots` 去重；Forge 创造模式拖拽会话还必须在首包之后改用服务端已经更新过的 `menuCarried` 作为唯一真实潜影盒状态。

## 修复方式

只修改 Forge 1.20.1：

1. 客户端保留拖拽会话和 `dragId`，允许 `MouseDragged.Pre` 继续调用 `trySendMouseDraggedBundlingIntent`。
2. 客户端在同一次按住右键直到释放期间，只拦截重复的 `MouseButtonPressed.Pre`，避免快速拖拽过程中重复发送普通 `PICKUP_INSERT` / `EXTRACT`。
3. 服务端恢复处理 `MOUSE_DRAG_PICKUP_INSERT` 和 `MOUSE_DRAG_EXTRACT`，继续通过 `containerId`、`dragId` 和 `processedSlots` 去拒绝旧包、重复包和重复槽位。
4. 服务端继续拒绝 `MOUSE_DRAG_INSERT`，不新增拖拽合并普通 carried stack 到非空槽的行为。
5. 服务端为重复 mouse dragged 槽位增加 debug 日志，后续复测可以直接确认是否出现同一 dragId + 同一槽位重复成功处理。
6. Forge 创造模式拖拽会话首包仍使用客户端上传的 carried 潜影盒来建立服务端状态；首包成功写回后，后续同一 `dragId` 的 continuation 改用服务端 `menuCarried` 中的最新潜影盒，不再使用旧客户端 payload 反复计算。
7. 非拖拽的创造模式普通 bundling 仍保持原逻辑，继续使用本次点击上传的 carried payload；本次修改只收窄到带 `dragId` 的 mouse dragged session。

## NeoForge 影响

不修改 NeoForge 1.21.1。

NeoForge 仍保留当前 mouse dragged 实现，本次只通过 `:neoforge-1.21.1:compileJava` 做回归编译确认。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `docs/stage-1.0.0-p3.4-forge-mouse-dragged-debug-fix.md`

## 验证命令

需要执行：

- `git diff --check`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat --console=plain :neoforge-1.21.1:compileJava`

本次没有修改网络包或 common 类，按任务要求不额外运行双平台 build。

## 验证结果

- `git diff --check`：通过，仅有 CRLF 工作区提示。
- `.\gradlew.bat :forge-1.20.1:compileJava`：通过，`BUILD SUCCESSFUL in 9s`。
- `.\gradlew.bat --console=plain :neoforge-1.21.1:compileJava`：通过，`BUILD SUCCESSFUL in 952ms`。
- 未运行 `:forge-1.20.1:build` / `:neoforge-1.21.1:build`：本次没有修改网络包或 common 类。

## 待人工确认项

- 需要维护者在 Forge 1.20.1 实机确认：快速拖拽时后续槽位恢复处理，并且不再出现复制 / 消失。
- 无法仅通过编译确认 Minecraft 游戏内行为。
- 无法仅通过本地日志确认多人服务器场景。
