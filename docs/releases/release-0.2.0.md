# QuickShulker NeoForged 0.2.0 发布整理

## 版本定位

0.2.0 是 QuickShulker-NeoForged 当前发布前的稳定增强版本，重点放在以下几个方向：

* 末影箱 quick-open 额外同步
* shulker bundling 右键交互补齐
* 配置项补齐与范围收敛
* 以数据安全优先的小范围修正

本版本不新增与移植目标无关的新玩法，仍以 Forge 1.20.1 与 NeoForge 1.21.1 的稳定移植为主。

## 新增功能

### 末影箱同步

当前 quick-open 末影箱会额外同步客户端显示状态，包括：

* full sync：打开末影箱 quick-open 后发送完整 27 槽快照
* slot sync：末影箱打开期间按槽位增量同步变化
* 末影箱内容始终以服务端玩家自己的 `EnderChestInventory` 为准
* 末影箱内容不会写入宿主 `ItemStack`

这项同步的目标是让客户端显示与服务端真实末影箱库存保持一致，同时继续遵守“末影箱不绑定到宿主物品”的现有语义。

### shulker bundling

0.2.0 当前已完成以下四类右键 bundling 行为：

* `insert`：鼠标拿普通物品，右键背包里的单个潜影盒，将鼠标物品插入潜影盒
* `pickup insert`：鼠标拿单个潜影盒，右键背包里的普通物品，将槽位物品插入鼠标潜影盒
* `extract`：鼠标拿单个潜影盒，右键背包空槽，从潜影盒提取第一组非空物品到空槽
* `transfer`：鼠标拿起的单个潜影盒作为来源潜影盒，右键悬停的玩家背包单个潜影盒作为目标潜影盒，将来源内容尽可能转移到目标潜影盒

其中 `transfer` 当前按 quickshulker-multi 语义实现，方向为“鼠标来源潜影盒 -> 悬停目标潜影盒”，不是反向转移。

### 配置项补齐

0.2.0 已补齐当前移植范围内需要保留的配置项，并明确移除了 `rightClickClose` 这类当前不准备启用的高风险历史语义，避免配置名与实际行为不一致。

## 重要行为变更

`reopen inventory` 功能已在后续阶段彻底删除。

当前统一行为为：

* QuickShulker 页面按 `E` / `Esc` 会直接关闭界面
* 不会在关闭后自动回到玩家背包
* 从原版容器界面进入 quick-open 后，关闭 quick-open 也不会自动回背包

删除原因是该功能在原版容器界面进入 quick-open 后存在复制物品风险。本版本选择以数据安全优先，保留更简单、更可控的关闭路径。

## 未包含功能

0.2.0 当前明确不包含以下内容：

* `rightClickClose / 右键关闭当前盒子界面`
* mouse dragged 批量行为
* Bundle 独立菜单
* 末影箱 bundling
* 配置 GUI

这些内容不应视为已完成能力，也不应写入当前发布说明的已实现功能列表。

## 已知限制

当前已知限制如下：

* 配置 GUI / Mods 页面配置按钮可能仍不可用，但配置文件本身可用
* mouse dragged 批量行为暂未启用
* 0.2.0 当前优先保证右键 bundling 的数据安全，不扩展到更高风险的批量交互

## 验证说明

建议或应执行的验证如下；若未实际执行，应按“待人工确认”处理：

* `.\gradlew.bat build`
* `.\gradlew.bat :forge-1.20.1:build`
* `.\gradlew.bat :neoforge-1.21.1:build`
* Forge 1.20.1 实机测试
* NeoForge 1.21.1 实机测试
* 专用服务器测试
* 生存 / 创造模式 bundling 测试
* 复制 bug 复现路径回归测试

本次发布整理文档阶段未在该文档中将上述项目标记为“已通过”；除命令行静态检查外，其余均待人工确认。

## 最终人工测试清单

以下测试项用于 0.2.0 发布前最终回归，Forge 1.20.1 与 NeoForge 1.21.1 均应分别覆盖：

1. quick-open 潜影盒保存
2. quick-open 末影箱同步
3. quick-open 工作台 / 切石机 / 铁砧
4. 手持快捷键 / 手持右键
5. 背包悬停快捷键 / 背包悬停右键
6. 同一宿主重复打开拒绝
7. 不同宿主切换打开
8. 当前宿主槽位锁定
9. 生存模式 bundling `insert` / `pickup insert` / `extract` / `transfer`
10. 创造模式 bundling `insert` / `pickup insert` / `extract` / `transfer`
11. 配置关闭后对应 bundling 不触发
12. 原版容器界面进入 quick-open 后关闭，不会自动回背包
13. 复制 bug 复现路径回归
14. 双人专用服务器末影箱不串箱
15. 双人专用服务器 bundling 只影响本人

## 待人工确认项

以下内容当前仍需维护者人工确认：

* `.\gradlew.bat build`、`.\gradlew.bat :forge-1.20.1:build`、`.\gradlew.bat :neoforge-1.21.1:build` 是否在当前发布候选状态下全部通过
* Forge 1.20.1 实机测试结果
* NeoForge 1.21.1 实机测试结果
* 专用服务器双人隔离测试结果
* 生存 / 创造模式 bundling 四种行为的最终回归结果
* 复制 bug 复现路径回归结果
