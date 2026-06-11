# stage-0.2.0-p2.2-ender-chest-reopen-inventory

## 概要

本阶段把末影箱 quick-open 的 inventory-trigger reopen 体验补齐到与现有可重开界面一致。

注意：该阶段功能已因复制物品风险在后续阶段删除，当前行为以 `stage-0.2.0-p2x-remove-reopen-inventory.md`、`release-0.2.0.md` 和源码为准。

现在当玩家从背包界面悬停末影箱，并通过快捷键或右键打开 quick-open 菜单时，在正常按 `Esc` / `E` 关闭后，也会像潜影盒一样回到玩家背包界面。

同时说明一点：工作台、切石机、铁砧在本仓库当前实现里，本来就已经具备“背包触发打开后，正常关闭回到玩家背包”的体验；这次并不是给它们新增这个能力，只是末影箱也对齐了同一类 inventory-trigger 行为。

本阶段不修改 reopen policy、网络包、服务端关闭/保存逻辑，也不包含末影箱额外客户端同步。

## 修改内容

### common

* 将 `BuiltinQuickOpenables.ENDER_CHEST` 的 `reopenPlayerInventoryAfterClose` 从 `false` 调整为 `true`
* 复用现有 `QuickOpenReturnToInventoryPolicy` 的 trigger 判定，不新增分支逻辑

## 当前行为边界

* inventory trigger 的末影箱：
  `INVENTORY_KEYBIND` / `INVENTORY_RIGHT_CLICK` 打开后，正常按 `Esc` / `E` 关闭会回到玩家背包
* hand trigger 的末影箱：
  `HAND_KEYBIND` / `HAND_RIGHT_CLICK` 打开后，关闭仍不 reopen 背包
* 工作台、切石机、铁砧在当前实现里也保留 inventory-trigger reopen 行为
* `switch_open` 收尾仍不 reopen
* `host invalidated` / `disconnect` / `death` / `dimension change` 仍不 reopen
* quick-open 切换打开过程中，不会中途弹回玩家背包
* 末影箱库存来源仍然是玩家自己的 `EnderChestInventory`
* 本阶段不包含末影箱额外 full/slot sync

## 未包含内容

本阶段不包含以下功能，当前也没有新增或修改：

* `rightClickClose`
* reopen S2C 发送条件调整
* `HostSlotRef` 映射
* 宿主槽位锁定
* session close reason
* 潜影盒写回逻辑
* bundling
* mouse dragged
* Bundle 菜单
* 末影箱额外客户端同步

本阶段也没有专门为工作台、切石机、铁砧新增 reopen 逻辑，它们只是沿用仓库里已有的同类行为。

## 验证

已执行：

```powershell
.\gradlew.bat build
```

结果：

* 构建通过

## 建议人工测试

### Forge 1.20.1

* 背包悬停末影箱，快捷键打开，`Esc` 关闭，应回到背包
* 背包悬停末影箱，右键打开，`E` 关闭，应回到背包
* 手持末影箱快捷键打开，`Esc` 关闭，不应回到背包
* 手持末影箱右键打开，`Esc` 关闭，不应回到背包
* 从末影箱 quick-open 切换打开潜影盒，不应中途弹回背包
* 从潜影盒 quick-open 切换打开末影箱，不应中途弹回背包
* 两个玩家在专用服务器上分别打开自己的末影箱，确认 reopen 只影响本人，末影箱内容不串
* 专用服务器启动和进服无 client class loading 报错

### NeoForge 1.21.1

* 背包悬停末影箱，快捷键打开，`Esc` 关闭，应回到背包
* 背包悬停末影箱，右键打开，`E` 关闭，应回到背包
* 手持末影箱快捷键打开，`Esc` 关闭，不应回到背包
* 手持末影箱右键打开，`Esc` 关闭，不应回到背包
* 从末影箱 quick-open 切换打开潜影盒，不应中途弹回背包
* 从潜影盒 quick-open 切换打开末影箱，不应中途弹回背包
* 两个玩家在专用服务器上分别打开自己的末影箱，确认 reopen 只影响本人，末影箱内容不串
* 专用服务器启动和进服无 client class loading 报错

## 待人工确认项

* 尚未进行 Minecraft 客户端实机测试，仍需人工确认 Forge 1.20.1 与 NeoForge 1.21.1 下末影箱 inventory trigger 关闭后都能稳定回到玩家背包
* 尚未进行多人专用服务器实机测试，仍需人工确认 reopen 只影响本人且末影箱内容不会串用
