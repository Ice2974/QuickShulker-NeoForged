# stage-1.0.0-p5-ender-chest-right-click-bundling

本阶段接入末影箱 bundling 的单次右键输入、C2S intent、服务端 handler 和双平台实现。

本阶段不接入 mouse dragged。拖拽留到阶段 6；本阶段也不恢复 `rightClickClose`、Bundle 或 reopen inventory。

## 完成内容

- `common` 新增末影箱 bundling 网络 action：
  - `ENDER_CHEST_INSERT`
  - `ENDER_CHEST_PICKUP_INSERT`
  - `ENDER_CHEST_EXTRACT`
- Forge 1.20.1 / NeoForge 1.21.1 客户端右键识别优先判断末影箱语义，再判断潜影盒语义。
- Forge 1.20.1 / NeoForge 1.21.1 服务端 handler 接入玩家作用域末影箱 bundling：
  - 鼠标拿普通物品或潜影盒，右键槽内单个末影箱，将鼠标物品收入玩家自己的 `EnderChestInventory`。
  - 鼠标拿单个末影箱，右键普通物品或潜影盒，将目标槽物品收入玩家自己的 `EnderChestInventory`。
  - 鼠标拿单个末影箱，右键真实空槽，从玩家自己的 `EnderChestInventory` 取出最后一组物品放入该空槽（从后往前，阶段 5.1 统一）。
- 服务端每次处理都重新读取真实 carried stack、真实目标槽和玩家自己的 `player.getEnderChestInventory()`。
- 末影箱内容始终来自玩家真实末影箱库存，不读取或写回末影箱 `ItemStack` 的 NBT / DataComponent。
- 当前菜单是潜影盒菜单时，只有即将从末影箱取出的物品是潜影盒时才拒绝 extract；普通物品允许放出到潜影盒内容空槽。（阶段 5.1 修正：原阶段 5 过于粗暴地拒绝了所有潜影盒内容槽 extract。）

## 行为边界

- 末影箱优先级高于潜影盒：
  - 拿末影箱右键潜影盒，解释为“把潜影盒装入末影箱”。
  - 拿潜影盒右键末影箱，解释为“把潜影盒装入末影箱”。
  - 末影箱满时直接失败，不回退为“把末影箱装入潜影盒”。
- `ENDER_CHEST_EXTRACT` 只允许写入真实空槽。
- `ENDER_CHEST_EXTRACT` 不参与 mouse dragged，也不会创建拖拽 session。
- 从末影箱中取出的潜影盒只允许作为普通物品放到安全空槽（含玩家背包），不允许放入潜影盒内容槽；普通物品允许放入潜影盒内容空槽。（阶段 5.1 细化了该判断条件。）
- 如果当前打开的是末影箱 quick-open 菜单，右键 bundling 后通过现有菜单 broadcast / slot sync 路径同步显示。

## 验证重点

- Forge 1.20.1：
  - 鼠标普通物品右键槽内末影箱，确认物品进入玩家末影箱。
  - 鼠标潜影盒右键槽内末影箱，确认潜影盒进入玩家末影箱。
  - 鼠标末影箱右键普通物品或潜影盒，确认目标槽扣减或清空，物品进入玩家末影箱。
  - 鼠标末影箱右键真实空槽，确认取出最后一组末影箱物品（从后往前）。
  - 鼠标末影箱右键潜影盒菜单内容空槽，确认不会把取出的潜影盒放入潜影盒内容。
- NeoForge 1.21.1：同 Forge 维度。
- 多人测试需要确认服务端不信任客户端上传物品状态，且玩家之间末影箱库存互不串写。

## 未覆盖

- mouse dragged 末影箱 bundling。
- Bundle bundling。
- `rightClickClose`。
- reopen inventory。
- Minecraft 游戏内单人 / 多人验收。
