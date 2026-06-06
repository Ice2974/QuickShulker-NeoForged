# 阶段 6C：functional menus quick-open

> 历史开发记录：本文档记录早期阶段实现过程，不再作为当前实现状态的唯一依据。当前状态请以源码、README 和 `docs/release-0.1.0.md` 为准。


本阶段在已完成的 `shulker_box`、`ender_chest`、四个输入入口、宿主锁定、active session 防重入和 quick-open 切换基础上，补齐三类“不把内容写回宿主物品”的功能菜单 quick-open：

- `crafting_table`
- `stonecutter`
- `anvil`

本阶段目标是先打通最小可用闭环，不改原版菜单配方、经验、重命名、切石或铁砧消耗规则，也不额外扩张到 Bundle、鼠标拖拽批量行为、`rightClickClose` 或 reopen inventory S2C。

## 本阶段实现了什么

- 双平台 registry 新增：
  - `minecraft:crafting_table -> crafting_table`
  - `minecraft:stonecutter -> stonecutter`
  - `minecraft:anvil / chipped_anvil / damaged_anvil -> anvil`
- 继续复用已有四个 quick-open 输入入口：
  - 手持快捷键
  - 无界面手持右键
  - 背包 / 容器界面悬停快捷键
  - 背包 / 容器界面悬停右键
- 客户端只发送：
  - `requestedTypeId`
  - `HostSlotRef`
  - `trigger`
- 服务端继续按当前槽位重取宿主物品，并重新校验：
  - registry 解析出的当前类型是否仍与 `requestedTypeId` 一致
  - 当前堆叠数是否满足该类型要求
  - 当前玩家是否正在对同一宿主重复打开
- 三类新菜单都继续接入既有宿主锁定：
  - 左键 / 右键拿起
  - `shift-click`
  - 数字键交换
  - `Q`
  - `F`
  - `PICKUP_ALL`
  - `QUICK_CRAFT`
- 继续复用现有切换策略：
  - 同一宿主重复打开拒绝
  - 不同宿主允许切换
  - 切换前先走既有关闭路径安全收尾当前菜单
  - 收尾后必须重新校验新宿主仍然有效，才真正打开新菜单
  - 切换期间始终保持单 session，不创建两个互不相通的容器副本
- 鼠标恢复逻辑从“按 screen class 猜测”收紧为“确认目标 screen 的 menu 是本模组 `QuickOpenMenu` 且类型匹配”，因此新菜单也自动覆盖到：
  - `crafting_table`
  - `stonecutter`
  - `anvil`

## 本阶段没有实现什么

- `Bundle` 独立菜单
- 鼠标拖拽批量插入 / 提取
- `rightClickClose`
- reopen inventory S2C
- 对原版工作台 / 切石机 / 铁砧规则做增强或魔改
- 专用服务器 / 多人实机验证

## Forge 1.20.1 实现路径

- 输入入口：
  - `ForgeQuickShulkerClient`
- 类型注册：
  - `ForgeQuickOpenRegistry`
- 服务端权威校验：
  - `ForgeQuickOpenHandler`
- active session / 切换收尾：
  - `ForgeShulkerSessionManager`
- 新增菜单：
  - `ForgeCraftingTableMenu`
  - `ForgeStonecutterMenu`
  - `ForgeAnvilMenu`
- 宿主锁定复用：
  - `ForgeHostLockedMenuSupport`
- 鼠标恢复硬化：
  - `forge/client/ForgeQuickOpenMouseRestore`

### Forge 三类菜单如何打开

- `crafting_table`
  - 打开 `CraftingMenu`
  - `ContainerLevelAccess` 使用玩家当前位置
  - `stillValid` 改为只受本模组 host invalidation 控制，不要求脚下真有工作台
- `stonecutter`
  - 打开 `StonecutterMenu`
  - `ContainerLevelAccess` 使用玩家当前位置
  - `stillValid` 同样改为只受本模组 host invalidation 控制
- `anvil`
  - 打开 `AnvilMenu`
  - 使用 `HostBackedAnvilAccess`
  - `stillValid` 不要求玩家附近存在原版铁砧方块
  - `onTake` 触发的铁砧损耗通过宿主物品处理：
    - `anvil -> chipped_anvil`
    - `chipped_anvil -> damaged_anvil`
    - `damaged_anvil -> empty`
  - 同时保留原版铁砧使用 / 破碎音效

## NeoForge 1.21.1 实现路径

- 输入入口：
  - `NeoForgeQuickShulkerClient`
- 类型注册：
  - `NeoForgeQuickOpenRegistry`
- 服务端权威校验：
  - `NeoForgeQuickOpenHandler`
- active session / 切换收尾：
  - `NeoForgeShulkerSessionManager`
- 新增菜单：
  - `NeoForgeCraftingTableMenu`
  - `NeoForgeStonecutterMenu`
  - `NeoForgeAnvilMenu`
- 宿主锁定复用：
  - `NeoForgeHostLockedMenuSupport`
- 鼠标恢复硬化：
  - `neoforge/client/NeoForgeQuickOpenMouseRestore`

### NeoForge 三类菜单如何打开

- `crafting_table`
  - 打开原版 `CraftingMenu`
- `stonecutter`
  - 打开原版 `StonecutterMenu`
- `anvil`
  - 打开原版 `AnvilMenu`
  - 与 Forge 一样通过宿主物品处理铁砧损耗和音效

## 与 shulker_box / ender_chest 的差异

- `shulker_box`
  - 菜单内容写回宿主 `ItemStack`
- `ender_chest`
  - 菜单内容来自玩家 `EnderChestInventory`
- `crafting_table / stonecutter / anvil`
  - 不把菜单内容写回宿主物品
  - 也不读取玩家 `EnderChestInventory`
  - 关闭时完全依赖原版菜单自己的输入 / 输出槽位收尾规则
  - 但宿主仍然是 quick-open 会话的绑定对象，因此继续锁定

## 输入入口复用方式

- 客户端没有新增第二套输入系统
- `resolveTypeId(...)` 仍统一从平台 registry 查当前物品类型
- `HostSlotRef` 仍统一复用既有手持 / 背包槽位映射
- 发包前仍经过客户端配置过滤
- 发包后仍由服务端重新解析当前宿主物品，不信任客户端 `ItemStack`

## 配置项

继续复用已有配置语义：

- `quickCraftingTable`
- `quickStonecutter`
- `quickAnvil`
- `keybindInHand`
- `keybindInInventory`
- `rightClickToOpen`
- `rightClickInInventory`

行为要求：

- 客户端入口会先检查对应开关
- 服务端 `QuickOpenHandler` 也会再次检查
- 关闭后客户端不会主动发送，对应服务端也会拒绝

## 无界面右键如何避免抢原版交互

- 仍沿用阶段 6A.5 的 `RightClickItem` 入口
- 只有原版已经进入“物品右键使用”分支时才触发 quick-open
- 因此：
  - 对可放置位置右键时，优先原版放置方块
  - 对可交互方块 / 实体右键时，优先原版交互
- 本阶段没有额外添加 `RightClickBlock` / `RightClickEntity` 抢占逻辑

## 宿主锁定策略

- 继续锁定玩家库存中的宿主槽位
- 新增的工作台 / 切石机 / 铁砧菜单都实现了 `ForgeQuickOpenMenu` / `NeoForgeQuickOpenMenu`
- 并复用平台侧 `HostLockedMenuSupport`
- 目标是让 functional menu 与已有 `shulker_box` / `ender_chest` 在锁定策略上保持一致

## 切换打开策略

- 仍由 `ForgeShulkerSessionManager` / `NeoForgeShulkerSessionManager` 作为单一 active session 管理器
- 同一宿主再次请求：拒绝
- 不同宿主请求：先关闭旧菜单，再重新校验新宿主，再打开新菜单
- 因为 `crafting_table / stonecutter / anvil` 都走 transient session：
  - 不会写回宿主物品
  - 但切换离开时仍会执行原版菜单的关闭收尾
- 这样可以避免：
  - 重复页面
  - `shulker_box` / `ender_chest` 切换时旧内容回退

## 已知风险

- 本地尚未做 Minecraft 客户端实机验证，functional menu 的所有边界仍需人工测试
- 尚未做多人 / 专用服务器复制风险实测，不能声称“已确认无复制问题”
- `CraftingMenu` / `StonecutterMenu` 为了脱离真实世界方块打开，`stillValid` 被改为依赖本模组 host invalidation，而不是原版方块存在校验
- `AnvilMenu` 使用了自定义 `ContainerLevelAccess`，目的是：
  - 避免误伤世界里的真实铁砧方块
  - 把铁砧损耗落到宿主物品
- 这部分实现虽然尽量贴近参考项目，但仍建议重点人工验证：
  - 取走输出时的宿主损耗
  - 快速关闭 / 切换时输入槽返还
  - 创造模式下是否出现幽灵物品

## 人工测试清单

### Forge 1.20.1

- 主手 / 副手工作台快捷键打开
- 主手 / 副手切石机快捷键打开
- 主手 / 副手铁砧快捷键打开
- 手持三类方块无界面右键打开，只在不抢原版交互时触发
- 背包界面悬停热栏 / 主背包 / 副手中的三类方块，快捷键打开
- 背包界面悬停热栏 / 主背包 / 副手中的三类方块，右键打开且不拿起物品
- 对可放置位置右键时优先原版放置
- 对可交互方块 / 实体右键时优先原版交互
- 打开期间宿主不能被左键、右键、`shift-click`、数字键、`Q`、`F` 移动
- 从 `shulker_box` / `ender_chest` 切换到三类菜单，原有内容保存不回退
- 从三类菜单切回 `shulker_box` / `ender_chest` 时，当前菜单物品按原版规则收尾
- 创造模式确认无明显复制或幽灵物品

### NeoForge 1.21.1

- 与 Forge 1.20.1 同一组 12 项

### 专用服务器 / 多人

- 客户端 + 服务端双端安装
- 同一宿主重复打开拒绝
- 不同宿主切换打开正常收尾
- 死亡 / 掉线 / 切维度后的菜单收尾
- 创造 / 生存模式下是否出现复制或幽灵物品

## 阶段 7 仍需继续做什么

- 完成双平台 Minecraft 客户端实机验证
- 完成专用服务器 / 多人验证
- 继续补 functional menus 在异常关闭、快速切换、边界输入下的行为测试
- 评估是否需要把更多参考实现差异回灌到 current port
- 如后续要继续扩展其它 quick-openable，再在现有单一输入 / session / 锁定框架上增量接入
