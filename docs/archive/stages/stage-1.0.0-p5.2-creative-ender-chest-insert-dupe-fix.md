# stage-1.0.0-p5.2-creative-ender-chest-insert-dupe-fix

本阶段修复创造模式下用鼠标 carried stack 右键收入末影箱后、关闭非玩家背包界面时复制出一个物品的问题。

## 问题现象

- 阶段 5.1 之后，末影箱右键收纳 / 放出基本正常。
- 在创造模式下，打开任意"有格子的非玩家背包界面"（原版箱子、熔炉、合成台等工作台界面，以及 QuickShulker 潜影盒 / 末影箱 quick-open 菜单）时，鼠标拿着一个物品右键末影箱将其收入后，再关闭界面，会复制出一个物品。
- 生存模式相同操作物品数量守恒，不存在复制或丢失。

## 根因

根因是创造模式下 bundling handler 在写入末影箱后，没有把更新后的 carried 同步到服务端 `containerMenu.carried`，导致原版 `AbstractContainerMenu.removed(Player)` 在关闭菜单时退还的是"收入前"的旧 carried。

具体链路：

1. 玩家在创造模式下左键拾取一个物品。原版 `AbstractContainerMenu.clicked` 在客户端和服务端都把该物品放入 `containerMenu.carried`，来源槽位变空。
2. 玩家右键末影箱，客户端取消原版右键并发送 `ENDER_CHEST_INSERT` intent。
3. 服务端 `handleEnderChestInsert` 将 carried 物品写入玩家自己的 `EnderChestInventory`，并调用 `writeCarried(player, updatedCarried, null)`。
4. 修复前，创造模式分支下 `writeCarried` 只在存在 `DragSession` 时写入 session 缓存，之后直接 `return`，从不调用 `player.containerMenu.setCarried(...)`。因此服务端 `containerMenu.carried` 仍然保留步骤 1 左键拾取时的旧物品。
5. 客户端通过 `syncCreativeCursor` 正确把 carried 清空（日志可见 `Applied ... creative cursor sync: stack=<empty>`），所以界面期间玩家看不到异常。
6. 关闭界面时，原版 `AbstractContainerMenu.removed(Player)` 检测到玩家是 `ServerPlayer` 且 `getCarried()` 非空，调用 `Inventory.placeItemBackInInventory(carried)` 把旧物品退还给玩家背包。
7. 该物品此时既在末影箱（步骤 3 已写入），又被退还到玩家背包（步骤 6），形成复制。

经反编译确认，Forge 1.20.1 与 NeoForge 1.21.1 的 `AbstractContainerMenu.removed(Player)` 行为一致：

```
if (player instanceof ServerPlayer) {
    ItemStack carried = this.getCarried();
    if (!carried.isEmpty()) {
        if (!player.isAlive() || ((ServerPlayer)player).hasDisconnected()) {
            player.drop(carried, false);
        } else {
            player.getInventory().placeItemBackInInventory(carried);
        }
        this.setCarried(ItemStack.EMPTY);
    }
}
```

这与阶段 3.5 修复"创造模式 carried 潜影盒"时的历史结论一致：创造模式下不能把 `containerMenu.carried` 当作 bundling 的长期真值来源，但本次问题恰恰是"完全不写服务端 carried"导致 `removed()` 退还了过期值。

## 修复方式

最小必要修改：在双平台 `ForgeShulkerBundlingHandler.writeCarried` / `NeoForgeShulkerBundlingHandler.writeCarried` 的创造模式分支中，写入 `DragSession`（如果存在）之后，额外调用一次 `player.containerMenu.setCarried(copy)`，再 `return`。

```
private static void writeCarried(ServerPlayer player, ItemStack stack, DragSession dragSession) {
    ItemStack copy = stack.copy();
    if (player.getAbilities().instabuild) {
        if (dragSession != null) {
            dragSession.setCreativeCursor(copy);
        }
        player.containerMenu.setCarried(copy);   // 本次新增
        return;
    }
    player.containerMenu.setCarried(copy);
}
```

这样做同时满足：

- 创造模式 bundling 成功后，服务端 `containerMenu.carried` 立即变成与 `syncCreativeCursor` 一致的更新值（收入成功则为空 / 剩余）。
- 关闭界面时 `AbstractContainerMenu.removed()` 退还的是更新后的 carried，不再退还旧值，复制路径被切断。
- 客户端 carried 仍由 `syncCreativeCursor` 驱动，服务端写入值与客户端一致，不会触发额外的 `ClientboundContainerSetSlotPacket`（slot -1）覆盖。
- `resolvedCarried` 在创造模式下仍然只读 `DragSession` 或客户端 payload `cursorStack`，不读服务端 `containerMenu.carried`，因此不影响连续右键 / 拖拽真值来源。
- 生存模式分支完全不变，原有守恒逻辑不受影响。

该修复是通用的：它同时覆盖 `INSERT`、`PICKUP_INSERT`、`MOUSE_DRAG_PICKUP_INSERT`、`TRANSFER`、`ENDER_CHEST_INSERT`、`ENDER_CHEST_PICKUP_INSERT`、`ENDER_CHEST_EXTRACT` 等所有调用 `writeCarried` 的 bundling 路径。`ENDER_CHEST_EXTRACT` / `ENDER_CHEST_PICKUP_INSERT` 不调用 `writeCarried`（carried 末影箱本身不变），其服务端 carried 由原版左键拾取链路维护，关闭时退还末影箱是正确行为，无需额外处理。

## 行为边界

- 本次只修创造模式下 carried 写入末影箱后的关闭复制问题。
- 生存模式守恒不变：收入成功后 carried 被正确减少 / 清空，关闭界面退还更新后的 carried（通常为空）。
- 末影箱内容仍只读写玩家自己的 `EnderChestInventory`，不写回宿主 `ItemStack`。
- 末影箱右键放出、潜影盒 bundling、宿主槽位锁定、`rightClickClose` / `Bundle` / reopen inventory 均未恢复，也未修改。
- 不通过"无条件禁止创造模式末影箱收入"规避问题。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `docs/stage-1.0.0-p5.2-creative-ender-chest-insert-dupe-fix.md`

## 双平台影响

### Forge 1.20.1

- 创造模式所有 `writeCarried` 调用路径现在都会把更新后的 carried 写到服务端 `containerMenu`，关闭菜单时 `removed()` 退还正确值。
- `ENDER_CHEST_INSERT` 创造模式复制问题修复。
- 生存模式不变。

### NeoForge 1.21.1

- 与 Forge 保持相同修复策略。
- `ENDER_CHEST_INSERT` 创造模式复制问题修复。
- 生存模式不变。

## 验证命令

- `git diff --check`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`
- `.\gradlew.bat :common:test`

## 验证结果

- `git diff --check` 通过（仅 CRLF 标准化提示，无空白错误）。
- `:forge-1.20.1:compileJava` BUILD SUCCESSFUL。
- `:neoforge-1.21.1:compileJava` BUILD SUCCESSFUL。
- `:common:test` BUILD SUCCESSFUL（common 未改动，规则层不变）。
- 代码层修复完成；游戏内行为待人工验收。

## 待人工确认项

- 需要维护者在 Forge 1.20.1 实机确认：
  - 创造模式打开原版箱子 / 熔炉 / 合成台等有格子界面，拿物品右键末影箱收入后关闭界面，不再复制。
  - 创造模式打开 QuickShulker 潜影盒界面、末影箱界面，重复上述操作，不再复制。
  - 生存模式相同操作物品数量守恒，不丢失、不复制。
  - 末影箱右键放出、右键收纳潜影盒、满末影箱失败路径仍正常。
  - 创造模式连续右键 / 拖拽潜影盒 bundling 不会因本次修改出现"消失"或"复制"。
- 需要维护者在 NeoForge 1.21.1 实机确认同类场景。
- 无法仅通过本地编译确认 Minecraft 客户端内真实交互结果。
- 无法在本地完成多人服务器实机验证。
- 无法确认本次通用 `writeCarried` 改动在所有创造模式菜单类型（含第三方模组菜单）下均无副作用，建议重点回归创造模式各类界面的关闭退还路径。