# stage-3-mouse-dragged

> 历史说明：本阶段报告记录的是早期 mouse dragged shulker bundling 实现状态，已被
> `docs/stage-1.0.0-p4-bundling-menu-slots.md` 更新。当前行为不再支持“鼠标携带普通物品，
> 右键拖过多个潜影盒槽位逐个插入”；仅保留“鼠标携带单个潜影盒，右键拖过多个普通物品槽位
> 将物品收纳进该潜影盒”的拖拽路径。本文其余内容仅作历史参考。

本阶段实现 `supportsMouseDragged` 对应的潜影盒鼠标拖拽批量交互，并将该配置从兼容保留字段改为真实功能配置项。

## 实现内容

- Forge 1.20.1 与 NeoForge 1.21.1 客户端新增右键拖拽状态机。
- 单槽右键 bundling 仍沿用原有 `INSERT` / `PICKUP_INSERT` / `EXTRACT` / `TRANSFER` 行为。
- 拖拽经过新槽位时发送拖拽专用 action：
  - `MOUSE_DRAG_INSERT`
  - `MOUSE_DRAG_PICKUP_INSERT`
- 服务端收到拖拽 action 后重新读取玩家当前菜单、当前 carried stack 和目标 `HostSlotRef` 对应的真实 `ItemStack`。
- `supportsMouseDragged` 已在 Forge / NeoForge 配置 GUI 的潜影盒 bundling 页面重新开放。
- 配置注释和中英文 GUI 文案已更新为潜影盒鼠标拖拽批量操作，不再写兼容保留或未实现说明。

## 实际支持场景

1. 鼠标携带普通物品，右键按下一个潜影盒后继续拖过多个潜影盒槽位：
   - 第一格沿用现有单槽 `INSERT`。
   - 后续新槽位使用 `MOUSE_DRAG_INSERT`。
   - 服务端按真实 carried stack 和真实宿主潜影盒逐槽处理。

2. 鼠标携带单个潜影盒，右键按下一个普通物品槽后继续拖过多个普通物品槽位：
   - 第一格沿用现有单槽 `PICKUP_INSERT`。
   - 后续新槽位使用 `MOUSE_DRAG_PICKUP_INSERT`。
   - 服务端按真实 carried shulker 和真实目标槽位逐槽处理。

## 明确未支持场景

- Bundle 独立菜单。
- Bundle quick-open。
- Bundle bundling。
- Bundle 鼠标拖拽。
- 末影箱 bundling。
- 潜影盒拖过空槽批量提取。
- 潜影盒到潜影盒批量转移拖拽。
- 容器外部点击区域拖拽。
- 跨菜单切换时继续处理未完成拖拽。
- `rightClickClose`。
- `reopen inventory`。

## 安全策略

- 客户端只发送拖拽 action 与 `HostSlotRef`，不把目标槽位 `ItemStack` 当作可信来源。
- 生存模式服务端使用 `player.containerMenu.getCarried()` 作为真实 carried stack。
- 每个目标槽位都通过平台 `HostSlotResolver` 重新解析真实玩家背包 / hotbar / offhand 槽位。
- 无法解析为玩家可访问槽位时拒绝处理，不猜测 screen slot 与逻辑槽位映射。
- 当前 quick-open 宿主槽位仍通过 `HostIdentity.sameSlot` 拒绝 bundling / 拖拽操作。
- 服务端每次写入潜影盒内容后都写回真实宿主 `ItemStack` 并同步当前菜单。
- 创造模式批量拖拽路径服务端直接拒绝，避免复用创造模式 cursor 副本导致重复应用同一 payload。

## 修改文件

- `common/src/main/java/com/ice2974/quickshulkerneoforged/common/network/ShulkerBundlingAction.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeQuickShulkerConfig.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerConfigScreen.java`
- `versions/forge-1.20.1/src/main/resources/assets/quickshulker_neoforged/lang/en_us.json`
- `versions/forge-1.20.1/src/main/resources/assets/quickshulker_neoforged/lang/zh_cn.json`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeQuickShulkerConfig.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerConfigScreen.java`
- `versions/neoforge-1.21.1/src/main/resources/assets/quickshulker_neoforged/lang/en_us.json`
- `versions/neoforge-1.21.1/src/main/resources/assets/quickshulker_neoforged/lang/zh_cn.json`

## 验证命令和结果

- `.\gradlew.bat :versions:forge-1.20.1:compileJava`
  - 失败：实际项目没有 `:versions` 子项目。
- `.\gradlew.bat :versions:neoforge-1.21.1:compileJava`
  - 失败：实际项目没有 `:versions` 子项目。
- `.\gradlew.bat projects`
  - 通过，确认实际模块为 `:forge-1.20.1` 与 `:neoforge-1.21.1`。
- `.\gradlew.bat :forge-1.20.1:compileJava`
  - 通过。
- `.\gradlew.bat :forge-1.20.1:build`
  - 通过。
- `.\gradlew.bat :neoforge-1.21.1:build`
  - 通过，仅有既有弃用 API 编译提示。

## 待人工测试清单

- Forge 1.20.1 生存模式：普通物品拖过多个潜影盒，确认 carried 数量和每个潜影盒内容正确。
- Forge 1.20.1 生存模式：潜影盒拖过多个普通物品槽，确认普通槽位扣减和 carried shulker 内容正确。
- NeoForge 1.21.1 生存模式：普通物品拖过多个潜影盒，确认 carried 数量和每个潜影盒内容正确。
- NeoForge 1.21.1 生存模式：潜影盒拖过多个普通物品槽，确认普通槽位扣减和 carried shulker 内容正确。
- 双平台创造模式：确认拖拽批量路径不会产生复制，预期为不执行批量拖拽。
- 双平台专用服务器：确认拖拽包在服务端逐槽校验，客户端异常状态不会写回错误目标。
- 单槽 shulker bundling insert / pickup / extract / transfer 回归。
- quick-open shulker 关闭、`Esc`、`E` 保存回归。
- quick-open ender chest 仍使用玩家自己的 `EnderChestInventory`。
- 配置 GUI 可显示并修改 `supportsMouseDragged`。
- 关闭所有 quick-open 目标后，配置快捷键仍能打开配置界面。
- 确认 `rightClickClose` 没有恢复。
- 确认 `reopen inventory` 没有恢复。
- 确认 Bundle 相关功能没有出现入口。

## 待人工确认项

- Minecraft 游戏内实际拖拽手感和槽位高亮表现。
- 多人专用服务器实机测试结果。
- 创造模式下拒绝批量拖拽是否符合最终产品预期。
- 本阶段新增拖拽 action 属于对已有 bundling 包的小幅扩展，是否需要同步更新第三方来源说明仍待维护者确认。
