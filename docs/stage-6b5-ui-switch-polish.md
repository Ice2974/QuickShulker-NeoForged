# 阶段 6B.5：quick-open 容器切换 UI polish

本阶段只处理两个体验问题：

- QuickShulker 容器之间切换打开时，鼠标指针回到屏幕中心
- 调查潜影盒与末影箱界面布局差异是否需要按原作对齐

本阶段不修改：

- `references/`
- `shulker_box` 内容保存逻辑
- `ender_chest` 内容保存逻辑
- 宿主锁定逻辑
- active session 切换逻辑本身
- Bundle
- 工作台 / 切石机 / 铁砧
- 鼠标拖拽批量行为
- README / 发布说明

## 问题原因

当前双平台已经允许在一个 QuickShulker 容器界面里继续打开背包中的另一个潜影盒 / 末影箱。

但当服务端为新宿主重新打开菜单时，客户端会创建新的 vanilla container screen。对于这类 screen 重建，Minecraft 默认不会替我们保留切换前的绝对鼠标位置，因此鼠标会落回新界面的默认位置，看起来像“跳回屏幕中心”。

`references/quickshulker-1.20`、`references/quickshulker-1.21.1` 和 `references/quickshulker-26.1-neo` 都已经显式处理了这个问题：在发送 quick-open 请求前记录当前鼠标绝对坐标，并在新容器 screen 初始化后恢复坐标。

因此，本仓库当前问题不是服务端保存 / 会话收尾回退，而是客户端缺少对应的 UI 状态传递。

## 修复策略

本阶段采用“只对 QuickShulker 发起的 quick-open 切换生效”的最小修复：

1. 客户端在发送 `OpenHostItemIntent` 前检查当前 `minecraft.screen`
2. 只有当前 screen 是 `AbstractContainerScreen` 时，才记录：
   - 当前绝对鼠标坐标
   - 当前 source screen 对象
3. 进入客户端下一帧 / 后续 tick 后，只要检测到：
   - 当前 screen 已经不是旧的 source screen
   - 当前 screen 是 `AbstractContainerScreen`
   - 当前 menu 是 `ForgeQuickOpenMenu` / `NeoForgeQuickOpenMenu`
4. 立刻调用 GLFW 恢复鼠标位置
5. 恢复成功后清空待恢复状态；如果数十 tick 内没有打开目标 QuickShulker 菜单，也自动过期清空

这样可以保证：

- 只作用于本模组发起的 quick-open 菜单切换
- 不会全局修改所有 screen 切换行为
- 不需要改服务端 session / 保存逻辑
- 普通背包打开、普通关闭菜单、普通原版容器交互不受影响

## Forge 1.20.1 实现

实现文件：

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickOpenMouseRestore.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`

实现方式：

- `ForgeQuickShulkerClient.sendIntent(...)` 在真正发包前调用 `ForgeQuickOpenMouseRestore.capture(minecraft.screen)`
- `capture(...)` 只接受当前为 `AbstractContainerScreen` 的情况
- `onClientTick(...)` 在原有 held keybind 处理前，先执行 `ForgeQuickOpenMouseRestore.onClientTick()`
- 当新 screen 已经切换为 `ForgeQuickOpenMenu` 时，通过 `GLFW.glfwSetCursorPos(...)` 恢复坐标

作用范围：

- 背包 / 容器界面内右键 quick-open
- 背包 / 容器界面内快捷键 quick-open
- 已打开 QuickShulker 菜单后，再切换打开另一个 QuickShulker 菜单

不会额外处理：

- 无界面状态下的普通 screen 切换
- 非 QuickShulker 菜单
- 服务端拒绝请求后的其他界面行为

## NeoForge 1.21.1 实现

实现文件：

- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickOpenMouseRestore.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`

实现方式与 Forge 保持一致：

- 发包前记录 source screen 与绝对鼠标坐标
- 在 `ClientTickEvent.Post` 中检查待恢复状态
- 仅当新 screen 已切换为 `NeoForgeQuickOpenMenu` 时恢复鼠标位置
- 成功恢复或等待超时后清空状态

这样做避免了依赖 NeoForge / Forge 不同的 screen init 事件差异，逻辑都收敛在客户端 tick 上，双平台行为更容易保持一致。

## UI 布局差异调查

### references/ 原作行为

对比参考实现可确认：

- 潜影盒：原作使用 vanilla `ShulkerBoxMenu`
- 末影箱：原作使用 vanilla `GenericContainerScreenHandler.createGeneric9x3(...)` / `ChestMenu.threeRows(...)`

对应参考位置：

- `references/quickshulker-1.20/src/main/java/net/kyrptonaught/quickshulker/QuickShulkerMod.java`
- `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/QuickShulkerMod.java`
- `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/QuickShulker.java`

结论：

- 原作并没有把末影箱强行做成与潜影盒完全相同的 screen 布局
- 潜影盒与末影箱存在 vanilla screen 纹理、尺寸和“物品栏”标签位置差异，是原作就存在的 UI 差异
- 当前仓库潜影盒走 `ShulkerBoxMenu`、末影箱走 `ChestMenu/MenuType.GENERIC_9x3`，与原作基线一致

## 是否对齐 UI

本阶段不对潜影盒 / 末影箱 UI 做强制对齐，也不新增自定义末影箱 screen。

原因：

1. references/ 原作本身就保持 vanilla 差异，不存在“原作已经对齐、当前仓库未对齐”的回归
2. 当前差异主要来自 `ShulkerBoxScreen` 与 `ContainerScreen` 的原版布局差别，不是菜单逻辑错误
3. 为了只调整视觉而新增自定义末影箱 screen，反而会扩大本阶段修改范围，并增加点击区域、标签、纹理、分辨率适配的额外风险
4. 当前任务优先级是修复 quick-open 切换体验，不应在没有原作依据的前提下主动改变玩家可见 UI

因此本阶段调查结论是：

- UI 差异与原作一致
- 当前不处理 UI 统一化
- 如果发布前仍想做视觉 polish，应作为独立任务重新评估

## 人工测试清单

### Forge 1.20.1

- 打开 A 潜影盒后，鼠标悬停 B 潜影盒，右键打开 B，确认鼠标不跳回屏幕中心
- 打开 A 潜影盒后，鼠标悬停末影箱，右键打开末影箱，确认鼠标不跳回屏幕中心
- 打开末影箱后，鼠标悬停潜影盒，右键打开潜影盒，确认鼠标不跳回屏幕中心
- 打开 A 后使用快捷键切换到 B，确认鼠标不跳回屏幕中心
- `Esc` / `E` 关闭仍正常
- 潜影盒内容保存不回退
- 末影箱原版库存保存不回退
- 宿主锁定不回退
- `F` 键副手交换拦截不回退
- 右键打开后原版点击不会把宿主拿起
- 对比潜影盒与末影箱界面大小
- 对比“物品栏”标签位置
- 记录是否与 references/ 原作一致

### NeoForge 1.21.1

- 与 Forge 1.20.1 同样逐项验证

## 本阶段涉及文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickOpenMouseRestore.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickOpenMouseRestore.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `docs/stage-6b5-ui-switch-polish.md`

## 待后续 polish 观察的 UI 细节

- 高 DPI / 不同分辨率下，鼠标恢复是否仍与原作视觉一致
- 服务端拒绝 quick-open 请求时，待恢复状态超时清理是否足够保守
- 如果未来需要统一潜影盒 / 末影箱视觉，是否值得新增仅客户端自定义 screen 来对齐标签与背景纹理
