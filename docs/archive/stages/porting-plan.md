# QuickShulker 移植计划（阶段 2）

> 历史开发记录：本文档记录早期阶段实现过程，不再作为当前实现状态的唯一依据。当前状态请以源码、README 和 `docs/release-0.1.0.md` 为准。


本文档基于以下参考源码目录的阅读结果整理：

- `references/quickshulker-1.20`
- `references/quickshulker-1.21.1`
- `references/quickshulker-26.1-neo`

目标：

- 盘点 QuickShulker 现有功能与玩家可见行为
- 以 `quickshulker-1.21.1` 作为现代行为基线
- 标注 Forge 1.20.1 与 NeoForge 1.21.1 的平台差异
- 为后续阶段实现提供可执行的拆分路线

说明：

- 本阶段只做源码阅读、差异分析和计划整理
- 本文档不代表当前仓库功能已经实现
- 若后续需要复制、改写或大段移植第三方实现，本文统一标注“许可证 / 来源说明待检查”

---

## 1. 参考源码阅读结论

### 1.1 行为基线选择

- 现代基线优先采用 `references/quickshulker-1.21.1`
- NeoForge 平台接入方式主要参考 `references/quickshulker-26.1-neo`
- Forge 1.20.1 应以 `references/quickshulker-1.20` 为版本地基，并尽量回灌 `1.21.1` 中已经稳定的行为修正

### 1.2 主要行为演进

相对 `1.20`，`1.21.1` / `26.1-neo` 明显新增或强化了以下内容：

- 末影箱内容的客户端同步与容器打开后增量同步
- 鼠标右键拖拽批量插入 / 取出
- 潜影盒对潜影盒的转移行为
- 铁砧快捷打开
- 更严格的“打开中的宿主物品仍然存在”校验
- 更完整的按键注册与配置界面入口

`26.1-neo` 还额外包含：

- Bundle 直接作为可打开容器的菜单与界面实现
- NeoForge 菜单类型注册与客户端 Screen 注册

`1.21.1` 未直接提供 Bundle 独立菜单，但保留了 bundle / shulker 的右键插入、提取、转移逻辑。

---

## 2. 功能清单与行为基线

下面按功能列出玩家可见行为、参考文件、推荐落点和风险。

### 2.1 背包内打开潜影盒

- 玩家可见行为：
  在手中、背包或创造背包对应玩家槽位中，对单个潜影盒触发快捷打开；打开后显示潜影盒内容，关闭后内容写回宿主物品；若宿主物品被移走或数量非法变化，界面会被强制关闭。
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/QuickShulkerMod.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/Util.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/QuickOpenableRegistry.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/QuickShulkerData.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ScreenMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ContainerMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/PlayerInventoryMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java`
- 适合放在 common 的逻辑：
  - 可打开物品注册表抽象
  - “是否允许打开”的业务判断
  - 当前打开宿主槽位跟踪接口
  - 宿主物品一致性校验策略
  - 菜单打开请求的通用语义
- Forge 1.20.1 平台侧需要实现：
  - C2S 打开请求包
  - ShulkerBox 菜单创建
  - `CompoundTag` / NBT 版内容读写适配
  - Screen / menu hook 与槽位拦截
- NeoForge 1.21.1 平台侧需要实现：
  - NeoForge payload 注册
  - ShulkerBoxMenu 打开
  - `DataComponent` 版内容读写适配
  - AbstractContainerMenu/Slot hook
- 是否涉及网络：是
- 是否涉及配置：是
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：是
- 风险等级：高
- 建议实现阶段：阶段 4 先打通 Forge，阶段 5 对齐 NeoForge
- 备注：
  宿主物品一致性与关闭保存链路直接关联物品复制 / 丢失风险。许可证 / 来源说明待检查。

### 2.2 背包内打开末影箱

- 玩家可见行为：
  背包中的末影箱可像潜影盒一样快捷打开，但实际读取的是玩家末影箱库存；在多人环境下，客户端需要收到服务端同步的末影箱内容，并在登录、重生、切维度、打开容器后保持一致。
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/QuickShulkerMod.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/QuickShulkerData.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/util/EnderChestSyncHandler.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/EnderChestS2CSyncPacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/event/EventListeners.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ContainerOpenMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/client/QuickShulkerModClient.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/event/EventListeners.java`
- 适合放在 common 的逻辑：
  - “末影箱作为特殊 quick-openable”的抽象语义
  - 末影箱打开请求语义
  - 同步触发条件枚举
- Forge 1.20.1 平台侧需要实现：
  - 打开末影箱菜单
  - 首次全量同步与打开后槽位增量同步
  - 登录 / 重生 / 切维度监听
- NeoForge 1.21.1 平台侧需要实现：
  - ChestMenu 打开
  - PlayerEvent / 容器监听 / payload 回写
  - `Container` API 对应的同步落点
- 是否涉及网络：是
- 是否涉及配置：是
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：否，核心差异更偏向同步与菜单
- 风险等级：高
- 建议实现阶段：阶段 4 完成基本链路，阶段 7 强化多人同步
- 备注：
  这是最容易出现“客户端看见的末影箱内容与服务端不一致”的功能。许可证 / 来源说明待检查。

### 2.3 快捷键打开

- 玩家可见行为：
  在没有 GUI 打开的情况下，按快捷键可打开主手或副手中的可打开物品；在容器界面中，按同一快捷键可打开鼠标悬停的物品；还存在一个额外快捷键可打开配置界面。
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/event/ModKeyCallback.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/event/KeyBindingRegister.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/client/QuickShulkerModClient.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ScreenMixin.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/event/KeyBindingRegister.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/client/QuickShulkerClient.java`
- 适合放在 common 的逻辑：
  - 输入触发后的业务判断
  - 主手 / 副手 / 当前悬停槽位的优先级规则
  - 配置项语义
- Forge 1.20.1 平台侧需要实现：
  - KeyMapping 注册
  - 客户端 tick 检测
  - 触发后向服务端发起打开请求
- NeoForge 1.21.1 平台侧需要实现：
  - NeoForge `RegisterKeyMappingsEvent`
  - 客户端 tick 事件接入
  - 配置界面注册
- 是否涉及网络：是
- 是否涉及配置：是
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：否
- 风险等级：中
- 建议实现阶段：阶段 6

### 2.4 鼠标悬停 / 右键 / 按键行为

- 玩家可见行为：
  - 在背包界面右键单个潜影盒 / 末影箱可直接打开
  - 在背包界面按快捷键可打开悬停物品
  - 可选启用“再次右键已打开宿主物品时关闭并返回玩家背包”
  - 鼠标位置会在打开后恢复，避免交互手感突变
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ScreenMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/client/ClientUtil.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/Util.java`
- 适合放在 common 的逻辑：
  - 触发条件判定
  - rightClickClose 语义
- Forge 1.20.1 平台侧需要实现：
  - `HandledScreen` 注入点
  - 鼠标坐标恢复
  - ScreenHandler 槽位到玩家背包槽位的映射
- NeoForge 1.21.1 平台侧需要实现：
  - 对应 Screen / menu 注入
  - 创造模式槽位映射处理
- 是否涉及网络：是
- 是否涉及配置：是
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：否
- 风险等级：中
- 建议实现阶段：阶段 6

### 2.5 配置项

- 玩家可见行为：
  配置决定哪些容器能快捷打开、是否允许右键 / 快捷键 / 拖拽、是否允许插入 / 提取 / 转移，以及配置界面的打开快捷键。
- 主要参考文件：
  - `references/quickshulker-1.20/src/main/java/net/kyrptonaught/quickshulker/config/ConfigOptions.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/config/ConfigOptions.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/config/ConfigOptions.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/config/ModConfigMenu.java`
- 适合放在 common 的逻辑：
  - 配置模型
  - 默认值
  - 配置项语义说明
  - 平台无关的读取接口
- Forge 1.20.1 平台侧需要实现：
  - 配置文件加载 / 保存
  - 客户端 keybind 显示绑定
  - 如需多人一致性，补充服务端生效边界
- NeoForge 1.21.1 平台侧需要实现：
  - 配置加载 / 保存
  - IConfigScreenFactory 或等价方案
  - 客户端按键与配置界面对接
- 是否涉及网络：间接涉及
- 是否涉及配置：是
- 是否涉及菜单 / Screen / Container：间接涉及
- 是否涉及 NBT / DataComponent 差异：否
- 风险等级：中
- 建议实现阶段：阶段 6
- 配置基线建议：
  以 `1.21.1` 配置项为主，Forge 1.20.1 回灌 `openSettingGui`、`supportsBundlingTransfer`、`supportsMouseDragged`、`quickAnvil`；`quickBundle` 是否回灌到 NeoForge 1.21.1 目标仓库需单独确认。

### 2.6 多人服务器可用性

- 玩家可见行为：
  双端安装时，客户端触发打开请求，服务端重新决定是否打开与保存；末影箱内容应保持服务端权威；容器关闭、死亡、换维度、重新登录后不应出现伪同步。
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/OpenInventoryPacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/EnderChestS2CSyncPacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/util/EnderChestSyncHandler.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/Util.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ContainerMixin.java`
- 适合放在 common 的逻辑：
  - 请求类型定义
  - 打开前校验规则
  - 打开中宿主校验规则
  - 保存与回退语义
- Forge 1.20.1 平台侧需要实现：
  - 服务端 packet handler
  - 打开前后合法性校验
  - 关闭时写回与客户端 reopen inventory 行为
- NeoForge 1.21.1 平台侧需要实现：
  - 同等 server-authoritative 链路
  - payload thread / enqueue 语义
  - 末影箱同步事件注册
- 是否涉及网络：是
- 是否涉及配置：是
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：是
- 风险等级：高
- 建议实现阶段：阶段 7

### 2.7 菜单 / Screen / Container 相关逻辑

- 玩家可见行为：
  潜影盒、末影箱、工作台、切石机、铁砧等会打开原版或近原版菜单；已打开的宿主槽位不可再被原交互直接点击破坏；某些容器会绕过原版“必须接触方块”的 `canUse` 检查。
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ContainerMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/CraftingScreenHandlerMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/AnvilScreenHandlerMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/ModScreenHandlerContext.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/api/ModContainerLevelAccess.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/gui/MenuTypes.java`
- 适合放在 common 的逻辑：
  - 宿主槽位标记接口
  - “虚拟上下文”的抽象需求
- Forge 1.20.1 平台侧需要实现：
  - ScreenHandler mixin / hook
  - `ScreenHandlerContext` 等价包装
  - 特殊菜单 `canUse` 放行
- NeoForge 1.21.1 平台侧需要实现：
  - AbstractContainerMenu / ContainerLevelAccess 包装
  - 必要时自定义 MenuType 注册
  - Bundle 独立菜单如要移植则需额外 GUI 链路
- 是否涉及网络：间接涉及
- 是否涉及配置：间接涉及
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：间接涉及
- 风险等级：高
- 建议实现阶段：阶段 4、5
- 备注：
  Bundle 独立菜单不是本仓库最小目标，但 `26.1-neo` 已有实现，可作为后续是否纳入范围的参考。许可证 / 来源说明待检查。

### 2.8 网络同步

- 玩家可见行为：
  客户端打开动作不会单机伪开，服务端会真正决定是否打开；某些关闭动作会把玩家界面切回背包；末影箱内容会进行 S2C 全量 / 增量同步；创造模式下某些 bundling 行为依赖额外包修正客户端视图。
- 主要参考文件：
  - `references/quickshulker-1.20/src/main/java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java`
  - `references/quickshulker-1.20/src/main/java/net/kyrptonaught/quickshulker/network/QuickBundlePacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/OpenInventoryPacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/QuickBundlePacket.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/EnderChestS2CSyncPacket.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/network/OpenShulkerPacket.java`
- 适合放在 common 的逻辑：
  - 包语义定义
  - 请求 / 同步方向抽象
  - 通用校验规则
- Forge 1.20.1 平台侧需要实现：
  - packet channel 与 codec
  - server execute / thread safety
  - 客户端接收后 reopen inventory
- NeoForge 1.21.1 平台侧需要实现：
  - payload registrar
  - handler thread 与 enqueue 约束
  - PacketDistributor / ClientPacketDistributor 适配
- 是否涉及网络：是
- 是否涉及配置：间接涉及
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：间接涉及
- 风险等级：高
- 建议实现阶段：阶段 4、5、7

### 2.9 容器数据读取与保存

- 玩家可见行为：
  打开的宿主物品内容应正确展示、修改并回写；关闭菜单、插入 / 提取、shift-click、拖拽后都应保存；宿主物品变化时应及时停止继续写入错误目标。
- 主要参考文件：
  - `references/quickshulker-1.20/src/main/java/net/kyrptonaught/quickshulker/BundleHelper.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/util/BundleHelper.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/Util.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/shulkerutils/ItemStackInventory.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/gui/screen/BundleContainer.java`
- 适合放在 common 的逻辑：
  - 宿主内容访问接口
  - 插入 / 提取 / 转移语义
  - 关闭保存触发点抽象
- Forge 1.20.1 平台侧需要实现：
  - NBT / `CompoundTag` 内容读写
  - 容器关闭回写
  - 物品计数与空槽逻辑
- NeoForge 1.21.1 平台侧需要实现：
  - `DataComponents.CONTAINER` / `BUNDLE_CONTENTS` 等组件读写
  - 容器回写与 `setChanged` 持久化
- 是否涉及网络：间接涉及
- 是否涉及配置：间接涉及
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：是
- 风险等级：高
- 建议实现阶段：阶段 3 先抽象，阶段 4 / 5 落地
- 备注：
  此处是最核心的平台差异面。许可证 / 来源说明待检查。

### 2.10 可能影响物品复制或丢失的逻辑

- 玩家可见行为：
  若实现不对，可能出现重复取出、关闭后未保存、宿主槽位叠堆、创造模式伪同步、末影箱客户端残影等严重问题。
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/api/Util.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/ContainerMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/PlayerInventoryMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/util/EnderChestSyncHandler.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/util/BundleHelper.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/QuickBundlePacket.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/mixin/ItemMixin.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/gui/screen/BundleItemMenu.java`
- 适合放在 common 的逻辑：
  - 宿主锁定规则
  - 关闭条件
  - 保存前校验
  - 只允许服务端权威写入的规则
- Forge 1.20.1 平台侧需要实现：
  - `PlayerInventory` 叠堆限制修正
  - ScreenHandler 点击取消
  - 创造模式特例处理
- NeoForge 1.21.1 平台侧需要实现：
  - 同类逻辑迁移到 NeoForge 菜单体系
  - `DataComponent` 回写一致性
  - Bundle 菜单如移植则要额外覆盖 quick-move / scroll / insert 逻辑
- 是否涉及网络：是
- 是否涉及配置：间接涉及
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：是
- 风险等级：高
- 建议实现阶段：阶段 7

### 2.11 额外可打开容器：工作台 / 切石机 / 铁砧 / Bundle

- 玩家可见行为：
  - 工作台、切石机可像快捷工具一样从背包或手持打开
  - `1.21.1` / `26.1-neo` 支持铁砧快捷打开，并保留耐久损耗逻辑
  - `26.1-neo` 支持 Bundle 独立菜单与滚动浏览
- 主要参考文件：
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/QuickShulkerMod.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/CraftingScreenHandlerMixin.java`
  - `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/mixin/AnvilScreenHandlerMixin.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/gui/screen/BundleItemMenu.java`
  - `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/gui/screen/BundleContainer.java`
- 适合放在 common 的逻辑：
  - 注册表级“可打开项类型”语义
  - 铁砧损耗语义
- Forge 1.20.1 平台侧需要实现：
  - 工具类菜单打开
  - 铁砧上下文包装
- NeoForge 1.21.1 平台侧需要实现：
  - 对应 Menu / Context 适配
  - 若要支持 Bundle 独立菜单，需注册自定义 MenuType 与客户端 Screen
- 是否涉及网络：是，取决于打开方式
- 是否涉及配置：是
- 是否涉及菜单 / Screen / Container：是
- 是否涉及 NBT / DataComponent 差异：Bundle 涉及
- 风险等级：中到高
- 建议实现阶段：阶段 6 补齐，Bundle 菜单若纳入则单独排期

---

## 3. Forge 1.20.1 与 NeoForge 1.21.1 差异点

### 3.1 NBT / CompoundTag 与 DataComponent 差异

- Forge 1.20.1 应按 NBT / `CompoundTag` 思路处理潜影盒等宿主物品内容。
- NeoForge 1.21.1 需要围绕 `DataComponent` / `DataComponents` 处理名称、Bundle 内容和容器内容。
- `1.21.1` 的比较逻辑从旧版 `ItemStack.areEqual` 过渡到更强调 components 的比较方法。
- 需要在 common 先定义统一的宿主内容访问接口，再让平台层分别实现 NBT 与 DataComponent 版读写。
- 风险：
  如果 common 直接依赖某一侧数据格式，后续保存与比较逻辑会迅速失控。

### 3.2 菜单 / Screen / Container API 差异

- Forge 1.20.1 参考的是 `ScreenHandler` / `HandledScreen` / `ScreenHandlerContext`。
- NeoForge 1.21.1 参考的是 `AbstractContainerMenu` / `AbstractContainerScreen` / `ContainerLevelAccess`。
- 菜单打开工厂、槽位索引名、`canUse` 覆盖、slot listener 接口都存在命名和签名差异。
- `26.1-neo` 还展示了 NeoForge 自定义 `MenuType` 注册方式。
- 建议：
  common 只定义“打开某类逻辑菜单”和“宿主槽位锁定”语义，平台层分别做 mixin / hook。

### 3.3 网络包注册与处理差异

- `1.20` 参考旧式 Fabric networking receiver。
- `1.21.1` 使用 `PayloadTypeRegistry` + `PacketCodec`。
- `26.1-neo` 使用 `RegisterPayloadHandlersEvent`、`PayloadRegistrar`、`StreamCodec`、`PacketDistributor`。
- NeoForge 侧还需要注意 handler thread 与 `enqueueWork` / client registration 的线程语义。
- 建议：
  common 定义“打开请求”“重新打开背包通知”“末影箱全量同步”“末影箱槽位同步”四类消息语义，平台模块各自实现 codec 与分发。

### 3.4 配置注册与同步差异

- 参考源码主要使用 kyrptconfig 自带配置体系，并未体现 Forge / NeoForge 原生 config API。
- Fabric / NeoForge 的配置界面接入方式不同，但配置项语义可保持一致。
- 本仓库后续可以保留 common 配置模型，在平台侧决定如何加载 / 暴露 UI。
- 风险：
  客户端配置与服务端权威行为的边界需明确，例如只影响输入行为的配置可客户端本地生效，影响多人一致性的行为开关应服务端重新校验。

### 3.5 按键注册差异

- Fabric 参考使用 `KeyBindingHelper`。
- NeoForge 参考使用 `RegisterKeyMappingsEvent` 与 `KeyMapping.Category`。
- 配置界面快捷键与主功能快捷键都需要注册。
- 建议：
  common 只保留 keybind 配置字段与触发语义，不要放任何平台注册代码。

### 3.6 客户端请求与服务端校验边界

- 客户端只负责发起“我想打开这个槽位”的请求，不应直接决定打开结果。
- 服务端应重新取玩家当前容器 / 当前槽位 / 当前物品，再决定是否打开。
- 打开后应持续监听宿主槽位是否仍保持可用。
- `rightClickClose` 会在服务端主动关闭后再通知客户端回到背包界面。
- 风险：
  如果只按客户端提交的 `slotId` 和 `ItemStack` 盲信处理，极易出现非法打开或复制问题。

### 3.7 多人环境下的数据保存与同步风险

- 末影箱：
  需要登录、重生、切维度、打开后增量同步，否则客户端缓存可能滞后。
- 潜影盒 / Bundle：
  关闭保存、宿主被移走强制关闭、创造模式包补丁都直接影响安全性。
- 铁砧：
  如果上下文不对，可能出现菜单能开但耐久消耗、破损进阶或音效不同步。
- 创造模式：
  参考源码专门为 creative inventory 做了额外包与槽位映射修正。

---

## 4. 参考源码之间的不一致与处理建议

### 4.1 行为不一致点

- `1.20` 没有 `1.21.1` 那样完整的末影箱同步链路。
- `1.20` 没有 `supportsBundlingTransfer`、`supportsMouseDragged`、`quickAnvil`、`openSettingGui`。
- `26.1-neo` 有 `quickBundle` 与独立 Bundle 菜单；`1.21.1` 配置中未暴露该开关。
- `Util.forceCloseScreenIfNotPresent` 在 `1.21.1` / `26.1-neo` 比 `1.20` 更偏向按“物品类型 + 单栈限制”判断，而不完全按旧版精确相等判断。

### 4.2 处理建议

- 现代行为基线按 `1.21.1` 处理。
- Forge 1.20.1 应尽量回灌以下行为或 bugfix：
  - 更完整的末影箱同步链路
  - `openSettingGui`
  - `supportsBundlingTransfer`
  - `supportsMouseDragged`
  - `quickAnvil`
  - 更稳妥的宿主存在性校验
- `quickBundle` 与 Bundle 独立菜单不列入当前最小必做范围，除非维护者确认要把 `26.1-neo` 的行为一起纳入本仓库目标。

---

## 5. 后续阶段建议

### 5.1 阶段 3：common 层基础逻辑与平台抽象接口

- 目标：
  建立平台无关的 quick-openable 注册、配置模型、宿主内容访问抽象、菜单打开语义和网络语义。
- 修改范围：
  - `common`
  - 必要时少量 `versions/*` 占位接口接线
  - `docs/`
- 不应该碰的范围：
  - `references/`
  - 大规模平台实现细节
  - README / 发布说明
- 主要风险：
  - 抽象过度导致实现困难
  - 抽象不足导致 common 混入平台 API
- 建议验证方式：
  - 编译检查
  - 接口边界人工审查
  - 对照本文档确认功能覆盖面
- 完成标准：
  - common 不依赖 Forge / NeoForge API
  - 已定义宿主内容访问、网络语义、配置模型、菜单打开抽象
  - 能支撑阶段 4 / 5 接入

### 5.2 阶段 4：Forge 1.20.1 首条功能链路跑通

- 目标：
  先在 Forge 1.20.1 打通“单个潜影盒从客户端触发到服务端打开并保存”的最小链路，再补末影箱基础打开。
- 修改范围：
  - `versions/forge-1.20.1`
  - `common`
  - 必要文档
- 不应该碰的范围：
  - NeoForge 平台实现
  - 非最小链路的 UI 增强
  - 无关构建结构
- 主要风险：
  - NBT 读写与菜单保存不一致
  - 宿主槽位锁定不完整
  - 末影箱首次同步缺失
- 建议验证方式：
  - `.\gradlew.bat :versions:forge-1.20.1:build`
  - 人工代码检查打开 / 关闭 / 保存链路
  - 生成游戏内测试清单但不声称已实测
- 完成标准：
  - 潜影盒基础打开链路完整
  - 末影箱基础打开方案明确并能编译
  - 没有 common 越界依赖

### 5.3 阶段 5：NeoForge 1.21.1 对齐功能链路

- 目标：
  将阶段 4 的核心链路迁移到 NeoForge 1.21.1，并对齐 `DataComponent` 版内容访问和 NeoForge payload 注册。
- 修改范围：
  - `versions/neoforge-1.21.1`
  - `common`
  - 必要文档
- 不应该碰的范围：
  - 与最小链路无关的额外功能
  - references 内容
- 主要风险：
  - DataComponent 内容回写错误
  - Menu / payload API 适配失真
  - 末影箱 S2C 同步线程边界错误
- 建议验证方式：
  - `.\gradlew.bat :versions:neoforge-1.21.1:build`
  - 编译与代码路径审查
- 完成标准：
  - NeoForge 可编译
  - 潜影盒与末影箱基础链路对齐
  - 与 `1.21.1` 基线相比无明显行为倒退

### 5.4 阶段 6：配置、快捷键、鼠标行为补全

- 目标：
  补齐 `1.21.1` 基线的配置项、快捷键、右键打开、悬停打开、rightClickClose、鼠标拖拽、铁砧等交互层功能。
- 修改范围：
  - `common` 配置语义
  - 两个平台的 client hook / keybinding / menu hook
  - `docs/`
- 不应该碰的范围：
  - 无关的玩法扩展
  - README 大改
- 主要风险：
  - 创造模式槽位映射错误
  - 输入事件与原版点击冲突
  - 鼠标拖拽导致重复插入 / 提取
- 建议验证方式：
  - 编译检查
  - 人工测试清单更新
  - 对照参考源码逐项核对配置项默认值
- 完成标准：
  - 配置项覆盖 `1.21.1` 基线
  - 快捷键与右键行为在两平台均接通
  - 鼠标拖拽与铁砧逻辑代码完整

### 5.5 阶段 7：多人同步与数据安全验证

- 目标：
  强化服务端校验、末影箱同步、关闭保存和创造模式特例，集中处理所有复制 / 丢失风险。
- 修改范围：
  - 两个平台网络层
  - 宿主锁定与监听逻辑
  - 末影箱同步逻辑
  - `docs/`
- 不应该碰的范围：
  - 无关重构
  - 发布元数据
- 主要风险：
  - 服务端与客户端状态不一致
  - 创造模式包修复不足
  - 玩家死亡 / 掉线 / 换维度后保存错误
- 建议验证方式：
  - 两个平台构建
  - 完整人工测试清单
  - 针对高风险点添加必要 debug 日志
- 完成标准：
  - 高风险数据链路都有明确保护
  - 文档中列出的多人同步场景均有代码覆盖点
  - 待人工游戏内测试项清晰

### 5.6 阶段 8：README / 发布说明 / NOTICE 检查

- 目标：
  在功能基本稳定后，再补 README 当前状态、安装说明、已知限制，并检查来源说明是否需要更新。
- 修改范围：
  - `README.md`
  - `README_en.md`
  - 如确有必要，再人工检查 `THIRD_PARTY_NOTICES.md`
  - `docs/`
- 不应该碰的范围：
  - 无关源码
  - references
- 主要风险：
  - 文档写得比实现更超前
  - 来源说明遗漏第三方移植片段
- 建议验证方式：
  - 人工核对文档与源码一致性
  - 对照本文件中的“许可证 / 来源说明待检查”标记
- 完成标准：
  - README 只描述已实现能力
  - 来源说明待检查项被维护者逐一确认

---

## 6. 建议的实现优先级

推荐下一阶段先从“背包内打开潜影盒”的最小闭环开始：

- 它是所有 quick-open 逻辑的基础链路
- 它同时覆盖网络请求、菜单打开、宿主槽位锁定、关闭保存四个核心问题
- 这条链路打通后，末影箱、工作台、切石机、铁砧都能复用同一套抽象

建议顺序：

1. common 抽象宿主内容访问与 quick-open 注册
2. Forge 1.20.1 先打通潜影盒
3. Forge 1.20.1 接入末影箱同步
4. NeoForge 1.21.1 对齐潜影盒与末影箱
5. 再补按键、右键、拖拽、铁砧等交互

---

## 7. 当前识别到的高风险点

- 宿主物品在打开期间被移动、合并、替换后的强制关闭与保存边界
- 末影箱客户端缓存与服务端真实内容的同步时机
- 创造模式下的特殊槽位映射与 bundling 包修正
- `CompoundTag` 与 `DataComponent` 双实现长期一致性
- 铁砧快捷打开后的耐久损耗与破损升级逻辑
- 鼠标拖拽批量插入 / 取出导致的重复操作
- Bundle 独立菜单若后续纳入范围，其滚动、quick-move 与 `DataComponents.BUNDLE_CONTENTS` 回写都属于额外高风险面

---

## 8. 待人工确认项

- 是否将 `26.1-neo` 中的 `quickBundle` 与 Bundle 独立菜单视为本仓库必移植范围
- Forge 47.x 与 NeoForge 21.x 的最低兼容 loader 版本仍待后续构建 / 运行验证
- 本阶段未进行 Minecraft 游戏内测试，无法确认行为与参考实现完全一致
- 本阶段未进行多人服务器测试，无法确认所有同步细节在真实环境下无偏差
- 若后续复制、改写或大段移植 `references/` 中实现，许可证 / 来源说明是否需要更新仍待人工确认

---

## 9. 与当前仓库状态的一致性检查

- 与 `bootstrap-skeleton.md` 一致：
  当前仓库仍处于“仅有多模块骨架、尚未实现 gameplay features / menus / networking / configs / keybindings”的状态。
- 与 `AGENTS.md` 一致：
  本文档将 `1.21.1` 作为现代行为基线，同时保留 Forge 1.20.1 的版本地基与回灌策略。
- 当前未发现必须立即修改 Gradle 骨架的明确错误。
