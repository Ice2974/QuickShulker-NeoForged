# stage-1.0.0-p6.2-ender-chest-on-ender-chest-vanilla-behavior

本阶段修复“鼠标拿起单个末影箱，右键悬停槽内另一个末影箱”时，QuickShulker 仍可能进入末影箱 bundling 输入抑制或拖拽续包路径，导致槽内末影箱短暂消失的问题。

## 背景

阶段 6.1 已将末影箱 mouse dragged 服务端续包重新纳入 `quickEnderChest()` 门控。本次继续收窄末影箱 bundling 的输入边界：末影箱本身不能被收入末影箱，因此“拿末影箱右键末影箱”应完全交给原版处理。

本次范围只覆盖 carried 为单个末影箱、hovered slot 也是末影箱的场景。拿普通物品或潜影盒右键末影箱、拿末影箱右键普通物品或潜影盒、拿末影箱右键空槽，以及对应拖拽普通物品 / 潜影盒 / 空槽的既有行为保持不变。

## 修改

- common `EnderChestBundlingRules.resolveOperation` 现在在解析 `INSERT` / `PICKUP_INSERT` 前检查 `canInsertIntoEnderChest`，不再只靠“悬停末影箱”或“手持末影箱”推导操作。
- common 单元测试新增末影箱不能收入末影箱的 resolve / insert / pickupInsert 覆盖，并修正 Fake adapter，使其与平台实现一致地拒绝末影箱本体。
- Forge 1.20.1 客户端 `ForgeQuickShulkerClient` 新增显式判断：当 carried 是单个末影箱且 hovered stack 是末影箱时，`determineEnderChestBundlingIntent` 返回空，不发送 `ENDER_CHEST_INSERT`、`ENDER_CHEST_PICKUP_INSERT` 或 `ENDER_CHEST_EXTRACT`。
- NeoForge 1.21.1 客户端 `NeoForgeQuickShulkerClient` 做同等处理。
- 双平台 `trySendMouseDraggedBundlingIntent` 对同一场景直接清理当前 drag 状态并放行原版输入，不发送任何 `MOUSE_DRAG_ENDER_CHEST_*` 续包，也不继续使用 release 抑制吞掉原版释放。
- Forge / NeoForge 服务端在 `handleEnderChestInsert` 和 `handleEnderChestPickupInsert` 中显式拒绝末影箱作为待收入物。即使异常客户端伪造请求，服务端也会在写入玩家末影箱、缩减目标槽或写回 cursor / slot 之前返回。

## 数据安全

- 客户端只在明确生成合法 QuickShulker bundling intent 时才取消原版右键。
- 末影箱收入末影箱在 common 规则、客户端输入层和服务端最终校验三处均被拒绝。
- 服务端拒绝路径只读当前 carried / target stack 并记录 debug，不写入 `EnderChestInventory`，不移动、删除或写回任何物品。

## 验证

- `git diff --check`：通过。
- `.\gradlew.bat :common:test`：通过。
- `.\gradlew.bat :forge-1.20.1:compileJava`：通过。
- `.\gradlew.bat :neoforge-1.21.1:compileJava`：通过。编译过程仅输出 javac 过时 API 提示，未失败。

## 待人工确认

- 仍需维护者进行 Minecraft 游戏内确认：拿单个末影箱右键槽内末影箱时不再出现短暂消失，且原版右键行为未被吞。
- 仍需维护者进行多人服务器确认。
