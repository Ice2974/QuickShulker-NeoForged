# 阶段 6A.5：无界面手持潜影盒右键打开

> 历史开发记录：本文档记录早期阶段实现过程，不再作为当前实现状态的唯一依据。当前状态请以源码、README 和 `docs/releases/release-0.1.0.md` 为准。


本阶段在阶段 6A 已完成的输入与配置链路上，补齐了 `shulker_box` 的“无界面手持右键打开”入口。

目标仍然保持保守：

- 只处理 `shulker_box`
- 只补“无界面手持右键打开”
- 继续复用现有 `OpenHostItemIntent -> 服务端重校验 -> session 打开` 链路
- 不改 `references/`
- 不扩展到 `ender_chest`、`rightClickClose`、Bundle、工作台、切石机、铁砧、鼠标拖拽批量行为

## 本阶段实现了什么

- Forge 1.20.1：
  - 无界面时，主手持 1 个潜影盒，右键可打开
  - 无界面时，副手持 1 个潜影盒，右键可打开
- NeoForge 1.21.1：
  - 无界面时，主手持 1 个潜影盒，右键可打开
  - 无界面时，副手持 1 个潜影盒，右键可打开
- 两端都继续只发送：
  - `requestedTypeId = shulker_box`
  - `HostSlotRef`
  - `QuickOpenTrigger`

## 本阶段没有实现什么

- `ender_chest` 的手持右键打开
- `rightClickClose`
- reopen inventory S2C
- Bundle、工作台、切石机、铁砧入口
- 鼠标拖拽批量行为
- 其它 quick-openable 的手持右键扩展

## 双平台事件接入

### Forge 1.20.1

- 客户端入口事件：`PlayerInteractEvent.RightClickItem`
- 处理侧别：只在客户端侧发送 `OpenHostItemIntent`
- 服务端不直接在右键事件里打开，仍然只通过现有 C2S payload 进入 `ForgeQuickOpenHandler`

### NeoForge 1.21.1

- 客户端入口事件：`PlayerInteractEvent.RightClickItem`
- 处理侧别：只在客户端侧发送 `OpenHostItemIntent`
- 服务端不直接在右键事件里打开，仍然只通过现有 C2S payload 进入 `NeoForgeQuickOpenHandler`

## 如何避免抢原版交互

本阶段沿用 references 中原作的判断思路：把“手持右键打开”挂在 `RightClickItem` 级别，而不是粗暴拦截所有右键。

这样做的含义是：

- 当准星命中可交互方块时，应先走原版方块交互路径，本入口不会抢走
- 当准星命中可交互实体时，应先走原版实体交互路径，本入口不会抢走
- 当潜影盒本应被原版放置到方块上时，应先走原版 `UseOn` / 放置路径，本入口不会抢走
- 只有当原版已经判定“这次右键进入 item-use 分支”时，QuickShulker 才会在客户端发送打开请求

这与 references 中：

- `references/quickshulker-1.20/.../QuickShulkerMod.java`
- `references/quickshulker-1.21.1/.../QuickShulkerMod.java`
- `references/quickshulker-26.1-neo/.../QuickShulker.java`

里对“手持右键打开”的挂载位置保持同类语义。

## Trigger 与 payload

本阶段没有新增 trigger。

直接复用现有：

- `QuickOpenTrigger.HAND_RIGHT_CLICK`

因此：

- Forge packet 编解码无需额外改协议结构
- NeoForge payload 编解码无需额外改协议结构
- 现有按字符串写入 / 读取 trigger 的兼容方式保持不变

## 配置如何控制

客户端发送前会检查：

- `quickShulkerBox = true`
- `rightClickToOpen = true`

服务端在 `ForgeQuickOpenHandler` / `NeoForgeQuickOpenHandler` 中会再次按 trigger 重读配置并拒绝不允许的请求：

- `HAND_RIGHT_CLICK` 需要 `rightClickToOpen = true`
- 仍然要求 `quickShulkerBox = true`

这样可以保证：

- 配置关闭后客户端不会继续发送
- 即使客户端仍发送，服务端也会拒绝

## 服务端权威校验

服务端继续沿用已有权威校验链路，不信任客户端 `ItemStack`：

- 重新根据 `HostSlotRef` 解析当前宿主槽位
- 重新读取当前宿主物品
- 要求当前物品仍然是 `shulker_box`
- 要求数量仍为 `1`
- 要求 `requestedTypeId` 与服务端 registry 匹配
- 要求当前玩家没有冲突中的 active QuickShulker session

本阶段没有改动既有：

- 宿主锁定
- 重复打开保护
- F 键副手交换拦截
- Esc / E 关闭保存
- 取空保存为空

## 已知风险

- 本地未进行 Minecraft 实机验证，无法确认 Forge / NeoForge 两端对 `RightClickItem` 的所有边角行为都与 references 完全一致
- 当前“别抢原版交互”的结论建立在原作同类入口和 Forge / NeoForge `RightClickItem` 事件语义一致的前提上，仍建议维护者做放置、方块交互、实体交互专项实测
- 服务端配置当前仍依赖现有平台配置读取路径；若后续要支持更严格的专用服配置策略，需要在后续阶段统一梳理

## 阶段 6B 如何复用到 ender_chest

阶段 6B 可以直接复用本阶段入口骨架：

- 继续使用相同的 `RightClickItem` 客户端事件
- 继续复用 `trySendHeld(..., trigger)` 风格的发送路径
- 在 `resolveTypeId` / registry 过滤层放开 `ender_chest`
- 在服务端 handler 中为 `ender_chest` 增加对应 config gate 与打开逻辑

这样可以保证：

- “不抢原版交互”的判定方式不需要重做
- HostSlotRef、payload、session 权威校验链路不需要重写

## 人工测试清单

### Forge 1.20.1

- 无界面，主手持 1 个潜影盒，对空气或无可交互目标右键，打开盒子
- 无界面，副手持 1 个潜影盒，对空气或无可交互目标右键，打开盒子
- 准星对着可放置方块位置右键，应优先放置潜影盒，不打开 QuickShulker
- 准星对着箱子、门、按钮、工作台等可交互方块右键，应优先执行原版交互，不打开 QuickShulker
- 准星对着可交互实体右键，应优先执行原版交互，不打开 QuickShulker
- 配置关闭 `rightClickToOpen` 后，手持右键不打开
- 配置关闭 `quickShulkerBox` 后，手持右键不打开
- 打开后 `Esc` / `E` 关闭保存正常
- 打开期间宿主锁定、`F` 键副手交换拦截不回退

### NeoForge 1.21.1

- 无界面，主手持 1 个潜影盒，对空气或无可交互目标右键，打开盒子
- 无界面，副手持 1 个潜影盒，对空气或无可交互目标右键，打开盒子
- 准星对着可放置方块位置右键，应优先放置潜影盒，不打开 QuickShulker
- 准星对着箱子、门、按钮、工作台等可交互方块右键，应优先执行原版交互，不打开 QuickShulker
- 准星对着可交互实体右键，应优先执行原版交互，不打开 QuickShulker
- 配置关闭 `rightClickToOpen` 后，手持右键不打开
- 配置关闭 `quickShulkerBox` 后，手持右键不打开
- 打开后 `Esc` / `E` 关闭保存正常
- 打开期间宿主锁定、`F` 键副手交换拦截不回退
