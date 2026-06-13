# Stage 1.0.0 P6.3 Bundling Config Page

本阶段整理配置页面的 Quick Open / Bundling 显示与末影箱收纳配置门控。

## 修改内容

- 配置页面中原 `Shulker Bundling` 页签改为统一的 `Bundling` 页签；中文显示为“收纳”。
- 中文 `Quick Open` 页签显示改为“快速打开”，页面标题改为“启用的快速打开目标”。
- `Bundling` 页面标题改为 `Bundling Options`；中文显示为“收纳选项”。
- `Quick Open` 和 `Bundling` 页面子选项起始高度从 96 调整为 78，与“基础功能”页第一行选项对齐。
- `Bundling` 页面改为双列管理：
  - 左侧为潜影盒收纳配置。
  - 右侧为末影箱收纳配置。
- 中文配置项去掉玩家可见的 `quick-open`、`bundling`、`Bundling`、`Quick Open`、`Shulker Box`、`Ender Chest` 等英文残留。
- 英文配置项缩短为 `Shulker Insert`、`Ender Insert`、`Shulker Drag Batch`、`Ender Drag Batch` 等较短文案，降低配置界面拥挤风险。

## 新增末影箱配置

新增 4 个末影箱收纳配置项，默认值均为 `true`，保持阶段 6 已实现的默认玩家体验：

- `enderChestBundlingInsert`：控制 `ENDER_CHEST_INSERT`，即右键将 carried 物品收入 hovered 末影箱。
- `enderChestBundlingPickup`：控制 `ENDER_CHEST_PICKUP_INSERT`，即手持末影箱右键将 hovered 物品收入玩家末影箱。
- `enderChestBundlingExtract`：控制 `ENDER_CHEST_EXTRACT`，即手持末影箱右键空槽从玩家末影箱取出物品。
- `enderChestMouseDragged`：控制 `MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT` 和 `MOUSE_DRAG_ENDER_CHEST_EXTRACT`。

`quickEnderChest` 仍保留在 Quick Open / 快速打开 页面，用于末影箱 quick-open 目标开关，不再作为末影箱收纳配置页的显示项。

## 配置兼容

Forge 1.20.1 与 NeoForge 1.21.1 的 config spec、snapshot、apply、ConfigView 和配置页面状态均已同步新增字段。旧配置文件缺少新增末影箱配置项时，平台 config spec 会按默认值 `true` 补齐，不应导致启动失败。

旧潜影盒配置项未改 key，语义保持不变：

- `supportsBundlingInsert`
- `supportsBundlingPickup`
- `supportsBundlingTransfer`
- `supportsBundlingExtract`
- `supportsMouseDragged`

`supportsMouseDragged` 注释已更新为只描述潜影盒拖拽批量操作，不再写成末影箱不受影响。

## Gate 调整

- 客户端单次末影箱收纳 intent 由新增末影箱配置分别控制。
- 服务端单次 `ENDER_CHEST_INSERT`、`ENDER_CHEST_PICKUP_INSERT`、`ENDER_CHEST_EXTRACT` 分别由新增 insert、pickup、extract 配置控制，并保留服务端真实槽位、carried stack、末影箱内容写回等最终校验。
- 客户端和服务端末影箱 mouse dragged 续包由 `enderChestMouseDragged` 控制。
- 末影箱不能收入末影箱、防潜影盒嵌套、从后往前放出、创造模式 carried 同步等既有安全规则未改动。

## 验证

- `git diff --check`：通过。
- `.\gradlew.bat :common:test`：通过。
- `.\gradlew.bat :forge-1.20.1:compileJava`：通过。
- `.\gradlew.bat :neoforge-1.21.1:compileJava`：通过。

## 待人工确认

- 未进行 Minecraft 客户端内配置页面视觉确认，需要人工确认三页第一行选项在实际 GUI 中对齐。
- 未进行 Forge / NeoForge 游戏内末影箱收纳开关组合测试，需要人工确认新增配置与实际操作行为一致。
- 未进行多人服务器测试。
