# stage-1.0.0-p3.3-mouse-dragged-data-safety

## 阶段目标

本阶段处理 1.0.0 发布前 mouse dragged shulker bundling 的数据安全问题，重点覆盖快速右键拖拽收纳 / 放出时的吞物品、复制物品、旧 packet 和旧 carried stack 风险。

本项目仍保持逐槽 packet 处理，不改为 batch，不实现 Bundle、末影箱 bundling 或其他新功能。

## 阶段 3.3 已完成的修复

- `ShulkerBundlingIntent` 携带 `containerId`。
- `ShulkerBundlingIntent` 携带 `dragId`。
- Forge / NeoForge packet / payload 编解码同步传递 `containerId` 和 `dragId`。
- Forge 使用 `consumerMainThread`，NeoForge 使用 `context.enqueueWork`，确保 bundling 菜单和物品读写在服务端线程执行。
- 服务端按玩家 UUID 保存 DragSession，用于记录 `containerId`、`dragId`、`processedSlots` 和 `lastSeenGameTime`。
- 同一 dragId 下同一 action + HostSlotRef 重复请求通过 `processedSlots` 去重。
- 拖拽收纳写回前执行数量守恒校验：目标槽减少数量必须等于 carried shulker 内容增加数量。
- 拖拽放出写回前执行数量守恒校验：放出 stack 数量必须等于 carried shulker 内容减少数量。
- 服务端收到 bundling 请求后先校验 packet `containerId` 必须匹配当前 `player.containerMenu.containerId`。
- screen close、鼠标释放、screen init、menu/containerId 变化时，客户端发送 `END_MOUSE_DRAG` 清理服务端 DragSession。

## 本补丁发现的问题

阶段 3.3 的 containerId、dragId、processedSlots、守恒校验和服务端线程处理方向正确，可以明显降低 Forge 1.20.1 快速拖拽吞物品 / 复制物品主问题。

本补丁继续确认以下残留 / 回归风险：

- 旧实现中服务端 `DragSession.carried` 可能作为旧 carried shulker 副本被继续使用。如果关闭背包或菜单变化后旧拖拽包仍到达，且 `containerId` 仍匹配，尤其是 `InventoryMenu containerId=0`，服务端可能继续用旧 carried 副本处理请求。
- 仅靠 `containerId=0` 不能完全识别 InventoryMenu screen close 后的旧拖拽请求，必须结合 dragId 生命周期、END_MOUSE_DRAG 清理和当前真实 carried 校验。
- 2026-06-13 日志回归显示：NeoForge 1.21.1 的 `END_MOUSE_DRAG` 会发送空 cursor stack，但 payload 使用 `ItemStack.STREAM_CODEC`，导致 `EncoderException: Empty ItemStack not allowed` 并断开连接。
- 2026-06-13 日志回归显示：创造模式右键收纳 / 放出后，如果服务端把用于同步的临时 carried shulker 留在 `InventoryMenu`，关闭背包时会额外结算出一个潜影盒副本。
- 2026-06-13 后续 debug.log 显示：如果在创造模式 bundling 同步后立刻清空服务端 carried 并广播 full state，客户端鼠标上的潜影盒会被同步为空，表现为潜影盒消失，拖拽功能也无法继续使用。

## 本补丁修复策略

- DragSession 不再保存 carried `ItemStack`。
- DragSession 只保留：
  - `containerId`
  - `dragId`
  - `processedSlots`
  - `lastSeenGameTime`
- 生存 / 普通服务端拖拽路径每次都重新读取当前真实 `player.containerMenu.getCarried()`。
- `resolvedCarried(..., dragSession)` 不再从 DragSession 返回 carried 副本。
- `carriedStillMatches(..., dragSession)` 不再比较 session carried，而是重新比较当前真实 `player.containerMenu.getCarried()`。
- `setCarried(..., dragSession)` 只更新 `player.containerMenu.setCarried(updated)`，不把 updated carried 写入 DragSession。
- 拖拽延续包 `MOUSE_DRAG_PICKUP_INSERT` / `MOUSE_DRAG_EXTRACT` 到达时如果没有匹配的 active DragSession，服务端直接拒绝，不为旧延续包重建 session。
- 服务端收到 `END_MOUSE_DRAG` 后按 `containerId + dragId` 清理匹配 DragSession。
- NeoForge shulker bundling payload 改用 `ItemStack.OPTIONAL_STREAM_CODEC`，允许 `END_MOUSE_DRAG` 携带空 cursor stack。
- 创造模式 bundling 操作完成后不再立刻广播空 carried。服务端保留临时 carried 直到客户端发送 `END_MOUSE_DRAG`，然后仅在服务端清空该临时 carried，不向客户端广播空鼠标栈。
- `clearDragSession` 收尾路径也会清理创造模式服务端临时 carried，用于登出、死亡、切维度等异常收尾。

## 创造模式策略

本补丁恢复创造模式 mouse dragged 收纳 / 放出。

创造模式与生存模式的 carried 来源不同：

- 生存 / 普通服务端路径继续以服务端当前 `player.containerMenu.getCarried()` 为权威状态。
- 创造模式的服务端 `InventoryMenu` 可能没有表达客户端 CreativeModeInventoryScreen 的临时鼠标栈，因此创造模式 bundling 继续使用客户端上传的 `cursorStack` 作为创造界面临时 cursor 表达。
- 创造模式 DragSession 仍不保存 carried，也不把 DragSession 当作实际写回来源。
- 创造模式操作后会先向客户端同步更新后的 carried shulker，保证右键收纳 / 放出和后续拖拽可以继续使用。
- 创造模式鼠标右键释放 / screen close / menu change 触发 `END_MOUSE_DRAG` 后，服务端只清理自己的临时 carried，避免关闭背包时复制潜影盒，同时不广播空 carried，避免客户端潜影盒消失。

## 未实现范围

- Bundle。
- 末影箱 bundling。
- `rightClickClose`。
- `reopen inventory`。
- 向非空槽合并放出。
- 潜影盒到潜影盒拖拽转移。

## 修改文件

- `common/src/main/java/com/ice2974/quickshulkerneoforged/common/network/ShulkerBundlingAction.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/network/NeoForgeShulkerBundlingPayload.java`
- `docs/stage-1.0.0-p3.3-mouse-dragged-data-safety.md`

## 验证命令和结果

本节只记录已经运行的命令；未运行的命令不能写成已通过。

- `git diff --check`
  - 通过；仅有 Git 工作区 LF/CRLF 提示。
- `.\gradlew.bat :forge-1.20.1:compileJava`
  - 通过。
- `.\gradlew.bat :neoforge-1.21.1:compileJava`
  - 通过；仅有既有 deprecated API 提示。
- `.\gradlew.bat :forge-1.20.1:build`
  - 通过。
- `.\gradlew.bat :neoforge-1.21.1:build`
  - 通过。

## 未验证内容

- 尚未进行 Minecraft 客户端内真实拖拽实机测试。
- 尚未进行 Forge / NeoForge 专用服务器多人测试。
- 尚未确认创造模式恢复 mouse dragged 后的最终玩家体验是否需要继续调整。
- 尚未确认许可证 / NOTICE 是否需要因参考原作思路而更新。

## 待人工确认项

- Forge 1.20.1 与 NeoForge 1.21.1 实机快速拖拽收纳 / 放出是否在生存、创造和服务器环境下均无吞物品、复制物品、潜影盒消失。
- 快速拖拽过程中关闭背包、切换菜单、死亡、掉线、切维度后的同步表现。
- 创造模式 mouse dragged 恢复后是否符合最终发布体验预期。
- 是否需要更新 `THIRD_PARTY_NOTICES.md` 或发布说明中的来源说明。
