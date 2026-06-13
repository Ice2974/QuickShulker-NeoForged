# stage-1.0.0-p6-ender-chest-mouse-dragged

本阶段接入"拿起单个末影箱拖拽"的批量行为，与潜影盒拖拽体验保持一致，同时保留末影箱优先级和防嵌套规则。

## 范围

本阶段只支持"拿末影箱拖拽"，不支持"拿普通物品拖过多个末影箱"的批量 insert。

支持的两种拖拽模式：

- 拿起单个末影箱从非空槽开始拖拽：拖过非空槽时将目标槽物品收入玩家自己的 EnderChestInventory。
- 拿起单个末影箱从空槽开始拖拽：拖过空槽时从玩家自己的 EnderChestInventory 批量放出物品。

## 行为规则

### 批量收纳（pickup insert drag）

- 拖拽起始格子为非空槽时进入 PICKUP_INTO_CARRIED_ENDER_CHEST 模式。
- 拖过非空槽时将目标槽物品收入末影箱：
  - 普通物品可以收入。
  - 潜影盒也可以收入末影箱。
  - 末影箱不能收入末影箱（防嵌套），客户端 canInsertIntoEnderChest 拦截。
- 末影箱满时收入失败，服务端 esult.changed() == false，目标槽和末影箱内容均不变，不回退为潜影盒 bundling。
- 目标槽为当前 quick-open 宿主时跳过（isCurrentQuickOpenHost / HostIdentity.sameSlot）。

### 批量放出（extract drag）

- 拖拽起始格子为空槽时进入 EXTRACT_FROM_CARRIED_ENDER_CHEST 模式。
- 放出顺序与潜影盒一致：从后往前。
- 目标槽是潜影盒内容槽（quick-open 潜影盒菜单或原版潜影盒菜单）时：
  - 复用阶段 5.3 的 skip shulker 规则：skipShulkerBoxes = true。
  - 跳过末影箱内潜影盒，从后往前继续找普通物品。
  - 如果末影箱里全是潜影盒（或为空），本槽放出失败且末影箱内容不变。
- 目标槽不是潜影盒内容槽时（例如玩家背包普通空槽）：
  - 保持普通 extract 语义（skipShulkerBoxes = false）。
  - 可以从末影箱放出潜影盒到真实空槽（不会嵌套，因为目标不是潜影盒内容槽）。

## 实现方式

### common 网络层

ShulkerBundlingAction 新增两个枚举值：

- MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT
- MOUSE_DRAG_ENDER_CHEST_EXTRACT

这两个值追加在 ENDER_CHEST_EXTRACT 之后、UNKNOWN 之前。序列化使用 ction.name() / romSerializedName（基于 alueOf），自动兼容。

### 客户端（Forge + NeoForge）

- DragMode 新增 PICKUP_INTO_CARRIED_ENDER_CHEST 和 EXTRACT_FROM_CARRIED_ENDER_CHEST。
- determineEnderChestBundlingIntent 未修改：仍然在拿单个末影箱且悬停真实空槽时生成 ENDER_CHEST_EXTRACT，在拿单个末影箱且悬停非空可收入物品时生成 ENDER_CHEST_PICKUP_INSERT。
- prepareBundlingIntent：当 supportsMouseDragged() 开启且 action 为 ENDER_CHEST_PICKUP_INSERT 或 ENDER_CHEST_EXTRACT 时，分配 dragId，使末影箱右键成为拖拽起始。
- eginMouseDrag：根据起始 action 设置 PICKUP_INTO_CARRIED_ENDER_CHEST 或 EXTRACT_FROM_CARRIED_ENDER_CHEST。
- 	rySendMouseDraggedBundlingIntent：
  - pickup 模式：isSingleEnderChest(carried) && !hoveredStack.isEmpty() && canInsertIntoEnderChest(hoveredStack) → MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT。
  - extract 模式：isSingleEnderChest(carried) && hoveredStack.isEmpty() → MOUSE_DRAG_ENDER_CHEST_EXTRACT。
- isEnderChestBundlingAction 包含两个新 drag action，确保拖拽状态清理（clearMouseDrag / screen init / close）覆盖末影箱拖拽。
- 输入取消策略与潜影盒拖拽一致：右键按下时 suppressBundlingMousePressedUntilRelease、suppressNextInventoryRightRelease，左键同时按下时 clearMouseDrag。

### 服务端（Forge + NeoForge）

- isEnderChestDragAction：新增辅助方法，判断 action 是否为末影箱拖拽相关（含起始 action 和续拖 action）。
- handle：
  - 末影箱基础 action（ENDER_CHEST_INSERT）且非拖拽 → 原有单次路径。
  - 末影箱拖拽 action → 进入 drag session 流程（与潜影盒一致）。
- isDragSessionIntent：包含 ENDER_CHEST_PICKUP_INSERT、ENDER_CHEST_EXTRACT、MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT、MOUSE_DRAG_ENDER_CHEST_EXTRACT。
- esolveDragSession：
  - isValidDragCarried(carried, enderChestDrag)：末影箱拖拽要求 isSingleEnderChest，潜影盒拖拽要求 isSingleShulkerBox。
  - 续拖 action（MOUSE_DRAG_*）无活动 session 时拒绝。
- handleEnderChestBundling：新增 4 参数重载（含 dragSession），内部 switch 调用 session-aware 的 handleEnderChestPickupInsert(4-arg) / handleEnderChestExtract(4-arg)。
- handleMouseDragEnderChestPickupInsert / handleMouseDragEnderChestExtract：检查 supportsMouseDragged()，将 action 重映射为基础 action 后委托 4 参数核心方法。
- handleEnderChestPickupInsert / handleEnderChestExtract：新增 4 参数重载，esolvedCarried 和 carriedStillMatches 传入 dragSession，其余校验链路（canSafelyReadAndShrink、target stack 二次校验、canSafelyReplace、writeback）保持不变。
- 创造模式：末影箱 carried 在整个拖拽过程中不变，esolvedCarried 在首次调用时使用 cursorStack，syncCreativeCursor 同步未改变的 carried，不复制不丢失。

## 数据安全边界

- 服务端始终重新校验 carried stack、目标槽真实 ItemStack、目标槽是否真实可写、玩家自己的 EnderChestInventory，不信任客户端。
- pickup insert：
  - 服务端读取目标槽 stack 后调用 ENDER_CHEST_SERVICE.pickupInsert，服务层在规则通过时才写入末影箱。
  - 写回前二次校验目标槽未变（ItemStack.matches）、carried 未变（carriedStillMatches）、目标槽可安全替换（canSafelyReplace）。
  - 末影箱满时 esult.changed() == false，服务层不写入，目标槽和末影箱内容均不变。
- extract：
  - 目标槽必须为空（	argetStack.isEmpty()）。
  - 规则层计算候选（extractLastStackFromPlayerEnderChest + skipShulkerBoxes），不直接写入。
  - 写回前二次校验目标槽仍为空、carried 未变。
  - 末影箱内容写回通过 writePlayerEnderChestContents，失败时拒绝。
- 拖拽过程中某个槽失败时保守跳过（eturn），不造成物品复制、丢失、重排或幽灵物品。
- 当前打开 QuickShulker 潜影盒菜单时，末影箱拖拽放出不能把潜影盒放进潜影盒内容槽（skipShulkerBoxes = true）。
- 拖拽状态清理与潜影盒 mouse dragged 一致：左右键同时按下、screen init / close、输入取消和 carried stack 同步。

## 创造模式边界

- 末影箱 carried 不随拖拽变化（末影箱只是触发器/容器门控）。
- esolvedCarried 在创造模式下首次使用 cursorStack，后续不依赖 dragSession.creativeCursor（因为末影箱 carried 不变）。
- syncCreativeCursor 同步未改变的末影箱 carried，不复制不丢失。
- 与阶段 5.2 的创造模式 carried 同步逻辑一致，未新增分叉的 carried 逻辑。

## 客户端行为说明

- 客户端只在 QuickShulker 判定本次输入将发送合法操作请求时才取消对应原版输入。
- pickup drag：只有 isSingleEnderChest(carried) && !hoveredStack.isEmpty() && canInsertIntoEnderChest(hoveredStack) 才发送 MOUSE_DRAG_ENDER_CHEST_PICKUP_INSERT。
- extract drag：只有 isSingleEnderChest(carried) && hoveredStack.isEmpty() 才发送 MOUSE_DRAG_ENDER_CHEST_EXTRACT。
- 如果服务端最终拒绝（例如末影箱满、全是潜影盒），该槽操作静默失败，不产生复制或丢失。客户端不需要预判服务端是否会拒绝。

## 未实现

- 不实现"拿普通物品拖过多个末影箱"的批量 insert。
- 不恢复 ightClickClose、Bundle 或 reopen inventory。

## 验证

- common 单元测试（EnderChestBundlingRulesTest）新增 5 个用例：
  - dragPickupInsertAcceptsShulkerBoxIntoEnderChest：pickup insert 可以收入潜影盒。
  - dragPickupInsertFailsWhenEnderChestFullAndLeavesContentsUnchanged：末影箱满时 pickup insert 失败且内容不变。
  - dragExtractOrderIsBackToFront：连续 extract 顺序为从后往前。
  - dragExtractSkipShulkerKeepsShulkerInPlaceWhenExtractingOrdinary：skip shulker 时跳过潜影盒取普通物品，潜影盒留在原位。
  - dragInsertDoesNotReorderExistingStacks：insert 不会重排已有 stack。
- :common:test 通过（19 个用例，含原有 14 个）。
- Forge 1.20.1：:forge-1.20.1:compileJava 通过。
- NeoForge 1.21.1：:neoforge-1.21.1:compileJava 通过。
- git diff --check 通过。

## 待人工确认项

- 无法进行 Minecraft 游戏内单人 / 多人验收。
- 需要人工确认：
  - 生存模式：拿末影箱从非空槽拖起，拖过普通物品和潜影盒，均收入末影箱。
  - 生存模式：末影箱满时拖过非空槽，静默失败，无变化。
  - 生存模式：拿末影箱从空槽拖起，拖过空槽，从后往前放出物品。
  - 生存模式：在 QuickShulker 潜影盒菜单内拖过潜影盒内容空槽，跳过末影箱内潜影盒，放出普通物品；若全是潜影盒则静默失败。
  - 生存模式：在玩家背包拖过普通空槽，可以放出潜影盒到真实空槽。
  - 创造模式：不复制、不丢失 carried 末影箱 stack。
  - 满背包、多人同时打开、死亡 / 掉线 / 切维度等场景下均无复制 / 丢失 / 幽灵物品。
- 无法确认该跨版本抽象是否适合长期维护（末影箱 drag 与潜影盒 drag 共用 DragSession / resolvedCarried / carriedStillMatches 链路）。
