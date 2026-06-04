# 阶段 5.5：打开中的宿主槽位锁定

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

## 修复策略

修复方向对齐 MoRanpcy/quickshulker 的原作行为：

- 打开中的宿主潜影盒在当前菜单里视为锁定槽位。
- 当前菜单不允许玩家主动拿起、移动、交换、丢弃、双击收集或拖拽命中该宿主。
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

## NeoForge 1.21.1 实现

修改位置：

- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerMenu.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerSessionManager.java`

实现方式与 Forge 保持一致：

1. 打开菜单时，把 `HostSlotRef` 传入 `NeoForgeShulkerMenu`。
2. 菜单内根据玩家库存槽位和 `HostSlotRef` 解析可见宿主槽位。
3. 若宿主是主手热栏潜影盒，则锁定对应 `menu slot index` 和对应的数字键交换目标。
4. 若宿主是副手潜影盒，则维持“当前菜单不可直接操作 + 服务端继续 tick 校验”的保守边界。

NeoForge 当前拦截点：

- `clicked(int slotId, int button, ClickType clickType, Player player)`
- `canTakeItemForPickAll(ItemStack stack, Slot slot)`
- `canDragTo(Slot slot)`
- `quickMoveStack(Player player, int index)`

NeoForge 当前直接拒绝的操作类型：

- `PICKUP`
- `QUICK_MOVE`
- `SWAP`
- `THROW`
- `CLONE`
- `PICKUP_ALL`
- `QUICK_CRAFT`

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

## 待人工确认项

- 当前 `ShulkerBoxMenu` 下副手宿主是否存在额外的 offhand swap 特殊输入路径，仍需实机确认。
- 尚未完成 Minecraft 游戏内人工测试，无法确认行为已与原作在所有点击边界上完全一致。
- 尚未完成多人 / 专用服务端人工测试。
- 若后续继续大段参考或改写 `references/` 中第三方实现，仍需人工检查许可证和来源说明是否需要更新。
