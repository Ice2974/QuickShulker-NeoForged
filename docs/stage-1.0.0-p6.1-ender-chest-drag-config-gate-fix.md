# stage-1.0.0-p6.1-ender-chest-drag-config-gate-fix

> 历史更新：阶段 6.3 已新增独立的末影箱收纳配置项。本文中 `quickEnderChest()` 同时控制末影箱 quick-open 与末影箱 bundling 的描述只代表阶段 6.1 当时状态；当前末影箱收纳以 `enderChestBundlingInsert`、`enderChestBundlingPickup`、`enderChestBundlingExtract`、`enderChestMouseDragged` 为准，`quickEnderChest` 只保留在 Quick Open 页面。

本阶段修复末影箱 mouse dragged 续包在服务端可能绕过 `quickEnderChest()` 开关的问题。

## 背景

阶段 6 已实现拿起单个末影箱拖拽批量收纳 / 批量放出：

- 拿末影箱拖过非空槽时，将目标槽物品收入玩家自己的 `EnderChestInventory`。
- 拿末影箱拖过空槽时，从玩家自己的 `EnderChestInventory` 从后往前放出物品。
- 向潜影盒内容槽放出时跳过潜影盒。
- 末影箱满时本槽操作失败，不回退为潜影盒 bundling。

源码包审查发现，服务端单次末影箱操作会经过 `handleEnderChestBundling()` 并检查 `quickEnderChest()`，但 `MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT` 和 `MOUSE_DRAG_ENDER_CHEST_EXTRACT` 会直接进入 mouse dragged handler，再委托到 pickup / extract 核心方法。续拖路径此前只检查 `supportsMouseDragged()` 和对应 bundling 开关，没有在服务端最终路径再次检查 `quickEnderChest()`。

日志目录中也能看到 NeoForge 1.21.1 侧连续发送 `MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT` / `MOUSE_DRAG_ENDER_CHEST_EXTRACT` 后，服务端按基础末影箱 action 继续处理的记录。这说明即使客户端异常发送续拖包，服务端核心方法也必须拥有独立门控。

## 修改

Forge 1.20.1 与 NeoForge 1.21.1 的服务端 handler 均新增平台内 helper：

- `supportsEnderChestBundling()`

当前 helper 返回对应平台配置视图的 `quickEnderChest()`。后续如果配置页面阶段新增独立末影箱 bundling 配置，可以只扩展此 helper。

同时将服务端最终门控下沉到末影箱核心方法：

- `handleEnderChestInsert`
- `handleEnderChestPickupInsert` 4 参数核心方法
- `handleEnderChestExtract` 4 参数核心方法

`handleEnderChestBundling()` 也改为调用同一 helper。这样以下所有末影箱 bundling 行为最终都受 `quickEnderChest()` 控制：

- `ENDER_CHEST_INSERT`
- `ENDER_CHEST_PICKUP_INSERT`
- `ENDER_CHEST_EXTRACT`
- `MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT`
- `MOUSE_DRAG_ENDER_CHEST_EXTRACT`

## 行为影响

本阶段不改变玩家可见拖拽行为，只修复服务端配置门控：

- `quickEnderChest=true` 时，阶段 6 已实现的末影箱拖拽行为保持不变。
- `quickEnderChest=false` 时，即使客户端异常发送末影箱单次或续拖 bundling 包，服务端也会拒绝。
- 潜影盒 mouse dragged 路径未修改。
- 未恢复 `rightClickClose`、Bundle 或 reopen inventory。
- 未做配置页面；独立末影箱 bundling 配置仍留到后续阶段。

## 数据安全边界

- 服务端不信任客户端配置状态，也不信任客户端是否正常发送拖拽起始包。
- 续拖包进入 pickup / extract 核心方法后会先检查 `supportsEnderChestBundling()`。
- 原有目标槽校验、carried 校验、末影箱内容写回和潜影盒内容槽 skip shulker 规则保持不变。
- 末影箱满时仍按阶段 6 行为失败，不回退潜影盒 bundling。

## 验证

- `git diff --check`：通过。仅有 Git 提示 Forge / NeoForge handler 工作区文件下次触碰时 LF 会被替换为 CRLF，无空白错误。
- `.\gradlew.bat :common:test`：通过，`BUILD SUCCESSFUL`。
- `.\gradlew.bat :forge-1.20.1:compileJava`：通过，`BUILD SUCCESSFUL`。
- `.\gradlew.bat :neoforge-1.21.1:compileJava`：通过。编译输出保留现有 deprecated API 提示，命令退出码为 0。

## 待人工确认

- Minecraft 游戏内测试未由本阶段自动完成。
- 多人服务器测试未由本阶段自动完成。
