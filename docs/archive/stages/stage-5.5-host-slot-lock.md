# 阶段 5.5：打开中的宿主槽位锁定

> 历史开发记录：本文档记录早期阶段实现过程，不再作为当前实现状态的唯一依据。当前状态请以源码、README 和 `docs/release-0.1.0.md` 为准。


本补丁位于阶段 4 Forge `shulker_box` 最小闭环和阶段 5 NeoForge `shulker_box` 最小闭环之间，目标是先修复一个共同存在的数据安全问题，再进入阶段 6A。

## 问题原因

当前阶段 4 / 5 已经具备以下保守策略：

- 打开时，服务端按 `HostSlotRef` 重新定位宿主物品。
- 打开期间，服务端每 tick 重新校验宿主是否仍然有效。
- 正常关闭且宿主有效时，写回实时容器内容。
- 宿主失效时，关闭菜单并丢弃本次修改，避免写回错误目标。

但阶段 4 / 5 还缺少一个关键保护：

- 当玩家在打开中的潜影盒菜单里直接操作“当前打开的那个宿主潜影盒”时，宿主本身仍然可被玩家拿起或移走。
- 一旦宿主被当前菜单主动拿走，服务端 tick 校验会把本次会话判定为宿主失效并关闭菜单。
- 这会触发“宿主失效 = 丢弃修改”的保守分支，导致本次已经放入或取出的内容不会保存。

因此，这不是“关闭保存逻辑”的回归，而是“菜单没有锁住宿主槽位”导致的前置数据安全漏洞。

阶段 5.5 继续补上了另一个同类问题：

- 当玩家通过右键打开 QuickShulker 潜影盒菜单后，再次右键当前打开的宿主盒子，客户端仍会在 `ScreenEvent.MouseButtonPressed.Pre` 中继续走 `trySendHovered(...)`。
- 这会再次向服务端发送新的 `OpenHostItemIntent`。
- 服务端 `ForgeShulkerSessionManager.open(...)` / `NeoForgeShulkerSessionManager.open(...)` 在补丁前没有 active session 防重入保护。
- 结果就是同一个宿主会被重复打开成多个 `ItemBackedShulkerContainer` 副本，页面内容彼此不互通，最终关闭时只会以最后关闭页面的内容写回宿主。

这确认属于重复 open / active session 重入问题。

## 修复策略

修复方向对齐 MoRanpcy/quickshulker 的原作行为：

- 打开中的宿主潜影盒在当前菜单里视为锁定槽位。
- 当前菜单不允许玩家主动拿起、移动、交换、丢弃、双击收集或拖拽命中该宿主。
- 当前 QuickShulker 菜单内不允许再次发送新的 open 请求。
- 菜单不再因为玩家点击宿主而关闭。
- 宿主若被外部原因移走、替换或数量变化，仍维持当前保守策略：
  - 关闭菜单。
  - 丢弃本次修改。
  - 不写回错误目标。

这次修复只解决“当前菜单主动操作宿主”的问题，不回退阶段 4 / 5 现有的宿主失效保护。

## Forge 1.20.1 实现

修改位置：

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerMenu.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerSessionManager.java`

实现方式：

1. 打开菜单时，把本次会话的 `HostSlotRef` 传入 `ForgeShulkerMenu`。
2. 菜单构造后扫描当前 `ShulkerBoxMenu` 的 `slots`，把 `HostSlotRef` 映射成当前菜单中的宿主 `menu slot index`。
3. 仅当宿主槽位在当前菜单里可见时，记录这个 `lockedMenuSlotIndex`。
4. 对主手热栏宿主，额外按 `HostSlotRef.logicalSlotIndex()` 锁定对应数字键交换目标。
5. 对副手宿主，由于当前 `ShulkerBoxMenu` 不显示 offhand 槽位，因此当前阶段只保证：
   - 当前菜单内没有可直接点击的副手宿主槽位。
   - 服务端宿主失效 tick 校验继续保留。
   - 不把副手宿主误映射为某个可见玩家背包槽位。

Forge 当前拦截点：

- `clicked(int slotId, int button, ClickType clickType, Player player)`
- `canTakeItemForPickAll(ItemStack stack, Slot slot)`
- `canDragTo(Slot slot)`
- `quickMoveStack(Player player, int index)`
- `ForgeQuickShulkerClient.trySendHovered(...)`
- `ForgeShulkerSessionManager.open(...)`

Forge 当前直接拒绝的操作类型：

- `PICKUP`
- `QUICK_MOVE`
- `SWAP`
- `THROW`
- `CLONE`
- `PICKUP_ALL`
- `QUICK_CRAFT`

说明：

- 当 `slotId` 命中锁定宿主槽位时，以上点击统一在 `clicked(...)` 入口直接拒绝。
- 当玩家尝试用数字键把其他槽位和“锁定宿主热栏位”交换时，`SWAP` 也会被直接拒绝。
- `PICKUP_ALL` 的双击收集路径额外通过 `canTakeItemForPickAll(...)` 阻止把宿主纳入收集目标。
- `QUICK_CRAFT` 的拖拽路径额外通过 `canDragTo(...)` 阻止拖拽分发命中宿主槽位。
- 当当前菜单已经是 `ForgeShulkerMenu` 时，客户端不再发送新的 `OpenHostItemIntent`。
- 当玩家已经存在 active session 时，服务端 `open(...)` 直接拒绝新请求，不创建新的 `ItemBackedShulkerContainer`，不调用 `player.openMenu(...)`，也不覆盖既有 session。

## NeoForge 1.21.1 实现

修改位置：

- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerMenu.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerSessionManager.java`

实现方式与 Forge 保持一致：

1. 打开菜单时，把 `HostSlotRef` 传入 `NeoForgeShulkerMenu`。
2. 菜单内根据玩家库存槽位和 `HostSlotRef` 解析可见宿主槽位。
3. 若宿主是主手热栏潜影盒，则锁定对应 `menu slot index` 和对应的数字键交换目标。
4. 若宿主是副手潜影盒，则维持“当前菜单不可直接操作 + 服务端继续 tick 校验”的保守边界。
5. NeoForge 1.21.1 额外明确区分：
   - 背包界面副手 screen slot 是 `InventoryMenu.SHIELD_SLOT = 45`
   - 玩家背包真实 offhand slot 是 `Inventory.SLOT_OFFHAND = 40`
   - QuickShulker 打开请求统一折叠为 `HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, 45)`
6. 对已经打开的副手宿主，`ShulkerBoxMenu` 虽然没有可见 offhand 槽位，但仍要阻止当前菜单内能够命中的 offhand swap 目标，避免通过 `ClickType.SWAP` 改变宿主。

本轮继续修正了一个更具体的 NeoForge 1.21.1 背包副手问题：

- 之前的副手识别条件过窄，过度依赖 `menu instanceof InventoryMenu + slot.index == InventoryMenu.SHIELD_SLOT + slot.getContainerSlot() == Inventory.SLOT_OFFHAND` 同时成立。
- 实机 NeoForge 1.21.1 下，这几项条件未必会同时成立，因此 `NeoForgeHostSlotResolver.forPlayerInventorySlot(...)` 可能直接返回 `Optional.empty()`。
- 一旦返回 `empty`，`NeoForgeQuickShulkerClient.trySendHovered(...)` 就不会发送 `OpenHostItemIntent`，也不会取消当前右键事件。
- 这正是“副手快捷键打不开、右键打不开且落回原版拿起物品”的直接原因。

新的副手识别策略改为：

1. 优先按 `Slot` 本身识别玩家副手：
   - `slot.container == player.getInventory()`
   - `slot.getContainerSlot() == Inventory.SLOT_OFFHAND`
   - 满足时直接返回 `HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, slot.index)`
2. 再按同样的 `slot.container == player.getInventory()` 识别 hotbar 和主背包。
3. `InventoryMenu.SHIELD_SLOT` 只作为 NeoForge 1.21.1 背包界面的兼容 fallback，不再作为唯一识别条件。

之所以不能只依赖 `InventoryMenu.SHIELD_SLOT`，是因为：

- QuickShulker 需要识别的是“这个 `Slot` 是否真实指向玩家副手宿主”，而不只是“这个 screen slot 看起来像副手位置”。
- NeoForge 1.21.1 下 screen slot、menu slot、底层 player inventory slot 可能不会以最理想化的方式同时对齐。
- 先按 `slot.container + containerSlot` 识别，能更接近 Forge 1.20.1 当前已经工作的语义。

为便于继续排查，这次还新增了临时 `debug` 日志。
如果副手实机仍失败，需要重点查看：

- `screenClass`
- `menuClass`
- `slotIndex`
- `containerSlot`
- `containerClass`
- `usesPlayerInventory`
- `inventoryMenu`
- `hoveredItemKey`
- `trigger`

本次实际失败日志已经确认了一种 NeoForge 1.21.1 特例：

- `screenClass=net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen`
- `menuClass=net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen$ItemPickerMenu`
- `slotIndex=0`
- `containerSlot=45`
- `containerClass=net.minecraft.world.entity.player.Inventory`
- `usesPlayerInventory=true`

这说明玩家测试时命中的不是普通 `InventoryScreen` 槽位，而是创造模式背包界面的 `SlotWrapper`。
在这个包装层里：

- wrapper 自己的 `slot.index` 可能是 `0`
- wrapper 自己的 `getContainerSlot()` 可能是 `45`
- 但其内部 target slot 才对应真实玩家背包语义

因此 NeoForge 1.21.1 不能只看当前悬停 `Slot` 外层暴露出来的 index / container slot；
对创造模式背包界面，还需要先解包 `CreativeModeInventoryScreen.SlotWrapper` 的 target slot，再映射为：

- 副手：`HostSlotRef(HostStorageScope.PLAYER_OFFHAND, 0, 45)`
- 其余玩家背包槽位：继续按 target slot 的 player inventory 语义识别

NeoForge 当前拦截点：

- `clicked(int slotId, int button, ClickType clickType, Player player)`
- `canTakeItemForPickAll(ItemStack stack, Slot slot)`
- `canDragTo(Slot slot)`
- `quickMoveStack(Player player, int index)`
- `NeoForgeQuickShulkerClient.trySendHovered(...)`
- `NeoForgeShulkerSessionManager.open(...)`

NeoForge 当前直接拒绝的操作类型：

- `PICKUP`
- `QUICK_MOVE`
- `SWAP`
- `THROW`
- `CLONE`
- `PICKUP_ALL`
- `QUICK_CRAFT`

NeoForge 当前也额外加入了重复打开保护：

- 当当前菜单已经是 `NeoForgeShulkerMenu` 时，客户端不再发送新的 `OpenHostItemIntent`。
- 当玩家已经存在 active session 时，服务端 `open(...)` 直接拒绝新请求，不创建新的 `ItemBackedShulkerContainer`，不调用 `player.openMenu(...)`，也不覆盖既有 session。
- 当背包界面右键副手潜影盒时，一旦客户端决定发送打开请求，就必须立即取消当前 `ScreenEvent.MouseButtonPressed.Pre`，避免原版继续把副手物品拿起。

## 与阶段 4 / 5 关闭保存逻辑的关系

本补丁没有回退阶段 4 / 5 已修复的关闭保存策略，仍保持：

- 正常关闭 + 宿主有效 = 写回实时容器内容。
- 宿主失效 = 放弃写回。

本补丁只是在“正常交互阶段”前移了一层保护，尽量不让玩家通过当前菜单把宿主主动操作成失效状态。

## 当前边界，留到阶段 7

以下边界没有在本补丁内扩展：

- inventory 任意槽位打开后的更广义宿主锁定复用
- 更完整的创造模式特殊交互覆盖确认
- 更完整的拖拽批量行为和其它特殊菜单路径实机验证
- 副手宿主在未来若进入可见槽位菜单时的显式锁定映射
- 多人环境下更完整的宿主锁定与防复制专项验证

阶段 7 继续关注的方向仍是：

- 更完整的宿主锁定
- 更全面的防复制验证
- 多人 / 专用服务端边界验证

## 人工测试清单

### Forge 1.20.1

1. 主手潜影盒按 `K` 打开。
2. 在盒子里放入物品后，左键点击宿主潜影盒，应无反应，菜单不关闭。
3. 在盒子里取出物品后，左键点击宿主潜影盒，应无反应，菜单不关闭。
4. `Shift` 点击宿主潜影盒，应无反应。
5. 数字键交换宿主潜影盒，应无反应。
6. `Q` 丢弃宿主潜影盒，应无反应。
7. 正常 `Esc` 关闭后内容保存。
8. 正常 `E` 关闭后内容保存。
9. 副手打开场景至少确认无复制、无崩溃、保存正常。
10. 右键打开潜影盒后，再次右键当前打开的宿主盒子，应无反应，不打开第二个页面。
11. 多次右键宿主盒子，不应出现两个不同内容的页面来回切换。
### NeoForge 1.21.1

1. 主手潜影盒按 `K` 打开。
2. 在盒子里放入物品后，左键点击宿主潜影盒，应无反应，菜单不关闭。
3. 在盒子里取出物品后，左键点击宿主潜影盒，应无反应，菜单不关闭。
4. `Shift` 点击宿主潜影盒，应无反应。
5. 数字键交换宿主潜影盒，应无反应。
6. `Q` 丢弃宿主潜影盒，应无反应。
7. 正常 `Esc` 关闭后内容保存。
8. 正常 `E` 关闭后内容保存。
9. 副手打开场景至少确认无复制、无崩溃、保存正常。
10. 右键打开潜影盒后，再次右键当前打开的宿主盒子，应无反应，不打开第二个页面。
11. 多次右键宿主盒子，不应出现两个不同内容的页面来回切换。
12. 打开背包，副手放 1 个潜影盒，悬停副手槽位按快捷键打开。
13. 打开背包，副手放 1 个潜影盒，悬停副手槽位右键打开，且原版不会把盒子拿起。
14. 副手宿主打开后，尝试数字键交换 / 其他当前菜单内可命中的宿主修改路径，应无效。

## 待人工确认项

- 当前 `ShulkerBoxMenu` 下除已拦截的 `ClickType.SWAP + button 40` 之外，副手宿主是否还存在额外特殊输入路径，仍需实机确认。
- 尚未完成 Minecraft 游戏内人工测试，无法确认行为已与原作在所有点击边界上完全一致。
- 尚未完成多人 / 专用服务端人工测试。
- 若后续继续大段参考或改写 `references/` 中第三方实现，仍需人工检查许可证和来源说明是否需要更新。
