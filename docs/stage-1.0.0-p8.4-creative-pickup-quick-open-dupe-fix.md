# 阶段 1.0.0 P8.4 创造模式拾取/丢弃后 quick-open 切换复制修复

## 背景

1.0.0 发布前测试发现新的创造模式复制 bug。已知复现路径：

1. 创造模式下，从创造物品栏快速扔出大量物品。
2. 捡起所有物品。
3. 反复开关背包界面。
4. 在不关闭背包的情况下打开 QuickShulker quick-open 页面。
5. 此时，最后一个捡起的物品可能被复制一份。

具体触发条件还不清楚，上述流程只是其中一种可能路径。当前仅在创造模式下发现。

## 归因分析

### 原版 Minecraft 行为

在原版 Minecraft 1.20.1 / 1.21.1 中，`ServerPlayer.openMenu()` 打开新容器时会先调用 `closeContainer()`，其中调用 `containerMenu.removed(player)`。对于 `InventoryMenu`（背包菜单），`AbstractContainerMenu.removed()` 默认是空实现，不会自动返还或清理 `carried` stack。

原版不会在已有 carried 的情况下通过程序化方式打开新容器，因此不产生此问题。

### Forge / NeoForge 平台行为

Forge 47.x 和 NeoForge 21.x 没有改变 `AbstractContainerMenu.removed()` 的默认空实现，也没有改变 `ServerPlayer.closeContainer()` 的 carried 处理逻辑。平台本身不引入此 bug。

### QuickShulker 模组影响

**确认属于 QuickShulker 自身逻辑导致的问题。**

QuickShulker 允许在背包/创造物品栏已打开的情况下，通过快捷键或鼠标悬停右键触发 quick-open。打开流程：

1. 客户端 `trySendHovered` 检查**客户端** `containerScreen.getMenu().getCarried().isEmpty()`。
2. 如果客户端 carried 为空，发送 `OpenHostItemIntent` 到服务端。
3. 服务端 `ForgeQuickOpenHandler.handle` / `NeoForgeQuickOpenHandler.handle` 验证宿主物品。
4. 服务端 `sessionManager.open()` 调用 `player.openMenu()` 打开 quick-open 菜单。

**问题根因**：`sessionManager.open()` 在调用 `player.openMenu()` 前，不检查也不清理**服务端** `player.containerMenu.getCarried()`。

在创造模式下，客户端和服务端的 carried 状态可能不一致：

- 创造模式下从创造物品列表拾取物品时，客户端 `CreativeModeInventoryScreen` 设置 `menu.setCarried()`，但不发送标准 `ServerboundContainerClickPacket`，服务端 `InventoryMenu.carried` 保持为空。
- 创造模式下从背包（Inventory tab）通过左键拾取物品时，服务端 `InventoryMenu.carried` 会有值。
- 反复开关背包、快速丢弃/拾取物品后，客户端和服务端 carried 可能进入不一致状态。

当服务端 `containerMenu.carried` 非空时，`player.openMenu()` → `closeContainer()` → `containerMenu.removed()` 不清理 carried。旧的 `InventoryMenu` 被重新设为 `containerMenu` 时，残留的 carried 值可能在后续操作中（如 quick-open 菜单关闭后返回 inventory menu、重新打开背包）被错误返还给玩家，产生复制。

之前阶段 8.2/8.3 的修复仅覆盖 **bundling 操作**的 carried 同步（`writeCarried` 创造模式分支），不覆盖"不做 bundling，直接 quick-open"的路径。

## 根因

`sessionManager.open()` 在打开 quick-open 菜单前不清理服务端残留的 `containerMenu.carried()`，导致创造模式下旧菜单的 carried 物品在后续流程中被错误返还或复制。

## 修复

在 Forge 和 NeoForge 的 `sessionManager.open()` 方法中，`clearDragSession` 之后、`player.openMenu()` 之前，新增 `clearStaleCreativeCarriedBeforeOpen(player)` 调用。

该方法的安全边界：

- **仅创造模式生效**：`player.getAbilities().instabuild` 为 true 时才执行。生存模式完全不受影响。
- **仅当前菜单不是 quick-open 菜单时执行**：`player.containerMenu instanceof ForgeQuickOpenMenu` / `NeoForgeQuickOpenMenu` 时跳过（quick-open 到 quick-open 的宿主切换由既有的 session finish 路径处理）。
- **仅 carried 非空时执行**：避免不必要的网络包。
- 清理时发送 `sendCreativeCursorSync(EMPTY)` 同步客户端，避免客户端显示残留 cursor。
- 创造模式物品本质上是无限的，清空 carried 不会造成真实物品损失——这与原版 `CreativeModeInventoryScreen` 关闭时销毁 cursor 物品的行为一致。

## 涉及路径

- Forge / NeoForge `sessionManager.open()` 方法。
- 服务端 `containerMenu.carried` 清理。
- 客户端 creative cursor sync。
- 生存模式 carried 主链路完全不变（`clearStaleCreativeCarriedBeforeOpen` 在非创造模式下直接返回）。
- 潜影盒/末影箱 bundling 的 `writeCarried` 创造模式分支不受影响（8.2/8.3 修复保持不变）。
- 宿主槽位锁定、同一宿主重复打开拒绝、容器写回目标校验等既有安全规则不受影响。

## 生存模式回归检查

- `clearStaleCreativeCarriedBeforeOpen` 第一行检查 `!player.getAbilities().instabuild` 直接返回，生存模式完全不进入清理逻辑。
- 生存模式下 quick-open 打开、菜单切换、carried stack 处理路径完全不变。
- 生存模式拾取物品后 quick-open 切换不会丢失物品（carried 由原版逻辑处理）。
- 生存模式不会出现 carried 被错误清空（因为根本不进入清理分支）。

## 是否仅影响创造模式

是。修复仅作用于创造模式（`instabuild == true`）。生存模式代码路径完全不受影响。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerSessionManager.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerSessionManager.java`

## 双平台影响

### Forge 1.20.1

- `ForgeShulkerSessionManager.open()` 新增 `clearStaleCreativeCarriedBeforeOpen` 调用和方法。
- 新增 `import ForgeQuickShulkerNetwork`。

### NeoForge 1.21.1

- `NeoForgeShulkerSessionManager.open()` 新增 `clearStaleCreativeCarriedBeforeOpen` 调用和方法。
- 新增 `import NeoForgeQuickShulkerNetwork`。

## 与上一阶段修复的关系

- 8.2 修复：标准左键来源 carried 在 INSERT 后服务端 carried 未更新导致 `removed()` 放回旧值的复制。
- 8.3 修复：创造物品列表来源 carried 被错误写入服务端导致 `removed()` 放回虚拟物品的复制。
- 8.4 修复：**不做 bundling**，直接 quick-open 时服务端残留 carried 未清理导致后续复制的路径。8.2/8.3 覆盖的是 bundling 后的 carried 同步，8.4 覆盖的是非 bundling 的 quick-open 打开前 carried 清理。

三者互补：
- 8.2/8.3 处理 bundling 操作对 carried 的写入。
- 8.4 处理 quick-open 打开前对已有 carried 的清理。

## 数据安全

- 创造模式 quick-open 打开前，服务端 `containerMenu.carried` 如果非空会被清空并同步客户端。
- 创造模式物品无限，清空 carried 不会造成真实物品损失。
- 生存模式完全不受影响。
- 潜影盒内容仍只读写玩家自己的容器，末影箱内容仍只来自玩家自己的 `EnderChestInventory`。
- 宿主槽位锁定、同一宿主重复打开拒绝、容器写回目标校验等既有安全规则不变。
- 服务端仍不信任客户端请求，`resolvedCarried` / `carriedStillMatches` / `HostSlotRef` 校验 / 宿主有效性校验逻辑不变。
- 潜影盒不能嵌套进潜影盒、末影箱不能收入末影箱等既有规则不放松。

## 验证

- `git diff --check`
- `.\gradlew.bat :common:test`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`

## 验证结果

- `git diff --check`：通过（仅行尾符警告）。
- `:common:test`：BUILD SUCCESSFUL（common 未改动，规则层不变）。
- `:forge-1.20.1:compileJava`：BUILD SUCCESSFUL。
- `:neoforge-1.21.1:compileJava`：BUILD SUCCESSFUL。
- 代码层修复完成；游戏内行为待人工验收。

## 待人工确认项

1. 无法进行 Minecraft 游戏内测试，无法确认创造模式复制问题在实机中已被完全消除。需维护者在 Forge 1.20.1 和 NeoForge 1.21.1 实机确认以下场景：
   - 创造模式下从创造物品栏快速扔出大量物品，捡起后反复开关背包，在不关闭背包的情况下 quick-open 潜影盒，不复制。
   - 创造模式下从创造物品栏快速扔出大量物品，捡起后反复开关背包，在不关闭背包的情况下 quick-open 末影箱，不复制。
   - 创造模式下从 Inventory tab（背包）拾起物品后直接 quick-open，不复制。
   - 创造模式下连续多次"丢弃/拾取/开关背包/quick-open"组合操作，不复制、不丢失。
   - 创造模式下 quick-open 菜单内切换宿主，不复制。
   - 创造模式下从创造物品列表拾取物品到 cursor 后直接 quick-open，cursor 被正确清空。
2. 无法进行多人服务器测试，无法确认多人环境下创造模式 quick-open 不再产生复制。
3. 无法确认是否存在除已知复现路径以外的其他创造模式复制触发方式。
4. 无法确认最低 Forge / NeoForge loader 版本。
5. 无法确认 Modrinth 发布元数据。
6. 无法确认该修复是否覆盖所有创造模式 carried 不一致场景（原版创造模式的 carried 同步机制本身较复杂，可能存在未发现的边缘路径）。