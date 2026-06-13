# stage-1.0.0-p5.1-ender-chest-extract-fix

本阶段修复末影箱右键放出物品在潜影盒界面内容槽无反应的问题，并将末影箱放出顺序统一为从后往前（与潜影盒一致）。

## 问题现象

- 阶段 5 末影箱右键收纳物品功能基本正常。
- 拿起末影箱，在潜影盒 quick-open 菜单或原版潜影盒界面的内容空格上右键尝试放出物品时，完全没有反应。
- 表现为：客户端取消了原版右键输入并发送了 ENDER_CHEST_EXTRACT intent，但服务端因为 isShulkerMenuContainerSlot 判断直接拒绝了所有潜影盒菜单内容槽的末影箱 extract。
- 该保护逻辑原本是为了避免"末影箱里取出的潜影盒被放进潜影盒"，但过于粗暴地拒绝了所有潜影盒菜单内容槽 extract，包括普通物品。

## 根因

- 客户端 determineEnderChestBundlingIntent 在拿起单个末影箱且悬停真实空槽时，无条件生成 ENDER_CHEST_EXTRACT intent 并 setCanceled(true)。
- 服务端 handleEnderChestExtract 顶部有 isShulkerMenuContainerSlot(player, intent.hostSlot()) 判断，当当前菜单是 ShulkerBoxMenu（含 quick-open 菜单和原版菜单）且目标槽位是菜单内容槽时，直接拒绝。
- 这导致客户端取消了原版右键，但服务端又拒绝了 QuickShulker 的末影箱 extract，玩家看到的最终效果是"无反应"。

## 完成内容

- common 规则层重命名：
  - EnderChestBundlingRules.extractFirstStackFromPlayerEnderChest 改为 extractLastStackFromPlayerEnderChest，内部改为调用 ContainerBundlingRules.extractLastStack（从后往前）。
  - PlayerEnderChestBundlingService.extractFirstStack 改为 extractLastStack。
  - 末影箱放出顺序与潜影盒一致：从末影箱库存最后非空槽位取出。
- Forge 1.20.1 / NeoForge 1.21.1 服务端 handleEnderChestExtract 调整：
  - 移除顶部的 isShulkerMenuContainerSlot 全量拒绝。
  - 改为先执行末影箱 extract（基于服务端真实 EnderChestInventory 计算即将取出的 ItemStack），再判断：
    - 如果目标槽位是潜影盒菜单内容槽 **且** 即将放出的物品是潜影盒，才拒绝（避免潜影盒嵌套进潜影盒）。
    - 如果即将放出的物品是普通物品，允许放出到潜影盒菜单内容空槽。
  - 调用从 extractFirstStackFromPlayerEnderChest 改为 extractLastStackFromPlayerEnderChest。
- 服务端仍然不信任客户端传来的物品状态；extract 基于玩家自己的 EnderChestInventory 预览/计算真实 ItemStack，再决定是否允许写入目标槽。
- 末影箱内容仍然只来自玩家自己的 EnderChestInventory，不读取或写回末影箱 ItemStack NBT / DataComponent。

## 行为边界

- 末影箱优先级仍高于潜影盒：末影箱满时不能回退为潜影盒 bundling。
- ENDER_CHEST_EXTRACT 只允许写入真实空槽。
- 从末影箱即将取出的物品是普通物品时，允许放出到潜影盒 quick-open 菜单或原版潜影盒界面的内容空槽。
- 从末影箱即将取出的物品是潜影盒时，拒绝放出到潜影盒内容槽，避免潜影盒嵌套进潜影盒。
- 末影箱放出顺序统一为从后往前（与潜影盒 extract 一致）。
- 未恢复 ightClickClose、Bundle 或 reopen inventory。

## 客户端行为说明

- 客户端 determineEnderChestBundlingIntent 未修改：仍然在拿起单个末影箱且悬停真实空槽时生成 ENDER_CHEST_EXTRACT intent 并取消原版右键。
- 这符合 AGENTS.md 输入规则：客户端只在 QuickShulker 判定本次输入将发送合法打开/操作请求时才取消对应原版输入。
- 如果服务端最终拒绝（例如取出的物品是潜影盒而目标是潜影盒内容槽），该次操作静默失败，不产生复制或丢失。
- 客户端不需要预判服务端是否会拒绝，因为"取消原版右键"发生在 intent 发送时，而服务端拒绝不会导致数据损坏。

## 验证

- common 单元测试：EnderChestBundlingRulesTest 已更新，验证从后往前取出语义。
- Forge 1.20.1：:forge-1.20.1:compileJava 通过。
- NeoForge 1.21.1：:neoforge-1.21.1:compileJava 通过。

## 待人工确认项

- 无法进行 Minecraft 游戏内单人 / 多人验收。
- 需要人工确认：
  - 拿起末影箱右键潜影盒 quick-open 菜单内容空槽，普通物品可正常放出。
  - 拿起末影箱右键原版潜影盒界面内容空槽，普通物品可正常放出。
  - 末影箱最后非空槽是潜影盒时，右键潜影盒内容空槽应被拒绝（不发生嵌套）。
  - 末影箱放出顺序与潜影盒 extract 顺序一致（从后往前）。
  - 创造模式、生存模式、满背包、多人环境下均无复制 / 丢失 / 幽灵物品。