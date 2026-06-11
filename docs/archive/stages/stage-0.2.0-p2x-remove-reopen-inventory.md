# stage-0.2.0-p2x-remove-reopen-inventory

## 变更目的

因发现 quick-open 菜单关闭后自动回到玩家背包的 `reopen inventory` 链路可能导致复制物品风险，本阶段彻底删除该功能。

当前行为统一为：

* QuickShulker 潜影盒页面按 `E` / `Esc` 后直接关闭界面，不自动回到玩家背包
* QuickShulker 末影箱页面按 `E` / `Esc` 后直接关闭界面，不自动回到玩家背包
* QuickShulker 工作台 / 切石机 / 铁砧页面按 `E` / `Esc` 后直接关闭界面，不自动回到玩家背包
* 从原版容器界面进入 quick-open 后，关闭 quick-open 也不会强制打开玩家背包

## 本阶段修改

### common

* 删除 `common/.../network/ReopenPlayerInventoryIntent.java`
* 删除 `common/.../network/ReopenPlayerInventoryQueue.java`
* 删除 `common/.../open/QuickOpenReturnToInventoryPolicy.java`
* 删除 `QuickOpenRequest.shouldReturnToPlayerInventory`
* 删除 `MenuOpenIntent.reopenPlayerInventoryAfterClose`
* 删除 `QuickOpenableType.reopenPlayerInventoryAfterClose`
* 删除 `BuiltinQuickOpenables` 内置 quick-open 类型中的 reopen 构造参数

### Forge 1.20.1

* 删除 `ForgeReopenPlayerInventoryPacket`
* 删除 `ForgeQuickShulkerNetwork` 中的 reopen S2C 注册、发送和客户端反射处理
* 删除 `ForgeShulkerSessionManager` 在 session 正常关闭后发送 reopen S2C 的逻辑
* 删除 `ForgeQuickOpenHandler` 中对 reopen 返回策略的计算
* 删除 `ForgeQuickShulkerClient` 中 pending reopen 队列、client tick reopen 处理和 `InventoryScreen` 强制 reopen

### NeoForge 1.21.1

* 删除 `NeoForgeReopenPlayerInventoryPayload`
* 删除 `NeoForgeQuickShulkerNetwork` 中的 reopen S2C 注册、发送和客户端反射处理
* 删除 `NeoForgeShulkerSessionManager` 在 session 正常关闭后发送 reopen S2C 的逻辑
* 删除 `NeoForgeQuickOpenHandler` 中对 reopen 返回策略的计算
* 删除 `NeoForgeQuickShulkerClient` 中 pending reopen 队列、client tick reopen 处理和 `InventoryScreen` 强制 reopen

## 明确保留

本阶段没有修改以下能力：

* quick-open 潜影盒
* quick-open 末影箱
* quick-open 工作台 / 切石机 / 铁砧
* 潜影盒保存与写回逻辑
* 末影箱 full sync / slot sync
* HostSlotRef 映射
* 宿主槽位锁定
* 同一宿主重复打开拒绝
* 不同宿主切换打开前安全收尾
* shulker bundling insert / pickup

## 不在本阶段范围

本阶段不包含以下功能的实现、恢复或扩展：

* `rightClickClose`
* extract
* shulker-to-shulker transfer
* mouse dragged 批量行为
* Bundle 菜单

## 验证

建议人工重点回归：

* 从原版容器界面进入 quick-open 后，按 `E` / `Esc` 关闭时不再自动回到玩家背包
* 潜影盒和末影箱重新打开后内容不出现复制
* 末影箱 full sync / slot sync 仍然正常
* bundling insert / pickup 仍然正常
