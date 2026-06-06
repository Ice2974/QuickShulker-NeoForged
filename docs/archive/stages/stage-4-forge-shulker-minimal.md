# 阶段 4：Forge 1.20.1 潜影盒最小闭环

> 历史开发记录：本文档记录早期阶段实现过程，不再作为当前实现状态的唯一依据。当前状态请以源码、README 和 `docs/release-0.1.0.md` 为准。


本阶段只在 `versions/forge-1.20.1` 中打通 `shulker_box` 的最小链路：

- 客户端按键触发 C2S 打开请求
- 服务端按 `HostSlotRef` 重新定位宿主槽位
- 服务端重新校验宿主物品类型和单堆叠约束
- 服务端从宿主 `ItemStack` 的 `BlockEntityTag` 读取潜影盒内容
- 服务端打开原版 `ShulkerBoxMenu`
- 菜单关闭时尝试把内容安全写回原宿主 `ItemStack`
- 宿主失效时关闭菜单并放弃写回

## 本阶段实际实现了什么

- Forge 1.20.1 最小 client key mapping：
  - 默认按键 `K`
  - 仅在无界面时尝试打开主手，其次副手
  - 仅支持数量为 `1` 的潜影盒
- Forge 1.20.1 C2S 打开请求：
  - 客户端只发送 `requestedTypeId + HostSlotRef + trigger`
  - 服务端不信任客户端物品内容
- Forge 1.20.1 `shulker_box` 注册与物品映射：
  - 所有原版颜色潜影盒都映射到 common 的 `shulker_box`
- Forge 1.20.1 NBT 版宿主内容读写：
  - 从 `BlockEntityTag` 读取 `Items`
  - 关闭时从实时菜单容器写回 `BlockEntityTag`
- Forge 1.20.1 最小会话校验：
  - 打开前校验当前宿主
  - 打开期间每 tick 重新校验
  - 宿主类型变化、堆叠数不为 `1`、宿主消失时关闭并丢弃改动
- Forge 1.20.1 关闭保存路径统一：
  - `Esc`、`E`、客户端正常关闭当前容器、客户端断开连接、服务端主动关闭，最终都汇入同一个服务端会话结束入口

## 本阶段没有实现什么

- NeoForge 1.21.1 的真实功能实现
- 背包界面悬停打开
- 背包界面右键打开
- 完整快捷键 / 配置界面
- 末影箱同步
- Bundle 独立菜单
- 工作台 / 切石机 / 铁砧快速打开
- 鼠标拖拽批量插入 / 提取
- reopen inventory S2C

## 使用到的阶段 3 抽象

- `QuickOpenableType` / `BuiltinQuickOpenables`
- `QuickOpenableRegistry`
- `QuickOpenRequest`
- `HostSlotRef`
- `HostItemSnapshot` / `HostItemReference`
- `HostItemValidator` / `DefaultHostItemValidator`
- `ContainerContentAccess`
- `MenuOpenIntent`
- `OpenSession`
- `OpenHostItemIntent`

## 潜影盒内容保存路径

1. 服务端收到打开请求后，按 `HostSlotRef` 重新从玩家背包取当前宿主 `ItemStack`
2. `ForgeShulkerContentAccess` 从宿主 `BlockEntityTag` 读取 27 格内容
3. 服务端用这些内容构造 `ItemBackedShulkerContainer`
4. 原版 `ShulkerBoxMenu` 对该容器进行交互
5. 菜单关闭后，若宿主仍通过校验，则把容器中的实时 `ItemStack` 列表写回原宿主 `BlockEntityTag`
6. 若宿主失效，则丢弃本次菜单中的修改

## 宿主失效时如何处理

- 每个服务器 tick 检查当前打开会话的宿主
- 若检测到：
  - 宿主槽位已空
  - 宿主类型变化
  - 宿主堆叠数不再为 `1`
- 则服务端关闭菜单
- 关闭时按服务端会话收尾逻辑走保守分支，放弃写回，避免把内容写到错误目标

## 阶段 4 关闭保存路径 bug 修复

### 问题现象

- `Esc` 关闭后，宿主潜影盒内容会保存
- `E` 关闭后，宿主潜影盒内容不保存
- 客户端异常结束进程时，单人世界中的这次菜单改动通常也不会保存
- 在后续排查中还发现另一类更严重的问题：
  - 某些情况下无论 `Esc` 还是 `E`
  - 只要关闭时 `ItemBackedShulkerContainer.dirty` 没有被可靠置位
  - 宿主潜影盒内容就会完全不变化

### 根因

- 阶段 4 初版把最终保存主要挂在 `ForgeShulkerMenu.removed()` 上
- 但并不是所有关闭方式都会稳定先经过这条路径
- 某些关闭方式会先让服务端当前 `containerMenu` 切回别的菜单，再由后续收尾逻辑处理
- 初版 `ForgeShulkerSessionManager.tick()` 在发现 `player.containerMenu != session.menu()` 时直接返回，没有把这类“菜单已切换但会话仍存在”的情况统一收口
- 同时，阶段 4 的 `finishSession()` 只有在 `session.container().isDirty()` 为真时，才会把 `OpenSession` 标记为 dirty
- 早期的通用会话规则曾把“未标记 dirty”的情况直接判成 `NO_CHANGES`
- 但 `ItemBackedShulkerContainer.dirty` 只是依赖 `setChanged()`，并不适合作为 item-backed shulker menu 是否需要最终写回的硬性前提
- 另外，空盒子保存场景还暴露出一个 Forge 1.20.1 NBT 细节：
  - 如果基于旧 `BlockEntityTag` 副本直接 `saveAllItems(...)`
  - 而保存前没有先清掉旧 `Items`
  - 那么“全部取空后关闭”的场景可能会把旧物品列表残留回宿主
- 结果就是：
  - `Esc` 关闭通常能走到 `removed() -> finishSession()`
  - `E` / 某些其他关闭路径则可能只发生菜单切换，会话悬空，导致没有进入最终保存逻辑
  - 即使已经进入最终保存逻辑，只要 dirty 没有可靠覆盖这次交互，也会被误判为 `NO_CHANGES`，最终完全不写回
  - 即使已经进入写回逻辑，如果清空保存前没有先移除旧 `Items`，也会出现“盒子取空后关闭，但宿主内容看起来完全没变”

### 修复后的关闭路径

- `Esc`：
  - 客户端请求关闭容器
  - 服务端关闭当前菜单
  - `ForgeShulkerMenu.removed()` 先进入统一服务端保存入口，再执行 `super.removed(player)`
- `E` / inventory key：
  - 若仍走正常容器关闭，和 `Esc` 一样进入统一保存入口
  - 若先发生菜单切换，服务器 tick 会检测到“当前菜单已不再是 quick shulker menu”，并主动调用同一个统一保存入口
- 客户端正常关闭当前容器：
  - 最终进入同一个服务端统一保存入口
- 客户端断开连接：
  - `PlayerLoggedOutEvent` 会调用同一个服务端统一保存入口
- 服务端主动关闭当前容器：
  - 仍然通过同一个服务端统一保存入口结束会话

### 修复后的最终保存规则

- 先由服务端重新校验宿主：
  - 宿主仍存在
  - 宿主仍是潜影盒
  - 宿主堆叠数仍为 `1`
- 若宿主有效，且关闭原因属于正常关闭类原因：
  - `PLAYER_CLOSED`
  - `PLAYER_DISCONNECTED`
  - 以及保留给后续阶段的其他正常结束原因
  - 则直接把 `session.container().copyContents()` 写回宿主 `ItemStack`
- 不再因为 `dirty=false` 就跳过正常保存
- 若宿主无效，或关闭原因属于：
  - `HOST_INVALIDATED`
  - `VALIDATION_REJECTED`
  - 则放弃写回，避免写到错误目标

### 关于“直接杀死 MC 进程”

- 这是异常终止，不属于“正常关闭路径”
- 如果进程被直接杀死，客户端和单人集成服务端都可能没有机会执行正常收尾
- 本次修复尽量覆盖“正常关闭容器”和“正常断开连接”
- 不能把“直接杀进程后必定保存”写成已保证行为

## 已知限制与风险

- `ContainerContentAccess` 的 common 快照模型目前不足以无损表达真实 `ItemStack` 数据；Forge 阶段 4 的写回使用了平台侧实时 `ItemStack` 容器作为保守补充。
- 本阶段未覆盖创造模式特例修正、背包界面槽位映射、多人并发场景和专用服务器实测。
- 当前没有 reopen inventory S2C；阶段 4 的最小按键入口不依赖该链路。
- 尚未做“打开后移动宿主到其他合法槽位再继续会话”的兼容逻辑；当前策略是直接关闭并放弃保存。
- 直接杀死 MC 进程属于异常终止，本阶段仍不能保证这类情况下的菜单改动一定落盘。
- `dirty` 仍然保留，但现在只适合用于 debug 日志或后续优化，不能再作为阶段 4 item-backed shulker 保存的硬性前提。

## 后续阶段仍需补充

### 阶段 5

- NeoForge 1.21.1 的同等 `shulker_box` 最小闭环
- DataComponent 版宿主内容读写

### 阶段 6

- 背包界面悬停 / 右键 / 按键入口
- 更完整的配置落地
- 工作台 / 切石机 / 铁砧等其他 quick-openable

### 阶段 7

- 多人 / 专用服务器验证
- 创造模式特例
- 更完整的宿主锁定、防复制和异常关闭处理

## 建议人工游戏内测试清单

- Forge 1.20.1 客户端启动，模组能加载
- 单人存档中主手持有 1 个空潜影盒，按 `K` 能打开
- 单人存档中副手持有 1 个空潜影盒，主手无可打开目标时按 `K` 能打开
- 向潜影盒中放入物品，关闭后再次打开，内容仍存在
- 从潜影盒中取出物品，关闭后再次打开，内容变化正确
- 从潜影盒中取出物品后按 `Esc` 关闭，再次打开，内容变化正确
- 从潜影盒中取出物品后按 `E` 关闭，再次打开，内容变化正确
- 打开空潜影盒，放入物品后按 `Esc` 关闭，再次打开，物品仍存在
- 打开空潜影盒，放入物品后按 `E` 关闭，再次打开，物品仍存在
- 打开有物品的潜影盒，全部取空后按 `Esc` 关闭，再次打开为空
- 打开有物品的潜影盒，全部取空后按 `E` 关闭，再次打开为空
- 从潜影盒中取出物品后，通过客户端正常返回标题或断开连接，再次进入世界后内容变化正确
- 主手或副手持有数量大于 1 的潜影盒时，服务端拒绝打开
- 打开后替换或移走宿主，不应把修改写回错误物品
- 创造模式下至少确认无明显复制或崩溃
- 专用服务器场景列为待测

## 待人工确认项

- Forge 47.x 最低兼容 loader 版本仍未确认
- 未进行 Minecraft 游戏内测试，无法确认行为与参考实现完全一致
- 未进行专用服务器 / 多人实测
- 若后续继续大段移植 `references/` 中实现，仍需人工检查许可证 / 来源说明是否需要更新
