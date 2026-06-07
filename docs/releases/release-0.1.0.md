# QuickShulker NeoForged 0.1.0 发布验证记录

本文档用于记录 `0.1.0` 的发布准备验证状态。

本文档区分以下几类信息：

- 当前工作区内实际完成的代码 / 构建验证
- 维护者反馈的游戏内实机测试结果
- 发布前仍需人工确认的事项

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
- 工作台 / 切石机 / 铁砧按原版风格菜单行为打开

不纳入 `0.1.0` 的内容：

- Bundle 独立菜单
- 鼠标拖拽批量行为
- `rightClickClose` 相关功能
- `reopen inventory` S2C
- 末影箱额外客户端同步协议
- 完整多人压力测试
- 完整创造模式边界专项覆盖
- Modrinth / CurseForge 高级发布元数据自动化

## 构建命令

本工作区内实际执行过的命令：

- `.\gradlew.bat build`

如果发布前需要再次复核，可使用：

- `.\gradlew.bat clean build`
- `.\gradlew.bat :forge-1.20.1:build`
- `.\gradlew.bat :neoforge-1.21.1:build`

## 构建结果

本工作区内已确认：

- `gradle.properties` 中 `mod_version=0.1.0`
- Forge 目标参数为 `Minecraft 1.20.1`、`Forge 47.x`、`Java 17`
- NeoForge 目标参数为 `Minecraft 1.21.1`、`NeoForge 21.x`、`Java 21`
- 发布产物文件名解析结果为：
  - `versions/forge-1.20.1/build/libs/QuickShulker-Forge-v0.1.0-mc1.20.1.jar`
  - `versions/neoforge-1.21.1/build/libs/QuickShulker-NeoForge-v0.1.0-mc1.21.1.jar`

## 验证来源

维护者反馈：

- 维护者已反馈阶段 7A.1 问题修正后的版本通过了实机测试

工作区侧验证：

- 已在当前仓库工作区内完成构建与产物命名验证
- 除非明确注明来自维护者反馈，否则本文档不额外声称新的游戏内或多人测试结果

## Forge 1.20.1 测试项

属于维护者实机验证背景范围的项目：

- 单人存档启动
- 本地 Forge 服务端启动
- 双端安装预期
- 潜影盒 quick-open
- 末影箱 quick-open
- 工作台 quick-open
- 切石机 quick-open
- 铁砧 quick-open
- 手持快捷键打开
- 无界面手持右键打开
- 悬停快捷键打开
- 悬停右键打开
- 同一宿主重复打开拒绝
- 不同宿主切换打开
- 宿主槽位锁定行为
- 潜影盒关闭保存

如果发布前还有时间，建议再人工复核：

- 将最终 jar 放入干净的 Forge `1.20.1` 客户端环境
- 将最终 jar 放入干净的 Forge `1.20.1` 专用服务端环境
- 再跑一轮 `Esc` 关闭、`E` 关闭、宿主切换和副手宿主保护回归

## NeoForge 1.21.1 测试项

属于维护者实机验证背景范围的项目：

- 单人存档启动
- 本地 NeoForge 服务端启动
- 双端安装预期
- 潜影盒 quick-open
- 末影箱 quick-open
- 工作台 quick-open
- 切石机 quick-open
- 铁砧 quick-open
- 手持快捷键打开
- 无界面手持右键打开
- 悬停快捷键打开
- 悬停右键打开
- 同一宿主重复打开拒绝
- 不同宿主切换打开
- 宿主槽位锁定行为
- 潜影盒关闭保存

如果发布前还有时间，建议再人工复核：

- 将最终 jar 放入干净的 NeoForge `1.21.1` 客户端环境
- 将最终 jar 放入干净的 NeoForge `1.21.1` 专用服务端环境
- 再跑一轮 `Esc` 关闭、`E` 关闭、宿主切换和副手宿主保护回归

## 数据安全重点

`0.1.0` 发布前应重点人工确认：

- 潜影盒正常关闭后内容能正确保存
- 潜影盒取空后仍能正确保存为空
- 同一宿主重复打开会被拒绝
- 从宿主 `A` 切换到宿主 `B` 时不会发生错误写回
- 宿主失效时不会把内容写回错误目标
- 危险菜单操作不能把当前宿主移走
- 普通非宿主物品仍可正常进行副手交换
- 末影箱 quick-open 使用玩家真实末影箱库存

## 已知限制

- `0.1.0` 优先聚焦稳定可用的核心 quick-open 行为
- Bundle 独立菜单尚未实现
- 鼠标拖拽批量行为尚未实现
- `rightClickClose` 当前暂不移植
- `reopen inventory` S2C 尚未实现
- 末影箱额外客户端同步协议尚未实现
- 完整多人压力测试仍待补充
- 创造模式复杂边界的完整专项验证仍待补充

## 待人工确认项

- 公开发布前对多人 / 专用服务端可用性的最终把握
- 创造模式复杂边界场景的最终信心
- 当前第三方来源说明是否已经充分覆盖所有实际改写片段
- 对外支持说明中 Forge `47.x` 的最低 loader 表述
- 对外支持说明中 NeoForge `21.x` 的最低 loader 表述
