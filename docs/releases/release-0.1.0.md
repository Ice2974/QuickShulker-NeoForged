# QuickShulker NeoForged 0.1.0 发布验证记录

本文档用于记录 `0.1.0` 当前发布范围、构建验证和仍需人工确认的事项。

## 发布范围

纳入 `0.1.0` 的内容：

- Forge `1.20.1`
- NeoForge `1.21.1`
- `client + server` 双端安装目标
- `shulker_box` quick-open
- `ender_chest` quick-open
- `crafting_table` quick-open
- `stonecutter` quick-open
- `anvil` quick-open
- 手持快捷键打开
- 无界面手持右键打开
- 背包 / 容器界面悬停快捷键打开
- 背包 / 容器界面悬停右键打开
- 同一宿主重复打开拒绝
- 不同宿主之间切换打开
- 宿主槽位锁定
- 普通物品保留原版副手交换行为
- 会移动当前宿主的副手交换会被拦截
- quick-open 切换时恢复鼠标位置
- 潜影盒关闭保存
- 末影箱使用玩家真实 `EnderChestInventory`

不纳入 `0.1.0` 的内容：

- Bundle 独立菜单
- Bundle quick-open
- Bundle bundling
- 鼠标拖拽批量行为
- `rightClickClose`
- `reopen inventory`
- 末影箱 bundling
- 额外 Bundle / mouse dragged 跨版本维护逻辑
- 完整多人压力测试
- 完整创造模式边界专项覆盖

## 当前决定说明

`1.0.0` 已明确主动跳过 Bundle 相关功能，而不是“暂未接入、后续默认补齐”。

原因：

- 目标版本中的 Bundle 仍属于实验性 / 非主线稳定玩法
- Bundle 菜单、保存、跨版本内容读写会显著增加维护成本
- 当前版本优先保证 shulker / ender chest quick-open、已有 shulker bundling、保存链路和 slot 映射稳定

## 构建命令

工作区已执行或可用于复核的命令：

```powershell
.\gradlew.bat build
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

## 当前已知状态

- Forge 目标参数：`Minecraft 1.20.1`、`Forge 47.x`、`Java 17`
- NeoForge 目标参数：`Minecraft 1.21.1`、`NeoForge 21.x`、`Java 21`
- 当前版本不应向玩家暴露 Bundle 支持入口
- 当前版本不恢复 `rightClickClose` 或 `reopen inventory`

## 人工回归重点

### Forge 1.20.1

- 单人存档启动
- 本地 Forge 服务端启动
- 双端安装预期
- 潜影盒 quick-open
- 末影箱 quick-open
- 工作台 / 切石机 / 铁砧 quick-open
- 手持快捷键 / 手持右键 / 悬停快捷键 / 悬停右键
- 同一宿主重复打开拒绝
- 不同宿主切换打开
- 宿主槽位锁定
- 潜影盒关闭保存
- bundling 配置项仍只作用于潜影盒 bundling

### NeoForge 1.21.1

- 单人存档启动
- 本地 NeoForge 服务端启动
- 双端安装预期
- 潜影盒 quick-open
- 末影箱 quick-open
- 工作台 / 切石机 / 铁砧 quick-open
- 手持快捷键 / 手持右键 / 悬停快捷键 / 悬停右键
- 同一宿主重复打开拒绝
- 不同宿主切换打开
- 宿主槽位锁定
- 潜影盒关闭保存
- bundling 配置项仍只作用于潜影盒 bundling

## 已知限制

- `0.1.0` 优先聚焦稳定可用的核心 quick-open 行为
- Bundle 相关功能在 `1.0.0` 主动跳过
- mouse dragged 批量行为未实现
- `rightClickClose` 未恢复
- `reopen inventory` 未恢复
- 末影箱 bundling 未实现
- 完整多人压力测试仍待补充
- 创造模式复杂边界的完整专项验证仍待补充

## 待人工确认项

- 无法确认最低 Forge `47.x` loader 版本
- 无法确认最低 NeoForge `21.x` loader 版本
- 无法在当前工作区内完成 Minecraft 游戏内测试
- 无法在当前工作区内完成多人服务器测试
- 当前第三方来源说明是否已充分覆盖所有实际改写片段
