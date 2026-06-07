# stage-0.2.0-p2.1-reopen-inventory-visual-polish

## 概要

本阶段只调整客户端 `reopen inventory` 的处理时序，目标是在服务端 `reopen` S2C 到达后，尽快在客户端主线程补一次 pending reopen 处理，尽量减少 quick-open 菜单关闭到玩家背包重新打开之间的空屏帧。

本阶段不修改服务端保存逻辑、不修改 reopen 触发条件，也不做客户端预测 reopen。

## 修改内容

### Forge 1.20.1

* `ForgeQuickShulkerClient` 新增公开 helper：收到 `reopen inventory` S2C 后，先写入 `ReopenPlayerInventoryQueue`，再立即尝试处理一次 pending reopen
* `ForgeQuickShulkerNetwork` 的 `reopen inventory` 客户端 handler 改为调用该 helper，而不是只 schedule 等下一次 client tick

### NeoForge 1.21.1

* `NeoForgeQuickShulkerClient` 新增同等 helper：收到 `reopen inventory` S2C 后，先写入 `ReopenPlayerInventoryQueue`，再立即尝试处理一次 pending reopen
* `NeoForgeQuickShulkerNetwork` 的 `reopen inventory` 客户端 handler 改为调用该 helper，而不是只 schedule 等下一次 client tick

## 行为边界

* 仍然只以服务端 `reopen inventory` S2C 作为 reopen 依据
* 仍然保留 `ReopenPlayerInventoryQueue` 的 pending 机制和 client tick 兜底处理
* 如果当前 `screen == null`，会尽快打开 `InventoryScreen`
* 如果当前仍是 quick-open 菜单或其他非空界面，不会强行覆盖，继续等待 tick 兜底
* hand trigger、ender chest、`switch_open` 等原本不应 reopen 的路径，本阶段不改变

## 已知限制

`reopen inventory` 依赖服务端 S2C 确认，关闭 quick-open 菜单后仍可能出现极短暂背景亮度变化；该问题不影响数据保存和功能使用。

## 验证

已执行：

```powershell
.\gradlew.bat build
```

结果：

* 构建通过

## 待人工确认

* 尚未进行 Minecraft 客户端实机验证，仍需人工确认 Forge / NeoForge 下按 `Esc` / `E` 关闭 quick-open 菜单时，背景短暂变亮现象是否已明显改善
