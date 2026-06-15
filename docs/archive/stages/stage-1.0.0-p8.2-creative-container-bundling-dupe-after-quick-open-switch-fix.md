# 阶段 1.0.0 P8.2 创造模式容器 bundling 后切换 quick-open 复制修复

## 背景

1.0.0 发布前测试发现，创造模式下存在稳定的物品复制 bug。该问题同时影响潜影盒和末影箱 bundling，共同特征是"先在背包界面执行一次容器类 bundling，不关闭背包直接打开 quick-open 页面，再执行一次容器类插入，关闭界面后插入的物品被复制一份回到背包"。

这是发布阻断问题，不能带到 1.0.0。

## 问题现象

### 复现路径 A：潜影盒

1. 创造模式打开背包界面。
2. 在背包界面使用一次"潜影盒右键放出"功能（EXTRACT）。
3. 不关闭背包，直接打开一个 quick-open 页面，例如右键工作台打开快捷页面。
4. 拿起一个物品，右键放入潜影盒（INSERT）。
5. 关闭背包 / quick-open 页面，会发现上一步放入潜影盒的物品被复制了一份到背包里。

### 复现路径 B：末影箱

1. 创造模式打开背包界面。
2. 在背包界面使用一次"末影箱右键放出"功能（ENDER_CHEST_EXTRACT）。
3. 不关闭背包，直接打开一个 quick-open 页面，例如右键工作台打开快捷页面。
4. 拿起一个物品，右键放入末影箱（ENDER_CHEST_INSERT）。
5. 关闭背包 / quick-open 页面，会发现上一步放入末影箱的物品被复制了一份到背包里。

除上述路径外，潜影盒 / 末影箱的 mouse dragged 取出和插入组合、从普通创造背包界面切换到 quick-open 页面、quick-open 页面关闭时 carried stack 收尾等路径也可能触发同类问题。

## 根因

### 历史背景

- 阶段 3.5 确立了创造模式下 bundling 的 carried 真值应保存在 QuickShulker 自己的 drag session 中，而不是长期写在服务端 `InventoryMenu.carried` 上。
- 阶段 5.2 在 `writeCarried` 创造模式分支中额外调用 `player.containerMenu.setCarried(copy)`，以修复"末影箱右键收入后关闭背包界面复制"问题。该修复使服务端 `containerMenu.carried` 与 `syncCreativeCursor` 推送的客户端 carried 保持一致，确保 `AbstractContainerMenu.removed()` 返还正确的 post-bundling carried。
- 阶段 8.1 发现 5.2 方式在某些场景下仍有复制风险，于是移除了 `writeCarried` 创造模式分支中的 `player.containerMenu.setCarried(copy)`，改为完全不写服务端 carried，只写 drag session 并通过 `syncCreativeCursor` 推客户端。

### 当前 bug 的直接原因

阶段 8.1 的"完全不写服务端 carried"策略引入了本次复制 bug。

创造模式下，当玩家通过**原版左键点击**拿起一个普通物品时，原版 `AbstractContainerMenu.clicked()` 会在服务端和客户端都把该物品放入 `containerMenu.carried`，来源槽位变空。

随后玩家用 bundling（如 INSERT）把该 carried 物品放入容器时：

1. 服务端 `handleInsert` 调用 `resolvedCarried(player, cursorStack)`，创造模式下返回客户端 payload 的 `cursorStack`（即左键拿起的物品）。
2. 执行 insert，`updatedCarried` = 剩余或空。
3. `writeCarried(player, updatedCarried, null)` 在 8.1 后的创造模式分支中**不写入** `player.containerMenu.setCarried(...)`，直接 return。
4. 因此服务端 `containerMenu.getCarried()` 仍然保留着步骤 1 左键拿起时的**旧物品**。
5. `syncCreativeCursor(player, updatedCarried)` 把客户端 carried 正确更新为剩余 / 空，客户端界面看不到异常。
6. 关闭界面时，原版 `AbstractContainerMenu.removed(Player)` 检测到服务端 `getCarried()` 非空（旧物品），调用 `Inventory.placeItemBackInInventory(carried)` 把旧物品返还到背包。
7. 该物品已经在步骤 2 被放入容器，现在又被返还到背包，形成复制。

### 为什么 8.1 的策略会引入这个问题

8.1 担心的是"bundling 载体（潜影盒 / 末影箱）本身被当作 carried 写入服务端后，菜单切换 / 关闭时被原版返还，与客户端 carried 状态不同步导致复制"。但 8.1 的"完全不写"策略忽略了一个关键场景：**普通物品通过原版左键拿起后，carried 已经在服务端有值，bundling 操作只通过 `syncCreativeCursor` 更新客户端，服务端 carried 成为过期值**。

这与 5.2 要修复的问题本质相同：服务端 carried 与客户端 carried 不同步，导致 `removed()` 返还过期值。5.2 的修复方向（写服务端 carried）是正确的；8.1 的"完全不写"是过度修正。

## 修复方式

### 修复 1：恢复创造模式写服务端 carried

在双平台的 `writeCarried` 创造模式分支中，恢复 `player.containerMenu.setCarried(copy)`：

```java
private static void writeCarried(ServerPlayer player, ItemStack stack, DragSession dragSession) {
    ItemStack copy = stack.copy();
    if (player.getAbilities().instabuild) {
        if (dragSession != null) {
            dragSession.setCreativeCursor(copy);
        }
        player.containerMenu.setCarried(copy);   // 恢复写入
        return;
    }
    player.containerMenu.setCarried(copy);
}
```

这样 bundling 修改 carried 后，服务端 `containerMenu.carried` 立即与 `syncCreativeCursor` 推送的客户端 carried 一致。关闭界面时 `AbstractContainerMenu.removed()` 返还的是正确的 post-bundling carried（INSERT 成功后通常为空），不再返还过期旧值，复制路径被切断。

该修复覆盖所有调用 `writeCarried` 的 bundling 路径：

- `INSERT`（普通物品右键放入潜影盒）
- `PICKUP_INSERT` / `MOUSE_DRAG_PICKUP_INSERT`（携带潜影盒右键 / 拖拽收入普通物品）
- `EXTRACT` / `MOUSE_DRAG_EXTRACT`（携带潜影盒右键 / 拖拽放出物品到空槽）
- `TRANSFER`（潜影盒之间内容转移）
- `ENDER_CHEST_INSERT`（普通物品右键收入末影箱）

`ENDER_CHEST_EXTRACT` / `ENDER_CHEST_PICKUP_INSERT` 不调用 `writeCarried`（末影箱载体本身 carried 不变），其服务端 carried 由原版左键勾取链路维护，关闭时返还末影箱是正确行为。

### 修复 2：quick-open 打开前清理 drag session

在双平台的 session manager `open()` 方法开头，调用 `clearDragSession(player)`：

```java
public void open(ServerPlayer player, HostItemReference hostItemReference, QuickOpenTrigger trigger) {
    ForgeShulkerBundlingHandler.clearDragSession(player);   // 新增
    ActiveSession existingSession = sessions.get(player.getUUID());
    ...
}
```

这确保从任意菜单（包括创造背包）切换到 quick-open 页面时，旧的 bundling drag session（及其 `creativeCursor` 缓存）被清理。由于修复 1 已经保证服务端 `containerMenu.carried` 与客户端一致，清理 drag session 只是丢弃 QuickShulker 内部的 creative cursor 缓存，不影响 `removed()` 的正确返还。

这同时防御了"旧 drag session 的 creativeCursor 在新菜单中被 `resolvedCarried` 误用"的潜在路径。

## 涉及文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerSessionManager.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerSessionManager.java`

## 数据安全

- 创造模式 bundling 修改 carried 后，服务端 `containerMenu.carried` 与客户端 `syncCreativeCursor` 推送值一致，`removed()` 返还正确值。
- 创造模式 INSERT 普通物品后关闭菜单不再复制（服务端 carried 为空或剩余，`removed()` 返还正确）。
- 创造模式 EXTRACT / PICKUP_INSERT 载体（潜影盒 / 末影箱）后切换菜单，载体由 `removed()` 正确返还到背包，新菜单 carried 为空。
- quick-open 打开前清理 drag session，防止旧 creativeCursor 泄漏到新菜单。
- 生存模式 carried / bundling 主链路完全不变（`writeCarried` 生存分支不受影响）。
- 末影箱内容仍只读写玩家自己的 `EnderChestInventory`，不写入宿主 ItemStack。
- 宿主槽位锁定、同一宿主重复打开拒绝、容器写回目标校验等既有安全规则不受影响。
- 服务端仍不信任客户端请求，`resolvedCarried` / `carriedStillMatches` / `HostSlotRef` 校验 / 宿主有效性校验逻辑不变。
- 不通过禁用创造模式潜影盒 / 末影箱 bundling 规避问题。

## 双平台影响

### Forge 1.20.1

- `ForgeShulkerBundlingHandler.writeCarried` 创造模式分支恢复 `player.containerMenu.setCarried(copy)`。
- `ForgeShulkerSessionManager.open` 开头调用 `ForgeShulkerBundlingHandler.clearDragSession(player)`。

### NeoForge 1.21.1

- `NeoForgeShulkerBundlingHandler.writeCarried` 创造模式分支恢复 `player.containerMenu.setCarried(copy)`。
- `NeoForgeShulkerSessionManager.open` 开头调用 `NeoForgeShulkerBundlingHandler.clearDragSession(player)`。

## 验证

- `git diff --check`
- `.\gradlew.bat :common:test`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`

## 验证结果

- `git diff --check`：通过（无空白错误）。
- `:common:test`：BUILD SUCCESSFUL（common 未改动，规则层不变）。
- `:forge-1.20.1:compileJava`：BUILD SUCCESSFUL。
- `:neoforge-1.21.1:compileJava`：BUILD SUCCESSFUL。
- 代码层修复完成；游戏内行为待人工验收。

## 待人工确认项

1. 无法进行 Minecraft 游戏内测试，无法确认创造模式复制问题在实机中已被完全消除。需维护者在 Forge 1.20.1 和 NeoForge 1.21.1 实机确认以下场景：
   - 创造模式潜影盒复现路径 A：背包内潜影盒右键放出 → 不关背包打开 quick-open → 拿物品右键放入潜影盒 → 关闭界面，不复制。
   - 创造模式末影箱复现路径 B：背包内末影箱右键放出 → 不关背包打开 quick-open → 拿物品右键放入末影箱 → 关闭界面，不复制。
   - 创造模式潜影盒 mouse dragged 取出后切换 quick-open 再 mouse dragged 插入，关闭不复制。
   - 创造模式末影箱 mouse dragged 取出后切换 quick-open 再 mouse dragged 插入，关闭不复制。
   - 创造模式潜影盒 / 末影箱 INSERT（右键插入）后直接关闭背包，不复制。
   - 创造模式潜影盒 / 末影箱 EXTRACT（右键取出）后切换 quick-open 页面，载体正确返还到背包，新菜单 carried 为空。
   - 创造模式连续多次 bundling + quick-open 切换，不出现复制或丢失。
   - 生存模式相同操作物品数量守恒，不丢失、不复制。
2. 无法进行多人服务器测试，无法确认多人环境下创造模式 bundling + quick-open 切换不再产生复制。
3. 无法确认是否存在除已知复现路径以外的其他创造模式复制触发方式。
4. 无法确认最低 Forge / NeoForge loader 版本。
5. 无法确认 Modrinth 发布元数据。