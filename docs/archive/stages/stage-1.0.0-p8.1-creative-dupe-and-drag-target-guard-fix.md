# 阶段 1.0.0 P8.1 创造模式复制修复与拖拽目标限制补齐

## 背景

1.0.0 发布前审查（P8）完成后，实机测试仍发现两个问题：

1. 创造模式下存在潜影盒复制风险。
2. 末影箱 / 潜影盒右键 bundling 已经会屏蔽不安全目标（例如不能对当前打开 QuickShulker 页面的宿主槽位进行 bundling），但部分拖拽路径在服务端缺少相同的限制。

本阶段修复这两个问题，Forge 1.20.1 和 NeoForge 1.21.1 双平台都要修，不新增玩家可见功能。

## 问题 1：创造模式潜影盒复制

### 根因

阶段 3.5 文档已经明确：创造模式下 bundling 的“最新 carried 栈”应保存在 QuickShulker 自己的 drag session 中，而不是长期写在服务端 `InventoryMenu.carried` 上。

但实际代码中 `writeCarried` 在创造模式下仍然同时做了两件事：

```java
if (player.getAbilities().instabuild) {
    if (dragSession != null) {
        dragSession.setCreativeCursor(copy);
    }
    player.containerMenu.setCarried(copy);  // ← 仍然写入服务端菜单 carried
    return;
}
```

这意味着每次创造模式 bundling 完成后，服务端 `player.containerMenu`（创造背包下通常是 `InventoryMenu`）的 `carried` 字段都会残留一个潜影盒或末影箱。

当创造背包关闭 / 重新打开、或从普通容器界面切换到 QuickShulker 页面时，这份残留的服务端 carried 会被原版逻辑当作待返还物品处理，从而产生复制。

### 复现路径（已知之一）

1. 创造模式打开物品栏。
2. 使用一次潜影盒右键或拖拽 bundling（`PICKUP_INSERT` / `EXTRACT`）。
3. 在不关闭背包的情况下右键打开一个 QuickShulker 页面。
4. 拿起潜影盒，对刚才的潜影盒继续使用右键或拖拽 bundling。
5. 关闭背包页面并重新打开，重复两次此操作。
6. 在第二次关闭背包时，会发现潜影盒被复制。

### 修复方式

`writeCarried` 在创造模式下不再调用 `player.containerMenu.setCarried(copy)`。

- 如果有 drag session，carried 只写入 `DragSession.creativeCursor`。
- 如果没有 drag session（单次右键 `INSERT` / `TRANSFER` / `ENDER_CHEST_INSERT`），不写入任何服务端菜单字段。
- 更新后的 carried 仍然通过独立的 `syncCreativeCursor` → S2C cursor sync 包推送到客户端当前鼠标 / 当前菜单。
- 生存模式路径不变，继续使用 `player.containerMenu.setCarried`。

这样做是安全的，因为：

- 创造模式下 `resolvedCarried` 只从 `DragSession.creativeCursor` 或客户端 `cursorStack` 读取，从不从 `player.containerMenu.getCarried()` 读取。
- 创造模式下 `carriedStillMatches` 恒返回 `true`，不依赖服务端菜单 carried。
- `broadcastChanges()` / `broadcastFullState()` 只同步槽位状态，不同步 carried。

### 涉及文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`

## 问题 2：末影箱拖拽路径缺少宿主限制

### 根因

潜影盒 bundling 的宿主限制（`isCurrentQuickOpenHost`）同时存在于：

- 客户端 `determineBundlingIntent`（右键路径）和 `trySendMouseDraggedBundlingIntent`（拖拽路径）。
- 服务端各 handler 顶部。

末影箱 bundling 的宿主限制只存在于：

- 服务端 `handleEnderChestBundling` dispatcher（覆盖右键路径 `ENDER_CHEST_INSERT` / `ENDER_CHEST_PICKUP_INSERT` / `ENDER_CHEST_EXTRACT`）。
- 客户端 `determineBundlingIntent` 和 `trySendMouseDraggedBundlingIntent`。

但末影箱拖拽续包路径（`MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT` / `MOUSE_DRAG_ENDER_CHEST_EXTRACT`）在服务端通过 `handleMouseDragEnderChestPickupInsert` / `handleMouseDragEnderChestExtract` 直接调用内部 handler，绕过了 `handleEnderChestBundling` dispatcher，因此缺少 `isCurrentQuickOpenHost` 校验。

这意味着如果客户端防护被绕过或状态不同步，恶意或错误客户端可以在服务端对当前打开的 QuickShulker 宿主槽位执行末影箱拖拽 bundling，违反宿主锁定规则。

### 修复方式

在三个末影箱内部 handler 顶部统一添加 `isCurrentQuickOpenHost` 校验：

- `handleEnderChestInsert`
- `handleEnderChestPickupInsert`（带 `requireActionConfig` 的最底层版本）
- `handleEnderChestExtract`（带 `requireActionConfig` 的最底层版本）

这样无论调用来源是右键 dispatcher 还是拖拽续包路径，都能保证宿主限制一致。

`handleEnderChestBundling` dispatcher 中原有的 `isCurrentQuickOpenHost` 校验保留作为 defense-in-depth，不删除。

### 涉及文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`

## 客户端拖拽限制对比

已确认客户端 `trySendMouseDraggedBundlingIntent` 与右键 `determineBundlingIntent` 已覆盖相同的限制：

- 当前打开 QuickShulker 页面的宿主槽位不能被 bundling / 拖拽（`HostIdentity.sameSlot` 检查）。
- 手持末影箱悬停末影箱时拖拽会停止（`isCarriedEnderChestHoveringEnderChest` 检查）。
- 潜影盒不能嵌套进潜影盒（`!isShulkerBox(hoveredStack)` 检查）。
- 末影箱不能收入末影箱（`canInsertIntoEnderChest` 拒绝末影箱）。

客户端不需要额外修改；本阶段补齐的是服务端的缺失校验。

## 数据安全

- 创造模式不再把 carried 残留在服务端菜单上，消除关闭 / 重开时的复制来源。
- 创造模式 bundling 后鼠标上的潜影盒 / 末影箱不会消失（仍通过 `syncCreativeCursor` 推送）。
- 末影箱拖拽路径服务端重新校验宿主限制，不依赖客户端过滤。
- 生存模式 carried / bundling 主链路不变。
- 宿主锁定、同一宿主重复打开拒绝、容器写回目标校验等既有安全规则不受影响。

## 双平台影响

### Forge 1.20.1

- `writeCarried` 创造模式不再写入 `player.containerMenu.setCarried`。
- `handleEnderChestInsert` / `handleEnderChestPickupInsert` / `handleEnderChestExtract` 添加 `isCurrentQuickOpenHost` 校验。

### NeoForge 1.21.1

- 与 Forge 保持一致的修复策略。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `docs/stage-1.0.0-p8.1-creative-dupe-and-drag-target-guard-fix.md`

## 验证

- `git diff --check`
- `.\gradlew.bat :common:test`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`

## 验证结果

- `git diff --check`：通过。
- `:common:test`：BUILD SUCCESSFUL（from cache，无变更）。
- `:forge-1.20.1:compileJava`：BUILD SUCCESSFUL。
- `:neoforge-1.21.1:compileJava`：BUILD SUCCESSFUL。

## 待人工确认项

1. 无法进行 Minecraft 游戏内测试，无法确认创造模式复制问题在实机中已被完全消除。需维护者在 Forge 1.20.1 和 NeoForge 1.21.1 实机确认以下场景：
   - 创造模式下使用潜影盒右键 / 拖拽 bundling 后，关闭并重新打开创造背包，不出现潜影盒复制。
   - 创造模式下从创造背包打开 QuickShulker 页面，在页面内拿起潜影盒并继续 bundling，关闭 / 重开背包不出现复制。
   - 创造模式下使用末影箱 bundling 后，关闭并重新打开创造背包，不出现末影箱复制。
2. 无法进行多人服务器测试，无法确认多人环境下创造模式 bundling 不再产生复制。
3. 无法确认是否存在除已知复现路径以外的其他创造模式复制触发方式。
4. 无法确认最低 Forge / NeoForge loader 版本。
5. 无法确认 Modrinth 发布元数据。