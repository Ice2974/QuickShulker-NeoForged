# QuickShulker NeoForged 1.0.0 发布整理

## 版本定位

1.0.0 是 QuickShulker-NeoForged 的首个正式发布版本，目标是在 Forge 1.20.1 与 NeoForge 1.21.1 上提供完整、稳定的 quick-open 与 bundling 核心体验：

* 潜影盒 / 末影箱 quick-open
* 潜影盒 bundling（insert / pickup insert / extract / transfer）
* 末影箱 bundling（单次右键 insert / pickup insert / extract）
* mouse dragged 批量收纳 / 批量放出
* 配置项补齐与范围收敛
* 以数据安全优先的全链路修正

本版本不新增与移植目标无关的新玩法。

## 新增功能

### 潜影盒 quick-open

* 背包内打开潜影盒
* 快捷键打开、鼠标悬停右键 / 按键、无界面手持右键 / 快捷键
* 服务端重校验、宿主槽位锁定、同一宿主重复打开拒绝、不同宿主切换安全收尾

### 末影箱 quick-open

* 背包内打开末影箱
* 末影箱内容始终以服务端玩家自己的 EnderChestInventory 为准，不写入宿主 ItemStack
* full sync / slot sync 同步客户端显示

### 潜影盒 bundling

* insert：鼠标拿普通物品，右键背包里的单个潜影盒，将鼠标物品插入潜影盒
* pickup insert：鼠标拿单个潜影盒，右键背包里的普通物品，将槽位物品插入鼠标潜影盒
* extract：鼠标拿单个潜影盒，右键背包空槽，从潜影盒提取第一组非空物品到空槽
* transfer：鼠标来源潜影盒 -> 悬停目标潜影盒内容转移

### 末影箱 bundling

* ender chest insert：鼠标拿普通物品，右键背包里的单个末影箱，将鼠标物品收入玩家末影箱
* ender chest pickup insert：手持末影箱右键背包普通物品，将槽位物品收入玩家末影箱
* ender chest extract：手持末影箱右键空槽，从玩家末影箱取出一组物品到空槽
* 末影箱不能收入末影箱
* 末影箱向潜影盒内容槽放出物品时跳过潜影盒

### mouse dragged 批量行为

* 潜影盒 mouse dragged 批量 insert / extract
* 末影箱 mouse dragged 批量 pickup insert / extract
* 从后往前扫描、创造模式 carried 同步、drag session 续包去重

### 配置项

* 双平台配置 spec、snapshot / apply、ConfigView 与配置页面状态均已同步
* 末影箱 bundling 各动作可分别开关
* 旧配置文件缺少新字段时按默认值补齐

## 重要行为变更

* reopen inventory 功能已彻底删除，quick-open 关闭后不会自动回背包
* rightClickClose / 右键关闭当前盒子界面 不包含在本版本中
* Bundle 独立菜单不包含在本版本中

## 未包含功能

1.0.0 当前明确不包含以下内容：

* rightClickClose / 右键关闭当前盒子界面
* Bundle 独立菜单
* reopen inventory

## 验证说明

建议或应执行的验证如下；若未实际执行，应按“待人工确认”处理：

* git diff --check
* gradlew.bat :common:test
* gradlew.bat :forge-1.20.1:compileJava
* gradlew.bat :neoforge-1.21.1:compileJava
* gradlew.bat build
* Forge 1.20.1 实机测试
* NeoForge 1.21.1 实机测试
* 专用服务器测试
* 生存 / 创造模式 bundling 测试
* 复制 bug 复现路径回归测试

## 最终人工测试清单

以下测试项用于 1.0.0 发布前最终回归，Forge 1.20.1 与 NeoForge 1.21.1 均应分别覆盖：

1. quick-open 潜影盒保存
2. quick-open 末影箱同步
3. quick-open 工作台 / 切石机 / 铁砧
4. 手持快捷键 / 手持右键
5. 背包悬停快捷键 / 背包悬停右键
6. 同一宿主重复打开拒绝
7. 不同宿主切换打开
8. 当前宿主槽位锁定
9. 生存模式潜影盒 bundling insert / pickup insert / extract / transfer
10. 创造模式潜影盒 bundling insert / pickup insert / extract / transfer
11. 末影箱 bundling insert / pickup insert / extract
12. 末影箱不能收入末影箱
13. 末彐箱向潜影盒内容槽放出物品时跳过潜彐盒
14. mouse dragged 批量收纳 / 批量放出
15. 创造模式末彐箱 bundling
16. 配置关闭后对应 bundling 不触发
17. 原版容器界面进入 quick-open 后关闭，不会自动回背包
18. 复制 bug 复现路径回归（含 P8.1：创造模式 bundling 后关 / 重开背包不复制潜影盒 / 末影箱；末影箱拖拽不能对当前 QuickShulker 宿主槽位 bundling；P8.2：创造模式潜影盒 / 末影箱 bundling 后不关背包直接切换 quick-open 页面，再执行容器插入，关闭后不复制；P8.3：创造模式从创造物品列表拿起潜影盒做 bundling 后切换 quick-open，在 quick-open 内对潜影盒重复 bundling 操作，关闭背包重开不复制潜影盒）
19. 双人专用服务器末彐箱不串箱
20. 双人专用服务器 bundling 只影响本人

## 待人工确认项

以下内容当前仍需维护者人工确认：

* 无法确认最低 Forge / NeoForge loader 版本
* 无法进行 Minecraft 游戏内测试
* 无法进行多人服务器测试
* 无法确认 Modrinth 发布元数据
