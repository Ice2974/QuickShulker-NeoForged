# stage-1.0.0-p5.3-ender-chest-extract-skip-shulker

本阶段调整末影箱向潜影盒内容槽放出物品时的候选选择逻辑：遇到潜影盒时跳过，继续从后往前寻找下一个可放出的普通物品；如果末影箱里全是潜影盒，则拒绝放出操作且末影箱库存不变。

## 问题现象

- 阶段 5.1 已修复末影箱右键放出顺序，统一为从后往前（与潜影盒一致）。
- 阶段 5.1 的防嵌套策略是：先按从后往前取出最后一个非空 stack，再判断"如果目标槽是潜影盒菜单内容槽且取出的恰好是潜影盒，则整体拒绝"。
- 这能避免潜影盒嵌套进潜影盒，但体验不够好：当末影箱末尾是潜影盒、前面还有普通物品时，玩家右键潜影盒内容空槽会被直接拒绝，明明有普通物品可放却放不出来。
- 期望行为：遇到潜影盒时跳过，继续从后往前寻找下一个可放出的普通物品；如果末影箱里全是潜影盒，才拒绝放出操作。

## 根因

- common 规则层 `ContainerBundlingRules.extractLastStack` 只支持"从后往前取第一个非空 stack"，没有按目标约束跳过特定物品的能力。
- `EnderChestBundlingRules.extractLastStackFromPlayerEnderChest` 直接委托上述方法，无法表达"目标槽是潜影盒内容槽时跳过潜影盒"。
- 平台层 `handleEnderChestExtract` 因此只能在取出之后做后置拒绝：`if (isShulkerMenuContainerSlot(...) && isShulkerBox(extractedStack)) return;`，导致即使前面有普通物品也被一并拒绝。

## 修复方式

在 common 规则层引入带 predicate 的 extract，让"是否跳过某 stack"成为可配置约束，同时保留普通 extract 语义不变。

### common 规则层

- `ContainerBundlingRules` 新增重载：

  ```
  public static <S> ShulkerBundlingResult<List<S>, S> extractLastStack(
      List<S> originalContents,
      ShulkerBundlingStackAdapter<S> adapter,
      Predicate<S> acceptable
  )
  ```

  从后往前扫描，跳过空 stack 和不满足 `acceptable` 的 stack，取第一个可接受的非空 stack。原有无 predicate 的 `extractLastStack` 改为委托新方法并传入 `stack -> true`，语义不变。

- `EnderChestBundlingRules.extractLastStackFromPlayerEnderChest` 新增 `boolean skipShulkerBoxes` 重载：

  - `skipShulkerBoxes == true` 时，向 `ContainerBundlingRules.extractLastStack` 传入 `stack -> !adapter.isShulkerBox(stack)`，即从后往前跳过所有潜影盒，取第一个普通物品。
  - `skipShulkerBoxes == false` 时，保持原语义（从后往前取最后一个非空 stack，即使是潜影盒也可以取出）。
  - 原有无参重载委托新方法并传入 `false`，调用方无需改动。

- `PlayerEnderChestBundlingService.extractLastStack` 同样新增 `boolean skipShulkerBoxes` 重载，原无参方法委托传入 `false`。

### 平台层（Forge 1.20.1 / NeoForge 1.21.1）

`handleEnderChestExtract` 调整为：

1. 在 carried 校验通过后、调用规则前，先计算：

   ```
   boolean targetIsShulkerContentSlot = isShulkerMenuContainerSlot(player, intent.hostSlot());
   ```

2. 调用规则时传入该标志：

   ```
   EnderChestBundlingRules.extractLastStackFromPlayerEnderChest(
       ENDER_CHEST_ACCESS.readPlayerEnderChestContents(player),
       ENDER_CHEST_ADAPTER,
       targetIsShulkerContentSlot
   );
   ```

3. 移除阶段 5.1 引入的后置拒绝块（`if (isShulkerMenuContainerSlot(...) && isShulkerBox(extractedStack)) return;`）。
   - 因为规则层已在候选选择阶段跳过潜影盒，取出的 stack 不可能是潜影盒。
   - 如果末影箱全是潜影盒（或为空），`result.changed()` 为 `false`，会被既有的 `if (!result.changed() ...) return;` 拦截，末影箱库存不变。
4. 后续的 `canSafelyReplace`、目标槽二次校验、carried 二次校验、writeback 链路保持不变。

## 行为边界

- 目标槽是潜影盒 quick-open 菜单或原版潜影盒菜单的内容空槽时：
  - 从玩家自己的 `EnderChestInventory` 从后往前扫描。
  - 跳过所有潜影盒。
  - 找到第一个非潜影盒物品后放出到目标空槽。
  - 如果没有可放出的非潜影盒物品（全是潜影盒或为空），拒绝，且末影箱库存不变。
- 目标槽不是潜影盒内容槽时（例如玩家背包普通空槽）：
  - 保持现有规则：从后往前放出最后一个非空物品。
  - 即使该物品是潜影盒，也可以放到真实空槽（不会发生嵌套，因为目标不是潜影盒内容槽）。
- 不允许潜影盒放入潜影盒内容槽。
- 跳过潜影盒不会改变其他槽位的正常放出顺序：被跳过的潜影盒仍留在原槽位，只有被选中的普通物品所在槽位被清空。
- 服务端始终基于玩家自己的真实 `EnderChestInventory` 计算候选，不信任客户端。
- 失败时不删除、移动或重排末影箱内物品：规则层失败时返回的 `updatedContainerStack` 是原始内容的副本，平台层在 `result.changed() == false` 时直接 return，不触发 writeback。
- 创造模式下不复制、不丢失 carried stack：carried 链路与阶段 5.2 一致，本次未改动 carried 同步逻辑。
- 未恢复 `rightClickClose`、Bundle 或 reopen inventory。
- 本阶段未新增完整 mouse dragged 放出功能；如果将来引入末影箱拖拽放出，应复用同一套候选选择规则（即根据目标槽是否潜影盒内容槽决定是否 `skipShulkerBoxes`）。

## 客户端行为说明

- 客户端 `determineEnderChestBundlingIntent` 未修改：仍然在拿起单个末影箱且悬停真实空槽时生成 `ENDER_CHEST_EXTRACT` intent 并取消原版右键。
- 这符合 AGENTS.md 输入规则：客户端只在 QuickShulker 判定本次输入将发送合法操作请求时才取消对应原版输入。
- 如果服务端最终拒绝（例如末影箱全是潜影盒而目标是潜影盒内容槽），该次操作静默失败，不产生复制或丢失。客户端不需要预判服务端是否会拒绝。

## 验证

- common 单元测试（`EnderChestBundlingRulesTest`）新增三个用例：
  - `extractSkipShulkerPicksOrdinaryItemBeforeShulkerAtTail`：末尾是潜影盒、前面有普通物品时，向潜影盒内容槽放出普通物品。
  - `extractSkipShulkerFailsWhenOnlyShulkersRemainAndLeavesContentsUnchanged`：末影箱全是潜影盒时，向潜影盒内容槽放出失败且内容不变。
  - `extractWithoutSkipStillReturnsLastShulkerForPlainSlot`：向普通真实空槽放出时，仍能放出最后一个潜影盒。
- `:common:test` 通过（含原有用例）。
- Forge 1.20.1：`:forge-1.20.1:compileJava` 通过。
- NeoForge 1.21.1：`:neoforge-1.21.1:compileJava` 通过。

## 待人工确认项

- 无法进行 Minecraft 游戏内单人 / 多人验收。
- 需要人工确认：
  - 生存模式：末影箱内末尾是潜影盒、前面有普通物品，右键潜影盒内容空槽，应跳过潜影盒并放出普通物品。
  - 生存模式：末影箱内全是潜影盒，右键潜影盒内容空槽，应拒绝且无变化。
  - 生存模式：右键玩家背包普通空槽，仍按从后往前放出，包括可以放出潜影盒。
  - 创造模式：不复制、不丢失 carried stack。
  - 满背包、多人同时打开、死亡 / 掉线 / 切维度等场景下均无复制 / 丢失 / 幽灵物品。