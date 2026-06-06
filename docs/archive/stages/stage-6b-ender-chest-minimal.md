# 阶段 6B：`ender_chest` 最小功能闭环

> 历史开发记录：本文档记录早期阶段实现过程，不再作为当前实现状态的唯一依据。当前状态请以源码、README 和 `docs/release-0.1.0.md` 为准。


本阶段在阶段 4 / 5 已完成的 `shulker_box` 最小闭环，以及阶段 5.5 / 6A / 6A.5 已收口的输入、宿主锁定、active session 防重入基础上，只新增 `ender_chest` 的最小打开链路。

## 本阶段实现了什么

- 复用现有客户端输入入口：
  - 手持快捷键
  - 无界面手持右键
  - 背包 / 容器界面悬停快捷键
  - 背包 / 容器界面悬停右键
- 双平台把原版 `Items.ENDER_CHEST` 绑定到 common `ender_chest` 类型。
- 客户端只发送：
  - `requestedTypeId = ender_chest`
  - `HostSlotRef`
  - `trigger`
- 服务端重新定位并重新校验宿主：
  - 当前槽位仍有物品
  - 当前物品仍是末影箱
  - 数量仍为 1
  - 当前物品经服务端 registry 解析后仍是 `ender_chest`
  - 当前玩家如已有 active session，会先判断 same-host；同宿主拒绝，不同宿主先安全收尾再重校验打开
- 服务端使用玩家自己的 `EnderChestInventory` / `PlayerEnderChestContainer` 打开 9x3 末影箱菜单。
- 打开期间继续锁定宿主槽位，防止左键、右键、`shift-click`、数字键交换、`Q`、`F`、`PICKUP_ALL`、`QUICK_CRAFT` 等操作移动宿主末影箱。
- 宿主失效时，服务端会保守关闭当前 quick-open 菜单。

## 本阶段没有实现什么

- 不扩展到 Bundle、工作台、切石机、铁砧。
- 不实现 `rightClickClose`。
- 不实现 `reopen inventory` 的 S2C 返回链路。
- 不新增末影箱专用全量 / 单槽同步包。
- 不扩展鼠标拖拽批量插入 / 提取行为。
- 不改动 `shulker_box` 已稳定的宿主锁定和关闭写回逻辑。

## Forge 1.20.1 实现路径

1. 客户端 `ForgeQuickShulkerClient` 继续复用已有输入入口，只把 `resolveTypeId(...)` 从“仅允许 `shulker_box`”扩展为“允许当前 registry 绑定且配置开启的类型”，并把 QuickShulker 菜单判定泛化为 `ForgeQuickOpenMenu`。
2. `ForgeQuickOpenRegistry` 把 `Items.ENDER_CHEST` 绑定到 common `ender_chest`。
3. `ForgeQuickOpenHandler` 先按 `requestedTypeId` 找到服务端类型，再根据当前宿主物品重新解析类型，只有两者一致时才继续打开。
4. `ForgeShulkerSessionManager` 保持“同玩家同一时刻只允许一个 active session”，并在内部按 `quickOpenableTypeId` 分流：
   - `shulker_box` 仍走原有 item-backed 容器与写回逻辑
   - `ender_chest` 走新的 `ForgeEnderChestMenu`
5. `ForgeEnderChestMenu` 继承原版 `ChestMenu(MenuType.GENERIC_9x3, ...)`，直接使用玩家自己的 `player.getEnderChestInventory()`，并复用宿主槽位锁定策略。

## NeoForge 1.21.1 实现路径

1. 客户端 `NeoForgeQuickShulkerClient` 与 Forge 一样复用已有四个输入入口，并把 QuickShulker 菜单判定泛化为 `NeoForgeQuickOpenMenu`。
2. `NeoForgeQuickOpenRegistry` 把 `Items.ENDER_CHEST` 绑定到 common `ender_chest`。
3. `NeoForgeQuickOpenHandler` 继续走“客户端只报类型和槽位，服务端重新解析当前宿主”的权威链路。
4. `NeoForgeShulkerSessionManager` 继续作为单一 active session 管理器，在内部为 `ender_chest` 分流到 `NeoForgeEnderChestMenu`。
5. `NeoForgeEnderChestMenu` 继承原版 `ChestMenu(MenuType.GENERIC_9x3, ...)`，直接使用玩家自己的 `player.getEnderChestInventory()`，并复用现有宿主锁定策略。

## EnderChestInventory 读取 / 保存方式

- 本阶段不把末影箱内容写回宿主 `ItemStack`。
- 打开时直接把玩家自己的 `EnderChestInventory` / `PlayerEnderChestContainer` 作为菜单底层容器传给原版 9x3 `ChestMenu`。
- 关闭时不走 `shulker_box` 的 item-backed 写回。
- 末影箱内容的持久化继续依赖原版 / 服务端玩家数据保存路径。
- 因为使用的就是玩家真实末影箱库存，所以再次打开时读取到的是同一份数据，而不是临时副本。

## 与 `shulker_box` 的差异

- `shulker_box`：
  - 打开的是 item-backed 自定义容器
  - 关闭时需要按宿主物品写回内容
- `ender_chest`：
  - 打开的是玩家真实 `EnderChestInventory`
  - 关闭时不向宿主物品写回内容
  - 数据风险重点从“宿主物品写回”转为“原版末影箱库存保存与同步”

## 输入入口复用方式

- 手持快捷键：继续使用现有 held-item keybind 入口。
- 无界面手持右键：继续使用阶段 6A.5 的 `RightClickItem` 边界，只在原版已经进入 item-use 分支时才发送 quick-open 请求。
- 界面悬停快捷键：继续使用已有 `HostSlotRef` 映射。
- 界面悬停右键：继续使用已有“玩家背包槽位 + carried 为空 + hover 槽位合法”判定。
- 客户端不会因为新增 `ender_chest` 再复制出一套专用输入事件处理器。

## 宿主锁定策略

- `ender_chest` 菜单虽然不需要把内容写回宿主物品，但宿主末影箱仍是这次 quick-open 的触发器，因此打开期间继续锁定。
- 双平台新菜单都沿用 `shulker_box` 的宿主锁定思路：
  - 锁定宿主可见 menu slot
  - 锁定对应热栏数字键交换目标
  - 阻止 `ClickType.SWAP + offhand button`
  - 阻止 `quickMoveStack`
  - 阻止 `canDragTo`
  - 阻止 `canTakeItemForPickAll`
- 服务端每 tick 仍会重新校验宿主；宿主失效时关闭当前菜单。

## 重复打开保护

- 客户端：当前菜单中如果目标还是同一宿主，会直接拒绝重复打开；如果目标是不同宿主，则允许继续发送切换请求。
- 服务端：仍只维护同一玩家一个 active session，但不会“一有 active session 就拒绝全部请求”。
- `ender_chest` 与 `shulker_box` 共用同一套 active session 管理：同一宿主重复打开拒绝，不同宿主先安全收尾旧 session，再重校验并打开新宿主。

## 已知风险

- 本地未做 Minecraft 实机验证，当前只能确认编译和模块构建通过。
- 本阶段没有做专用服务器 / 多人联机验证，不能声称多人下无复制风险。
- 本阶段没有补 `EnderChestInventory` 专用 S2C 同步包；当前判断依据是直接复用玩家真实末影箱库存与原版容器同步路径。
- `ender_chest` 目前继续要求宿主堆叠数为 1，这与本任务要求一致；若后续要对齐其他参考实现中的更宽松单堆叠策略，需要单独评估行为变化。

## 人工测试清单

### Forge 1.20.1

- 主手末影箱快捷键打开
- 副手末影箱快捷键打开
- 主手 / 副手末影箱无界面右键打开
- 对可放置位置右键时优先放置末影箱，不打开 QuickShulker
- 对可交互方块 / 实体右键时优先原版交互，不打开 QuickShulker
- 背包界面悬停热栏 / 主背包 / 副手末影箱，快捷键打开
- 背包界面悬停热栏 / 主背包 / 副手末影箱，右键打开，且原版不拿起物品
- 放入物品后 `Esc` / `E` 关闭，再次打开内容存在
- 取出物品后 `Esc` / `E` 关闭，再次打开内容变化正确
- 打开期间宿主末影箱不能被左键、右键、`shift-click`、数字键、`Q`、`F` 移动
- 当前 quick-open 菜单内再次右键宿主，不会打开第二个页面
- 创造模式下至少确认无明显复制或幽灵物品

### NeoForge 1.21.1

- 同上 12 项全部复测

### 专用服务器 / 多人

- 双端安装后验证末影箱打开与关闭保存
- 宿主在多人环境中被移动 / 掉线 / 切维度时是否保守关闭
- 创造 / 生存下是否存在复制或幽灵物品

## 后续阶段 6C / 7 需要继续做什么

### 阶段 6C

- 如果实机验证发现原版末影箱同步不足，再评估是否必须补专用 `ender_chest` S2C 同步包
- 继续补 `ender_chest` 在异常关闭、快速切屏、边界输入下的行为验证
- 视测试结果决定是否需要更细的菜单失效与回滚日志

### 阶段 7

- 创造模式专项验证
- 专用服务器 / 多人专项验证
- 评估是否继续扩展到其它 quick-openable
- 评估当前 session manager 泛化是否需要长期重命名或进一步抽象

## 参考来源

- `references/quickshulker-1.20`
- `references/quickshulker-1.21.1`
- `references/quickshulker-26.1-neo`

本阶段未直接修改 `references/` 目录。
