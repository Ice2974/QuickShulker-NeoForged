# 阶段 6A：`shulker_box` 基础输入入口与配置落地

本阶段在阶段 4 / 5 已有的双平台“手持潜影盒快捷打开最小闭环”基础上，补齐了 `shulker_box` 的基础输入入口和平台配置落地。

目标仍然保持保守：

- 只支持 `shulker_box`
- 所有打开入口最终仍走既有的 C2S `OpenHostItemIntent -> 服务端重新校验 -> 会话打开` 链路
- 不绕过阶段 4 / 5 已有的统一关闭保存入口
- 只支持能够稳定映射到玩家背包 `HostSlotRef` 的槽位

## 本阶段实现了什么

### 1. 双平台配置落地

Forge 1.20.1：

- 新增 `ForgeQuickShulkerConfig`
- 使用 `ForgeConfigSpec` 注册 `CLIENT` 配置
- 由 `QuickShulkerNeoForged` 在模组初始化时注册

NeoForge 1.21.1：

- 新增 `NeoForgeQuickShulkerConfig`
- 使用 `ModConfigSpec` 注册 `CLIENT` 配置
- 由 `QuickShulkerNeoForged` 在模组初始化时注册

本阶段真正落地并参与运行时判定的配置语义：

- `quickShulkerBox`
- `keybindInHand`
- `keybindInInventory`
- `rightClickInInventory`
- `rightClickToOpen`
- `rightClickClose`
- `supportsMouseDragged`

说明：

- `rightClickClose` 本阶段只保留配置项，不实现“再次右键关闭并返回背包”
- `supportsMouseDragged` 本阶段只保留配置项，不实现拖拽批量行为
- `activationKey` / `openSettingsKey` 仍沿用 common 默认语义，本阶段未接入独立配置界面或动态重绑逻辑

### 2. 手持快捷键继续复用统一链路

Forge / NeoForge 两个平台都保留了阶段 4 / 5 的：

- 主手优先
- 副手兜底
- 仅允许数量为 `1` 的潜影盒
- 客户端只发 `requestedTypeId + HostSlotRef + trigger`
- 服务端重新取当前宿主 `ItemStack`
- 服务端重新校验宿主类型与数量

与阶段 4 / 5 的区别是：

- 现在手持快捷键会先读取平台配置 `keybindInHand`
- 不再把“按下快捷键一定打开手持潜影盒”写死为唯一业务入口

### 3. 背包 / 容器界面悬停快捷键打开

Forge 1.20.1 输入接入点：

- `ScreenEvent.KeyPressed.Pre`

NeoForge 1.21.1 输入接入点：

- `ScreenEvent.KeyPressed.Pre`

行为：

- 当前打开的是 `AbstractContainerScreen`
- 鼠标悬停槽位有物品
- 当前光标未携带物品
- 配置 `keybindInInventory = true`
- 悬停槽位可稳定映射到玩家背包 `HostSlotRef`
- 悬停物品属于 `shulker_box`
- 且数量必须为 `1`

满足这些条件时，客户端会发出 `QuickOpenTrigger.INVENTORY_KEYBIND` 的打开请求。

### 4. 背包 / 容器界面右键打开

Forge 1.20.1 输入接入点：

- `ScreenEvent.MouseButtonPressed.Pre`

NeoForge 1.21.1 输入接入点：

- `ScreenEvent.MouseButtonPressed.Pre`

行为：

- 仅处理鼠标右键
- 当前光标未携带物品
- 配置 `rightClickInInventory = true`
- 配置 `rightClickToOpen = true`
- 悬停槽位可稳定映射到玩家背包 `HostSlotRef`
- 悬停物品属于 `shulker_box`
- 且数量必须为 `1`

满足这些条件时：

- 客户端发出 `QuickOpenTrigger.INVENTORY_RIGHT_CLICK`
- 事件在客户端侧被取消，避免继续进入原版右键拿起 / 放下逻辑

## HostSlotRef 映射策略

本阶段没有把 screen slot index 直接当作玩家背包 index 使用。

两个平台都采用同样的逻辑语义：

1. 客户端先确认该槽位底层容器就是玩家自身背包
2. 再读取该槽位在玩家背包容器中的真实 `container slot`
3. 最后转换为 common `HostSlotRef`

映射规则：

- `0..8` -> `HostStorageScope.PLAYER_HOTBAR`
- `9..35` -> `HostStorageScope.PLAYER_MAIN_INVENTORY`
- `40` -> `HostStorageScope.PLAYER_OFFHAND`

其中：

- `logicalSlotIndex` 对 hotbar 为 `0..8`
- `logicalSlotIndex` 对 main inventory 为 `0..26`
- `logicalSlotIndex` 对 offhand 固定为 `0`
- `menuSlotIndex` 保留当前菜单中的槽位 index，供后续阶段继续扩展和调试使用

服务端解析宿主时仍以 `scope + logicalSlotIndex` 为准，不信任客户端传来的 `ItemStack` 内容。

## 哪些槽位支持打开

本阶段支持：

- 玩家热键栏中的潜影盒
- 玩家主背包 27 格中的潜影盒
- 玩家副手槽中的潜影盒
- 上述槽位在玩家背包界面中
- 上述槽位在其他容器界面中，只要该槽位底层仍然是玩家自己的背包容器

## 哪些槽位暂不支持打开

本阶段暂不支持：

- 容器自身非玩家背包槽位中的潜影盒
- 盔甲槽
- 创造模式特殊槽位
- 配方书、搜索栏、创造物品栏包装槽等特殊 UI 槽位
- 无法稳定判断底层容器归属的特殊菜单槽位
- 鼠标拖拽批量插入 / 提取相关入口
- `rightClickClose`

原因：

- 这些槽位要么不是玩家背包宿主
- 要么 screen slot 与 inventory slot 的映射不够稳定
- 要么会引入更复杂的保存 / 关闭 / reopen inventory 语义，本阶段优先保守不支持

## 关闭保存逻辑

本阶段没有新增新的保存链路。

Forge 1.20.1：

- 继续复用阶段 4 的 `ForgeShulkerSessionManager` 统一关闭入口

NeoForge 1.21.1：

- 继续复用阶段 5 的 `NeoForgeShulkerSessionManager` 统一关闭入口

因此仍保持：

- 正常关闭 + 宿主有效 -> 写回实时容器内容
- 宿主失效 -> 放弃写回
- 不因为新增右键 / 悬停入口而绕过服务端校验

## 本阶段没有实现什么

- 末影箱同步
- Bundle 独立菜单
- 工作台 / 切石机 / 铁砧
- 鼠标拖拽批量插入 / 提取
- shulker 对 shulker 转移逻辑
- `rightClickClose`
- reopen inventory S2C
- 容器自身非玩家背包槽位中的潜影盒打开
- 创造模式特殊槽位支持

## 已知风险

- Forge 1.20.1 侧配置注册使用了 `ModLoadingContext.get()`，当前能编译通过，但存在上游弃用警告；后续若 Forge API 再调整，需要一起收敛
- NeoForge 1.21.1 侧已有的 `EventBusSubscriber.Bus.MOD` 仍然会产生弃用警告，本阶段未顺手重构无关注册方式
- `menuSlotIndex` 目前主要用于保留菜单上下文，本阶段服务端仍主要依赖 `scope + logicalSlotIndex`
- 创造模式和其他特殊包装槽位因为映射风险较高，本阶段故意不支持
- 本地未进行 Minecraft 游戏内验证，无法确认与参考实现在所有 UI 边角行为上完全一致

## 人工测试清单

### Forge 1.20.1

- 单人存档启动
- 本地 Forge 服务端启动
- 客户端 + 服务端双端安装
- 主手潜影盒按快捷键打开，关闭保存正常
- 副手潜影盒按快捷键打开，关闭保存正常
- 背包界面悬停玩家背包潜影盒，按快捷键打开，关闭保存正常
- 背包界面悬停玩家背包潜影盒，右键打开，关闭保存正常
- 悬停非玩家背包槽位时不误打开
- 宿主被移走、替换、数量变为非 1 时不写回错误目标

### NeoForge 1.21.1

- 单人存档启动
- 本地 NeoForge 服务端启动
- 客户端 + 服务端双端安装
- 主手潜影盒按快捷键打开，关闭保存正常
- 副手潜影盒按快捷键打开，关闭保存正常
- 背包界面悬停玩家背包潜影盒，按快捷键打开，关闭保存正常
- 背包界面悬停玩家背包潜影盒，右键打开，关闭保存正常
- 悬停非玩家背包槽位时不误打开
- 宿主被移走、替换、数量变为非 1 时不写回错误目标

## 后续阶段 6B / 7 还需要做什么

阶段 6B 建议继续处理：

- `rightClickClose` 的完整返回背包语义
- reopen inventory S2C
- 更多 inventory 菜单与特殊 screen 槽位映射
- 更细的输入冲突处理

阶段 7 建议继续处理：

- 多人 / 专用服务端实测
- 创造模式特殊槽位策略
- 更多 quick-openable 类型
- 更完整的 UI 边界、异常关闭和防复制验证

## 许可证 / 来源说明

- 本阶段主要参考了 `references/quickshulker-1.20`
- `references/quickshulker-1.21.1`
- `references/quickshulker-26.1-neo`

本次没有直接修改 `references/`，也没有在仓库中复制整段第三方源码文件。

如果后续阶段继续大段移植参考实现，仍需人工检查 `THIRD_PARTY_NOTICES.md` 与来源说明是否需要更新。
