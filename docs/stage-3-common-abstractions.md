# 阶段 3：common 层基础抽象设计

本文档记录阶段 3 在 `common` 模块新增的平台无关模型、接口与安全语义。本文档只描述抽象，不代表 Forge 1.20.1 或 NeoForge 1.21.1 已经接入真实菜单、按键、网络包或容器保存链路。

## 1. 本阶段新增的 common 抽象

### 1.1 quick-openable 类型与注册语义

新增：

- `common/.../open/QuickOpenableType.java`
- `common/.../open/QuickOpenableRegistry.java`
- `common/.../open/BuiltinQuickOpenables.java`
- `common/.../open/QuickOpenConfigGate.java`
- `common/.../open/QuickOpenableCategory.java`
- `common/.../open/QuickOpenMenuKind.java`

作用：

- 用纯 Java 模型表达“一个物品类型在逻辑上可被 QuickShulker 快速打开”
- 把“打开哪种菜单”“走哪个配置开关”“是否要求宿主堆叠数必须为 1”“打开期间是否锁定宿主槽位”“关闭后是否回到玩家背包”这些语义从平台实现中提前抽出来
- `QuickOpenableRegistry` 只负责：
  - 注册逻辑类型
  - 绑定平台未来传入的 `itemKey -> typeId`
  - 查询当前物品逻辑上属于哪种 quick-openable

当前内建逻辑类型：

- `shulker_box`
- `ender_chest`
- `crafting_table`
- `stonecutter`
- `anvil`

说明：

- `bundle` 仅在 `QuickOpenableCategory` 中预留扩展空间，没有作为当前阶段必须实现或默认注册的能力
- 这样可以满足 `docs/porting-plan.md` 中“quickBundle / Bundle 独立菜单待人工确认”的范围要求

对应阶段 2 需求：

- 背包内打开潜影盒
- 背包内打开末影箱
- 工具类容器打开语义
- 配置项对不同 quick-openable 的开关控制

### 1.2 打开请求与宿主槽位定位

新增：

- `common/.../open/QuickOpenRequest.java`
- `common/.../open/QuickOpenTrigger.java`
- `common/.../open/HostSlotRef.java`
- `common/.../open/HostStorageScope.java`

作用：

- 统一描述“客户端或平台 hook 想打开哪个宿主物品”
- 不在 common 里绑定 Minecraft 某个具体 `Slot`、`ItemStack`、`ScreenHandler` 或 `AbstractContainerMenu`
- `HostSlotRef` 只表达：
  - 宿主位于哪个逻辑存储域
  - 玩家背包逻辑索引
  - 当前菜单中的槽位索引

这样阶段 4/5 平台实现可以分别把：

- Forge 1.20.1 的 `ScreenHandler` / player inventory 槽位
- NeoForge 1.21.1 的 `AbstractContainerMenu` / player inventory 槽位

映射到同一套 common 语义。

对应阶段 2 需求：

- 快捷键打开
- 鼠标悬停 / 右键 / 按键行为
- 客户端只提交“我想打开哪个槽位”，服务端重新校验

### 1.3 宿主物品一致性校验语义

新增：

- `common/.../open/HostItemSnapshot.java`
- `common/.../open/HostItemReference.java`
- `common/.../open/HostValidationMode.java`
- `common/.../open/HostValidationFailure.java`
- `common/.../open/HostValidationResult.java`
- `common/.../open/HostItemValidator.java`
- `common/.../open/DefaultHostItemValidator.java`

作用：

- 把“打开前重新校验”和“打开期间宿主是否仍然有效”的判断提前建模
- common 不直接比较真实 `ItemStack`、NBT 或 DataComponent，而是比较平台侧生成的快照：
  - `itemKey`
  - `count`
  - `contentFingerprint`

当前内置校验模式：

- `EXACT`
- `SAME_ITEM_TYPE`
- `SAME_ITEM_TYPE_AND_SINGLE_COUNT`

这对应 `references/quickshulker-1.21.1` 中已经存在的两类关键行为：

- 打开前由服务端重新判断宿主是否合法
- 打开后若宿主被移走、替换、合并或数量异常变化，则应强制结束当前会话，避免继续向错误目标写回

对应阶段 2 需求：

- 潜影盒 / 末影箱打开前重新校验
- 打开期间宿主槽位仍然存在
- 避免物品复制 / 丢失

### 1.4 容器内容读取与写回抽象

新增：

- `common/.../content/ContainerContentAccess.java`
- `common/.../content/ContainerContentSnapshot.java`
- `common/.../content/ContainerSlotSnapshot.java`
- `common/.../content/ContentWriteResult.java`

作用：

- 用接口表达“平台如何从宿主物品或末影箱读取内容”和“如何把修改后的内容安全写回”
- common 只认识通用快照，不认识：
  - Forge 1.20.1 的 NBT / `CompoundTag`
  - NeoForge 1.21.1 的 DataComponent

这满足 AGENTS.md 对 `common` 边界的要求，也为阶段 4 / 5 平台侧分别实现 NBT 与 DataComponent 适配层留出了位置。

对应阶段 2 需求：

- 容器内容读取 / 写回
- 关闭菜单后物品保存
- 末影箱内容同步前后的统一数据表示

### 1.5 配置模型与默认值

新增：

- `common/.../config/QuickShulkerConfig.java`
- `common/.../config/QuickShulkerConfigView.java`
- `common/.../config/KeyBindingSpec.java`

作用：

- 把配置语义从平台配置系统中抽离
- 默认值优先对齐 `docs/porting-plan.md` 中的 1.21.1 行为基线

当前已覆盖的配置语义：

- 是否允许手持右键打开
- 是否允许快捷键打开手持物品
- 是否允许在背包界面用快捷键打开悬停物品
- 是否允许在背包中右键打开
- 是否允许对已打开宿主右键关闭并返回背包
- 是否允许 bundling insert / pickup / transfer / extract
- 是否允许鼠标拖拽批量行为
- 是否允许潜影盒快速打开
- 是否允许末影箱快速打开
- 是否允许工作台快速打开
- 是否允许切石机快速打开
- 是否允许铁砧快速打开
- 是否允许打开设置界面的快捷键

未直接纳入本阶段必做：

- `quickBundle` 独立菜单配置

处理方式：

- 保留扩展空间
- 不把它写成当前内建必实现功能

对应阶段 2 需求：

- 配置项迁移
- 快捷键 / 右键行为开关
- 1.21.1 配置基线对齐

### 1.6 网络消息语义

新增：

- `common/.../network/NetworkIntent.java`
- `common/.../network/NetworkDirection.java`
- `common/.../network/OpenHostItemIntent.java`
- `common/.../network/ReopenPlayerInventoryIntent.java`
- `common/.../network/EnderChestFullSyncIntent.java`
- `common/.../network/EnderChestSlotSyncIntent.java`

作用：

- 只定义消息方向与语义，不定义平台注册、codec、payload registrar 或 packet handler

当前已建模的消息：

- `C2S open_host_item`
  - 客户端请求服务端尝试打开某个宿主物品
- `S2C reopen_player_inventory`
  - 服务端要求客户端回到玩家背包
- `S2C ender_chest_full_sync`
  - 服务端发送末影箱全量内容
- `S2C ender_chest_slot_sync`
  - 服务端发送末影箱单槽位增量内容

对应阶段 2 需求：

- C2S 打开宿主物品请求
- S2C 重新打开玩家背包通知
- S2C 末影箱全量同步
- S2C 末影箱增量同步

### 1.7 菜单打开语义与打开会话安全状态

新增：

- `common/.../session/MenuOpenIntent.java`
- `common/.../session/OpenSession.java`
- `common/.../session/OpenSessionState.java`
- `common/.../session/OpenSessionSafetyPolicy.java`
- `common/.../session/CloseReason.java`
- `common/.../session/SaveDisposition.java`
- `common/.../session/OpenSessionRules.java`

作用：

- 把“打开某种菜单”“是否锁定宿主槽位”“关闭时是否保存或放弃保存”的语义提前固定下来
- 用 `OpenSession` 表达一个打开生命周期，而不是只表达一次瞬时点击

当前会话语义覆盖：

- 打开前可要求重新校验
- 打开期间可要求持续校验
- 宿主槽位可被标记为锁定中
- 若宿主失效，可丢弃脏改动
- 若是主动回到玩家背包，可由平台决定发送 reopen inventory

`OpenSessionRules` 当前给出的默认边界：

- 未产生改动时：`NO_CHANGES`
- 宿主失效或校验拒绝时：`DISCARD_CHANGES`
- 其他正常关闭：`SAVE_TO_HOST`

对应阶段 2 需求：

- 数据安全相关状态
- 打开中的宿主槽位锁定
- 关闭时保存或放弃保存的边界

## 2. 哪些内容留给 Forge 1.20.1 平台实现

以下内容本阶段未做，明确留给 `versions/forge-1.20.1`：

- `itemKey` 与真实 Forge 物品 / BlockItem 的映射
- `HostSlotRef` 与 Forge 1.20.1 `ScreenHandler` / player inventory / creative inventory 槽位的互转
- `ContainerContentAccess` 的 NBT / `CompoundTag` 实现
- `OpenHostItemIntent` 的真实 packet 注册与处理
- `ReopenPlayerInventoryIntent`、末影箱全量 / 增量同步包的真实注册
- 潜影盒 / 末影箱 / 工作台 / 切石机 / 铁砧菜单实际打开
- 打开期间对宿主槽位的 Forge mixin / hook 锁定与强制关闭

## 3. 哪些内容留给 NeoForge 1.21.1 平台实现

以下内容本阶段未做，明确留给 `versions/neoforge-1.21.1`：

- `itemKey` 与 NeoForge 1.21.1 物品注册表键的映射
- `HostSlotRef` 与 `AbstractContainerMenu` / player inventory / creative inventory 槽位的互转
- `ContainerContentAccess` 的 DataComponent 实现
- payload registrar / codec / handler 注册
- `S2C ender chest` 同步包的真实发送与接收
- 菜单打开与 `ContainerLevelAccess` / `canUse` 相关接入
- 打开期间宿主槽位锁定与强制关闭的 NeoForge hook

## 4. 当前仍未解决的风险

- `contentFingerprint` 具体如何在 Forge 1.20.1 与 NeoForge 1.21.1 保持长期可维护的一致语义，仍待阶段 4 / 5 验证
- `HostSlotRef` 对创造模式特殊槽位、打开中的容器槽位映射是否足够稳定，仍待阶段 4 的第一条功能链路验证
- 末影箱的“客户端缓存视图”与“服务端权威内容”之间，何时做全量同步、何时做增量同步，仍需阶段 7 结合真实平台代码收敛
- `rightClickClose` 在不同平台菜单生命周期中的表现是否需要细化额外状态，仍待阶段 6 接 UI / 输入层后确认
- 若后续需要大段移植 `references/` 中的现成实现，许可证 / 来源说明仍需人工检查

## 5. 阶段 4 建议从哪个最小闭环开始

推荐最小闭环：

- Forge 1.20.1
- 单一宿主类型先只做 `shulker_box`
- 只打通：
  - 客户端发出 `OpenHostItemIntent`
  - 服务端根据 `QuickOpenRequest + HostSlotRef` 重新取宿主
  - 服务端做 `DefaultHostItemValidator` 风格的一致性校验
  - 服务端打开最基础的潜影盒菜单
  - 关闭菜单时通过 `ContainerContentAccess` 的 Forge NBT 实现写回
  - 宿主失效时强制关闭并放弃保存

原因：

- 这条链路最集中覆盖阶段 3 新抽象的核心价值
- 一旦这条闭环成立，末影箱同步、右键 / 快捷键触发、Anvil / Stonecutter 等都能在同一套语义上继续扩展

## 6. 许可证 / 来源说明待检查

- 本阶段没有直接复制 `references/` 下的大段实现，只参考了命名与行为语义
- 若后续阶段 4 / 5 / 7 为了平台接入而移植或改写较大段第三方实现，仍需要人工检查 `THIRD_PARTY_NOTICES.md` 与来源说明是否需要更新
