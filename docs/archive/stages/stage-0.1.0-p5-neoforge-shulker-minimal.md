# 阶段 5：NeoForge 1.21.1 `shulker_box` 最小闭环

本阶段只在 `versions/neoforge-1.21.1` 中打通 `shulker_box` 的最小链路：

- 客户端通过最小 key mapping 请求打开主手或副手中的单个潜影盒
- 服务端按 `HostSlotRef` 重新定位宿主槽位
- 服务端重新校验当前宿主 `ItemStack`
- 服务端从 `DataComponents.CONTAINER` 读取 27 格潜影盒内容
- 服务端打开原版 `ShulkerBoxMenu`
- 关闭菜单时统一走服务端保存入口，把实时容器内容写回宿主物品
- 宿主失效时丢弃修改，避免写回到错误目标

## 本阶段实现了什么

- NeoForge 1.21.1 `shulker_box` 注册与物品映射：
  - 所有原版颜色潜影盒都映射到 common 的 `shulker_box`
- 最小客户端触发：
  - 新增最小 `K` 键位
  - 仅支持主手 / 副手持有的单个潜影盒
- 最小 C2S 网络：
  - 客户端只发送 `requestedTypeId + HostSlotRef + trigger`
  - 服务端决定是否允许打开
- 最小服务端会话管理：
  - 打开时记录 `OpenSession`
  - 每 tick 校验宿主是否仍然有效
  - `removed()` / 菜单切换 / 登出 / 重生 / 切维度统一收口到同一保存入口
- NeoForge 1.21.1 的 `DataComponents.CONTAINER` 读写：
  - 读取 27 格内容构造 item-backed 容器
  - 关闭时把容器当前内容写回 `DataComponents.CONTAINER`

## 本阶段没有实现什么

- 末影箱同步
- 背包界面悬停打开
- 背包界面右键打开
- 完整快捷键配置界面
- Bundle 独立菜单
- 工作台 / 切石机 / 铁砧
- 鼠标拖拽批量行为
- inventory 内任意槽位打开入口

## 复用了哪些阶段 3 common 抽象

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

## 对齐阶段 4 Forge 的行为

- 客户端不发送宿主内容，只发送打开意图和槽位引用
- 服务端收到请求后重新从玩家当前状态取真实宿主物品
- 只允许单个潜影盒作为宿主
- 用原版 `ShulkerBoxMenu` 打开菜单，而不是自定义玩家可见交互
- 菜单关闭统一走服务端保存入口，不依赖客户端判断是否成功打开
- `dirty` 只保留为调试信息，不作为是否保存的硬性前提
- 宿主失效时直接丢弃修改，避免误写回

## DataComponent 读取 / 写回路径

1. `NeoForgeQuickOpenHandler` 根据 `HostSlotRef` 重新定位宿主 `ItemStack`
2. `NeoForgeShulkerContentAccess.readItemStacks()` 从 `DataComponents.CONTAINER` 拷贝出 27 格内容
3. `ItemBackedShulkerContainer` 用这些内容创建实时容器副本，供 `ShulkerBoxMenu` 交互
4. 关闭时 `NeoForgeShulkerSessionManager.finishSession()` 在服务端重新校验宿主
5. 若宿主仍有效，则 `NeoForgeShulkerContentAccess.writeItemStacks()` 把容器当前内容写回 `DataComponents.CONTAINER`

## 关闭保存路径

- `NeoForgeShulkerMenu.removed()` 会在 `super.removed(player)` 之前进入统一保存入口
- 如果先发生菜单切换，`NeoForgeShulkerSessionManager.tick()` 会发现 `player.containerMenu != session.menu()`，并主动调用同一个保存入口
- 登出 / 重生 / 切维度也会调用同一个保存入口
- 保存时不以 `dirty` 为硬条件；只要是正常关闭且宿主有效，就直接写回当前容器内容

## 宿主失效时如何处理

服务端每 tick 都会重新校验宿主：

- 槽位物品消失
- 物品类型变化
- 数量不再是 1

一旦失效：

- 当前 quick shulker 菜单会被标记为失效并关闭
- 会话关闭原因改为 `HOST_INVALIDATED`
- 最终保存分支走 `DISCARD_CHANGES`
- 不把容器内容写回当前槽位

## 已知风险

- 当前 `HostItemSnapshot` 的 NeoForge 指纹使用 `ItemStack.getComponents().toString()`，足以支撑阶段 5 的最小校验链路，但还不适合作为长期稳定的跨版本精确序列化指纹
- `DataComponents.CONTAINER` 在“空潜影盒是否保留空组件”上的最终游戏内表现尚未实测
- 当前阶段 5 只覆盖手持入口，尚未验证未来 inventory 槽位入口复用时的 `HostSlotRef` 边界

## 阶段 6 / 7 需要继续补什么

- 背包界面悬停打开
- 背包界面右键打开
- inventory 槽位入口与 `menuSlotIndex` 映射
- 完整快捷键 / 配置接入
- 末影箱链路
- 更多 quick openable 类型
- 更稳健的 NeoForge `HostItemSnapshot` 指纹策略

## 人工游戏内测试清单

### NeoForge 1.21.1

- 单人存档启动
- 本地 NeoForge 服务端启动
- 客户端 + 服务端双端安装
- 主手持普通潜影盒，按 `K` 打开
- 主手持染色潜影盒，按 `K` 打开
- 副手持潜影盒，按 `K` 打开
- 菜单内增删物品后按 `Esc` 关闭，重新打开确认保存
- 菜单内增删物品后按 `E` 关闭，重新打开确认保存
- 打开后把宿主物品移动、替换或堆叠，确认菜单被关闭且不会把修改写回错误目标
- 打开后死亡 / 掉线 / 切维度，确认不会写回错误目标

## 待人工确认项

- `DataComponents.CONTAINER` 对空潜影盒写回后的最终原版表现
- 最小 key mapping 在实机 NeoForge 1.21.1 客户端中的输入体验
- 当前宿主失效判定在多人环境下是否还需要额外边界处理
