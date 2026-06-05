# 阶段 6A：`shulker_box` 基础输入入口与配置落地

本阶段在阶段 4 / 5 的最小 `shulker_box` 开闭环基础上，补齐了双平台的基础输入入口、平台配置读取与 HostSlotRef 映射收口。

本阶段仍然保持保守范围：

- 只处理 `shulker_box`
- 所有打开请求仍走既有的 `OpenHostItemIntent -> 服务端重校验 -> 会话打开`
- 不改动 `references/`
- 不引入末影箱同步、Bundle 独立菜单、工作台 / 切石机 / 铁砧、拖拽批量行为
- 不回退阶段 4 / 5 / 5.5 已验证的关闭保存、宿主锁定和重复打开保护

## 本阶段实现了什么

### 双平台基础输入入口

Forge 1.20.1 与 NeoForge 1.21.1 当前都支持以下 `shulker_box` 入口：

- 无界面时，主手潜影盒按快捷键打开
- 无界面时，副手潜影盒按快捷键打开
- 背包 / 容器界面中，鼠标悬停玩家热栏潜影盒，按快捷键打开
- 背包 / 容器界面中，鼠标悬停玩家主背包潜影盒，按快捷键打开
- 背包界面中，鼠标悬停副手潜影盒，按快捷键打开
- 背包 / 容器界面中，鼠标悬停玩家热栏潜影盒，右键打开
- 背包 / 容器界面中，鼠标悬停玩家主背包潜影盒，右键打开
- 背包界面中，鼠标悬停副手潜影盒，右键打开

### 右键打开会取消原版点击

双平台都在界面层使用 `ScreenEvent.MouseButtonPressed.Pre` 处理右键入口。

只要 QuickShulker 已决定发送 `OpenHostItemIntent`，就会立即取消当前右键点击，避免：

- 发出打开请求后原版同时拿起物品
- 右键副手潜影盒时回落到原版拿起逻辑
- 对当前宿主再次右键时创建第二个盒子页面

### 配置项真正参与运行时判断

本阶段确认并补齐了以下配置项在双平台的落地：

- `quickShulkerBox`
- `keybindInHand`
- `keybindInInventory`
- `rightClickInInventory`
- `rightClickToOpen`
- `rightClickClose`
- `supportsMouseDragged`

其中：

- `quickShulkerBox` 现在同时作用于客户端入口和服务端处理；关闭后不会再发送 / 拦截 `shulker_box` 打开请求
- `keybindInHand` 控制无界面时主手 / 副手快捷键入口
- `keybindInInventory` 控制界面中悬停槽位的快捷键入口
- `rightClickInInventory` 和 `rightClickToOpen` 共同控制界面中悬停槽位的右键入口
- `rightClickClose` 本阶段只保留配置项，不实现“再次右键关闭”
- `supportsMouseDragged` 本阶段只保留配置项，不实现拖拽批量行为

### 当前菜单内禁止重复 open

阶段 5.5 的行为保持不变：

- 当前菜单已经是 `ForgeShulkerMenu` / `NeoForgeShulkerMenu` 时，客户端不会再次发送新的 `OpenHostItemIntent`
- 服务端已有 active session 时，`open(...)` 直接拒绝新请求
- 不会创建第二个 `ItemBackedShulkerContainer`
- 不会覆盖既有 session

### 宿主锁定与关闭保存保持不回退

本阶段没有重写既有的宿主锁定和关闭保存逻辑，仍保持：

- 打开中的宿主不能被左键拿起
- 不能被右键拿起
- 不能 shift-click
- 不能数字键交换
- 不能 `Q` 丢弃
- 不能被拖拽影响
- 不能被 `PICKUP_ALL` 双击收集
- 宿主外部失效时关闭并丢弃修改，不写回错误目标

关闭保存仍保持：

- 正常关闭 + 宿主有效 = 写回实时容器内容
- 宿主失效 = 放弃写回
- 不把 `dirty` 重新作为保存硬条件
- `Esc` / `E` / 正常关闭都会走既有服务端关闭保存入口
- 取空盒子后关闭也应保存为空

## 本阶段没有实现什么

本阶段明确没有实现：

- 末影箱同步
- Bundle 独立菜单
- 工作台 / 切石机 / 铁砧
- 鼠标拖拽批量插入 / 提取
- `shulker -> shulker` 批量转移逻辑
- `rightClickClose` 的再次右键关闭行为
- `supportsMouseDragged` 对应的拖拽批量行为
- 容器自身非玩家背包槽位中的潜影盒打开

## Forge 1.20.1 输入入口

Forge 1.20.1 当前入口如下：

- 无界面手持快捷键：`TickEvent.ClientTickEvent`
- 界面悬停快捷键：`ScreenEvent.KeyPressed.Pre`
- 界面悬停右键：`ScreenEvent.MouseButtonPressed.Pre`

触发条件：

- `quickShulkerBox = true`
- 入口对应的配置项已开启
- 当前不是 `ForgeShulkerMenu`
- 当前没有鼠标携带物品
- 宿主槽位可以稳定映射到玩家背包 `HostSlotRef`
- 宿主物品是数量为 `1` 的潜影盒

## NeoForge 1.21.1 输入入口

NeoForge 1.21.1 当前入口如下：

- 无界面手持快捷键：`ClientTickEvent.Post`
- 界面悬停快捷键：`ScreenEvent.KeyPressed.Pre`
- 界面悬停右键：`ScreenEvent.MouseButtonPressed.Pre`

触发条件与 Forge 保持一致：

- `quickShulkerBox = true`
- 入口对应的配置项已开启
- 当前不是 `NeoForgeShulkerMenu`
- 当前没有鼠标携带物品
- 宿主槽位可以稳定映射到玩家背包 `HostSlotRef`
- 宿主物品是数量为 `1` 的潜影盒

NeoForge 1.21.1 额外保留了对 `CreativeModeInventoryScreen.SlotWrapper` 的 unwrap 处理，用于创造模式特例；该路径本阶段只保留结构支持，完整行为留到阶段 7 继续专项验证。

## HostSlotRef 映射策略

本阶段明确区分三种概念：

- screen slot：当前 screen 下悬停到的 UI 槽位
- menu slot index：当前 `AbstractContainerMenu` 中的槽位 index
- player inventory slot index：玩家背包真实容器槽位

QuickShulker 不直接把 screen slot index 当作玩家背包索引使用。

### 统一逻辑语义

双平台都采用以下策略：

1. 先判断当前悬停槽位是否属于“允许打开的玩家背包槽位”
2. 读取该槽位在当前菜单中的 `menu slot index`
3. 读取该槽位在玩家背包容器中的真实槽位
4. 再映射为 common `HostSlotRef`

### 映射结果

- `player inventory slot 0..8` -> `HostStorageScope.PLAYER_HOTBAR`
- `player inventory slot 9..35` -> `HostStorageScope.PLAYER_MAIN_INVENTORY`
- `player inventory slot 40` -> `HostStorageScope.PLAYER_OFFHAND`

`logicalSlotIndex` 定义如下：

- `PLAYER_HOTBAR`：`0..8`
- `PLAYER_MAIN_INVENTORY`：`0..26`
- `PLAYER_OFFHAND`：固定 `0`

`menuSlotIndex` 只保留当前菜单上下文，服务端真正重新取宿主时仍以 `scope + logicalSlotIndex` 为准。

### Forge 1.20.1 映射策略

Forge 1.20.1 当前使用：

- `slot.container == player.getInventory()` 识别玩家背包宿主
- `slot.getSlotIndex()` 读取玩家背包真实槽位
- `hoveredSlot.index` 保留为 `menuSlotIndex`

因此：

- 热栏宿主会映射到 `PLAYER_HOTBAR`
- 主背包 27 格宿主会映射到 `PLAYER_MAIN_INVENTORY`
- 副手宿主会在 `container slot 40` 时映射到 `PLAYER_OFFHAND`

### NeoForge 1.21.1 映射策略

NeoForge 1.21.1 当前使用：

- `unwrapSlot(...)` 先处理创造模式 `SlotWrapper`
- `effectiveSlot.container == player.getInventory()` 识别玩家背包宿主
- `effectiveSlot.getContainerSlot()` 读取玩家背包真实槽位
- `effectiveSlot.index` 保留为 `menuSlotIndex`

副手是当前阶段的特殊点：

- 背包界面菜单槽位：`InventoryMenu.SHIELD_SLOT = 45`
- 玩家背包真实槽位：`Inventory.SLOT_OFFHAND = 40`
- 最终映射：`HostSlotRef(PLAYER_OFFHAND, 0, 45)`

若当前为 `InventoryMenu` 且识别到 `menu slot 45`，NeoForge 仍保留副手兜底映射逻辑。

## 支持打开的槽位

本阶段支持：

- 玩家热栏中的潜影盒
- 玩家主背包 27 格中的潜影盒
- 玩家副手槽中的潜影盒
- 上述槽位在玩家背包界面中打开
- 上述槽位在其他容器界面中打开，但前提是底层槽位仍然属于玩家自己的背包容器

## 暂不支持打开的槽位

本阶段暂不支持：

- 容器自身槽位中的潜影盒
- 合成输入格
- 合成结果格
- 盔甲槽
- 配方书 / 创造搜索 / 创造物品栏等特殊 UI 槽位
- 无法稳定映射到底层玩家背包的特殊包装槽位
- 创造模式下除现有 NeoForge `SlotWrapper` 兜底外的其它特例路径

原因是这些槽位当前要么不是玩家宿主，要么映射和关闭语义风险仍然偏高，本阶段先不扩大支持面。

## 宿主锁定与重复打开保护如何保持

双平台都保持以下结构：

- 客户端在当前 QuickShulker 菜单内不再发起新的打开请求
- 服务端 session manager 发现已有 active session 时直接拒绝新请求
- `ForgeShulkerMenu` / `NeoForgeShulkerMenu` 继续锁定宿主菜单槽位
- `clicked(...)`、`quickMoveStack(...)`、`canDragTo(...)`、`canTakeItemForPickAll(...)` 继续阻止对宿主的危险操作
- 每 tick 继续重新校验宿主是否仍然有效；失效时关闭并丢弃改动

## 已知风险

- 本地未进行 Minecraft 实机验证，无法确认所有 UI 边角行为都与参考实现完全一致
- Forge 1.20.1 的输入槽位映射目前没有创造模式专项处理，本阶段只保证常规背包 / 容器界面
- NeoForge 1.21.1 虽保留了 `CreativeModeInventoryScreen.SlotWrapper` 处理，但创造模式整体仍应放到阶段 7 做专项验证
- `rightClickClose` 与 reopen inventory 的完整交互尚未实现

## 人工测试清单

### Forge 1.20.1

- QuickShulker 菜单打开期间，按 `F` 不会把当前 hover 物品切到副手
- QuickShulker 菜单打开期间，按 `F` 不会把副手宿主切回其他格，也不会复制出第二个盒子
- 生存模式下，QuickShulker 菜单打开期间触发副手交换后不会在副手留下需要再点一下才消失的幽灵盒子

- 单人存档启动
- 本地 Forge 服务端启动
- 客户端 + 服务端双端安装
- 无界面主手潜影盒按快捷键打开
- 无界面副手潜影盒按快捷键打开
- 背包界面悬停热栏潜影盒按快捷键打开
- 背包界面悬停主背包潜影盒按快捷键打开
- 背包界面悬停副手潜影盒按快捷键打开
- 背包 / 容器界面悬停热栏潜影盒右键打开
- 背包 / 容器界面悬停主背包潜影盒右键打开
- 背包界面悬停副手潜影盒右键打开
- 右键打开时原版不会同时拿起宿主
- 当前 QuickShulker 菜单中再次右键宿主不会打开第二页
- 宿主在菜单打开期间不能左键、右键、shift-click、数字键交换、Q 丢弃、拖拽影响、双击收集
- 关闭后内容正确写回
- 打开后宿主被移走 / 替换 / 数量变为非 1 时不写回错误目标
- 玩家死亡 / 掉线 / 切维度后会话按既有安全逻辑收尾

### NeoForge 1.21.1

- QuickShulker 菜单打开期间，按 `F` 不会把当前 hover 物品切到副手
- QuickShulker 菜单打开期间，按 `F` 不会把副手宿主切回其他格，也不会制造副手复制 / 异常宿主状态
- 生存模式下，QuickShulker 菜单打开期间触发副手交换后不会在副手留下需要再点一下才消失的幽灵盒子

- 单人存档启动
- 本地 NeoForge 服务端启动
- 客户端 + 服务端双端安装
- 无界面主手潜影盒按快捷键打开
- 无界面副手潜影盒按快捷键打开
- 背包界面悬停热栏潜影盒按快捷键打开
- 背包界面悬停主背包潜影盒按快捷键打开
- 背包界面悬停副手潜影盒按快捷键打开
- 背包 / 容器界面悬停热栏潜影盒右键打开
- 背包 / 容器界面悬停主背包潜影盒右键打开
- 背包界面悬停副手潜影盒右键打开
- 右键副手潜影盒时原版不会继续把副手宿主拿起
- 当前 QuickShulker 菜单中再次右键宿主不会打开第二页
- 宿主在菜单打开期间不能左键、右键、shift-click、数字键交换、Q 丢弃、拖拽影响、双击收集
- 关闭后内容正确写回
- 打开后宿主被移走 / 替换 / 数量变为非 1 时不写回错误目标
- 玩家死亡 / 掉线 / 切维度后会话按既有安全逻辑收尾
- 创造模式副手与 `SlotWrapper` 路径留待阶段 7 继续专项验证

## 后续阶段 6B / 7 需要继续做什么

阶段 6B 建议继续处理：

- `rightClickClose` 的完整再次右键关闭语义
- reopen inventory 的 S2C / 客户端返回背包链路
- 更多 screen / menu 的安全槽位映射
- 更细的输入冲突与边界行为校验

阶段 7 建议继续处理：

- 创造模式专项验证
- 多人 / 专用服务端实测
- 更多 quick-openable 类型
- 更完整的 UI 边界、异常关闭和跨版本映射验证

## 参考来源说明

本阶段主要参考：

- `references/quickshulker-1.20`
- `references/quickshulker-1.21.1`
- `references/quickshulker-26.1-neo`

本次未直接修改 `references/` 目录。

## 阶段 6A 补丁：菜单打开期间禁用副手交换

阶段 6A 完成后又确认了一条新的数据安全边界：

- 创造模式下，QuickShulker 菜单打开期间如果允许 `ClickType.SWAP` 命中原版副手交换路径，玩家可以在界面未关闭时把当前宿主盒子切到副手或从副手切走，进而出现复制或异常宿主状态。
- 生存模式下，同一条路径虽然通常不会直接复制真实物品，但会因为客户端预测、服务端拒绝、宿主锁定状态不同步而在副手留下幽灵盒子；再次点击副手格后幽灵物品才会被原版同步清掉。

根因确认：

- 阶段 5.5 已经锁住了宿主槽位本身，以及数字键交换、`Q`、`PICKUP_ALL`、`QUICK_CRAFT` 等常规危险路径。
- 但 QuickShulker 菜单打开期间，`offhand swap` / `F` 键对应的 `ClickType.SWAP + offhand button` 还没有被整体禁掉。
- 这会让“当前 hover 物品切到副手”和“副手宿主切回其他槽位”仍然进入原版交换流程，与 QuickShulker 的宿主锁定和服务端会话校验相冲突。

因此本阶段改为更保守的统一策略：

- 只要当前菜单是 `ForgeShulkerMenu` 或 `NeoForgeShulkerMenu`，整个菜单周期内一律禁止 `offhand swap`。
- 不再区分当前宿主是否本来就在副手，也不再只拦截 `hostSlotRef.scope() == PLAYER_OFFHAND` 的情况。
- 这样可以同时阻止用 `F` 把当前 hover 的其他物品切到副手、把副手宿主切到其他物品格，以及因原版预测留下的副手幽灵物品。

Forge 1.20.1 拦截点：

- 服务端菜单侧：`ForgeShulkerMenu.clicked(...)` 中的 `shouldBlockHostSlotClick(...)` 现在会直接拒绝 `ClickType.SWAP + button 40`。
- 客户端侧：`ForgeQuickShulkerClient.onScreenKeyPressed(...)` 在当前 `Screen` 是 `AbstractContainerScreen` 且 `menu instanceof ForgeShulkerMenu` 时，使用 `minecraft.options.keySwapOffhand.matches(...)` 预先取消原版副手交换键事件。
- 服务端在拒绝该点击后除了 `broadcastChanges()` 之外，还会对 `player.inventoryMenu` 调用 `sendAllDataToRemote()`，用于把不在当前 `ShulkerBoxMenu` 可见范围内的副手槽一并强制同步回客户端，避免生存模式副手 ghost item。

NeoForge 1.21.1 拦截点：

- 服务端菜单侧：`NeoForgeShulkerMenu.clicked(...)` 中的 `shouldBlockHostSlotClick(...)` 现在会直接拒绝 `ClickType.SWAP + button Inventory.SLOT_OFFHAND`。
- 客户端侧：`NeoForgeQuickShulkerClient.onScreenKeyPressed(...)` 在当前 `Screen` 是 `AbstractContainerScreen` 且 `menu instanceof NeoForgeShulkerMenu` 时，使用 `minecraft.options.keySwapOffhand.matches(...)` 取消原版副手交换键事件。
- 服务端同样会在拒绝点击后同步当前菜单并对 `player.inventoryMenu` 调用 `sendAllDataToRemote()`，作为副手槽位的强制回滚补偿。

与现有阶段目标的关系：

- 这次补丁不回退现有的宿主锁定、重复打开保护、关闭保存逻辑。
- `OpenHostItemIntent` 的防重入规则保持不变；当前 QuickShulker 菜单中仍不会发送新的打开请求。
- 这次修复只覆盖 QuickShulker 菜单打开期间的副手交换，不扩展到末影箱同步、Bundle、工作台 / 切石机 / 铁砧，或鼠标拖拽批量行为。
- 创造模式下 `CreativeModeInventoryScreen`、`SlotWrapper`、`CLONE` 等特殊路径虽然已被这次双端拦截显著收紧，但仍建议放到阶段 7 做专项实机验证。

## 阶段 6A.5 追加记录：无界面手持右键入口

阶段 6A.5 在本阶段文档基础上，又为 `shulker_box` 收口了“无界面手持右键打开”入口，关键点如下：

- 继续复用现有 `OpenHostItemIntent -> 服务端重校验 -> active session 打开` 链路。
- 客户端入口仍受以下配置共同控制：
  - `quickShulkerBox`
  - `rightClickToOpen`
- 具体触发器为 `HAND_RIGHT_CLICK`。
- 只在原版已经把这次右键判定为 item-use 分支时才发送请求，因此不会抢占：
  - 对可交互方块的原版右键
  - 对可交互实体的原版右键
  - 对可放置位置的原版潜影盒放置
- 服务端继续按 `HostSlotRef` 重新定位宿主，并要求：
  - 当前物品仍是 `shulker_box`
  - 数量仍为 1
  - 当前玩家没有 active QuickShulker session
- 当前 QuickShulker 菜单中仍不会再次发送新的 open 请求。

阶段 6B 的 `ender_chest` 最小闭环继续直接复用这条 6A.5 无界面手持右键边界，不额外复制独立入口。
