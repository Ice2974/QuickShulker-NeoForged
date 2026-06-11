# stage-0.2.0-p2-reopen-inventory

## 概要

注意：该阶段功能已因复制物品风险在后续阶段删除，当前行为以 `stage-0.2.0-p2x-remove-reopen-inventory.md`、`release-0.2.0.md` 和源码为准。

本阶段实现了 Forge 1.20.1 与 NeoForge 1.21.1 的 `reopen inventory` S2C 网络基础设施，并把它接入 quick-open session 的正常关闭路径。

当前行为边界：

* 仅 inventory trigger（`INVENTORY_KEYBIND` / `INVENTORY_RIGHT_CLICK`）打开、且 quick-open 类型允许 reopen 时，正常关闭 quick-open 菜单后才尝试重新打开玩家背包
* hand trigger（`HAND_KEYBIND` / `HAND_RIGHT_CLICK`）不会触发 reopen
* `INTERNAL_REOPEN` / `UNKNOWN` 不会触发 reopen
* `switch_open` 收尾不会触发 reopen
* `host invalidated` / `disconnect` / `death` / `dimension change` 不会触发 reopen
* 客户端只尝试重新打开玩家自己的 `InventoryScreen`，不会发送新的 quick-open C2S，也不会覆盖已经打开的其它界面

## 本阶段修改

### common

* 新增 `QuickOpenReturnToInventoryPolicy`，统一按 trigger 和 `QuickOpenableType.reopenPlayerInventoryAfterClose` 计算 `shouldReturnToPlayerInventory`
* 新增 `ReopenPlayerInventoryQueue`，作为纯 Java 的客户端待处理 reopen 队列

### Forge 1.20.1

* 新增 `ForgeReopenPlayerInventoryPacket`
* 在 `ForgeQuickShulkerNetwork` 注册 `PLAY_TO_CLIENT` 消息，并支持服务端定向发送 reopen intent
* 在 `ForgeQuickOpenHandler` 按 trigger/type 正确填充 `QuickOpenRequest.shouldReturnToPlayerInventory`
* 在 `ForgeShulkerSessionManager` 里把 `MenuOpenIntent.reopenPlayerInventoryAfterClose` 接上，并在 `menu_removed + PLAYER_CLOSED + host valid` 的 quick-open 正常收尾后发送 S2C
* 在 `ForgeQuickShulkerClient` 增加 pending reopen 处理，仅在当前没有其它界面时打开 `InventoryScreen`

### NeoForge 1.21.1

* 新增 `NeoForgeReopenPlayerInventoryPayload`
* 在 `NeoForgeQuickShulkerNetwork` 注册 `playToClient` payload，并支持服务端定向发送 reopen intent
* 在 `NeoForgeQuickOpenHandler` 按 trigger/type 正确填充 `QuickOpenRequest.shouldReturnToPlayerInventory`
* 在 `NeoForgeShulkerSessionManager` 里把 `MenuOpenIntent.reopenPlayerInventoryAfterClose` 接上，并在 `menu_removed + PLAYER_CLOSED + host valid` 的 quick-open 正常收尾后发送 S2C
* 在 `NeoForgeQuickShulkerClient` 增加 pending reopen 处理，仅在当前没有其它界面时打开 `InventoryScreen`

## 未包含内容

本阶段不包含以下功能，当前也没有实现或恢复：

* `rightClickClose / 关闭盒子界面`
* 末影箱额外同步
* shulker bundling insert / pickup / transfer / extract
* 鼠标拖拽批量行为
* Bundle 独立菜单

## 验证

已执行：

```powershell
.\gradlew.bat build
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

结果：

* 三条命令均构建成功

## 风险与待确认

* 尚未进行 Minecraft 客户端实机验证，仍需人工确认 inventory trigger 关闭后在两端环境下都能稳定回到玩家背包
* 目前客户端 reopen 采用短暂 pending 窗口，只在当前没有其它界面时执行；如后续出现特定平台时序差异，需要结合实机日志再做小范围调整
