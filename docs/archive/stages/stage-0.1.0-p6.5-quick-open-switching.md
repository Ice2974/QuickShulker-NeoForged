# 阶段 6C：QuickOpen 菜单内切换打开修复

本次修复只处理以下问题：

- 已打开 QuickShulker 潜影盒界面后，无法继续右键或按快捷键打开背包里的其他潜影盒 / 末影箱
- 已打开 QuickShulker 末影箱界面后，无法继续右键或按快捷键打开背包里的其他潜影盒 / 末影箱

本次不修改：

- `references/`
- Bundle
- 工作台 / 切石机 / 铁砧
- 鼠标拖拽批量行为
- README / 发布说明
- 已稳定的常规关闭保存链路

## 根因确认

问题根因确认是两段“方向正确但粒度过粗”的保护叠加：

1. 客户端 `ForgeQuickShulkerClient.trySendHovered(...)` / `NeoForgeQuickShulkerClient.trySendHovered(...)`
   - 只要当前菜单是 `ForgeQuickOpenMenu` / `NeoForgeQuickOpenMenu` 就直接 `return false`
   - 结果是 QuickShulker 菜单内，背包中的其他合法宿主也完全无法发起新的 `OpenHostItemIntent`

2. 服务端 `ForgeShulkerSessionManager.open(...)` / `NeoForgeShulkerSessionManager.open(...)`
   - 只要玩家已经有 active session 就直接拒绝
   - 结果是“切换打开另一个宿主”和“重复打开当前宿主”被混在一起，一并被拦截

这两段保护原本是为了修复“右键当前宿主再次打开同一个盒子，出现两个不互通页面”的 bug。
保护方向没有错，但现在需要细化为：

- 同一宿主重复打开：拒绝
- 不同宿主切换打开：允许
- 切换前：先显式安全收尾当前 session

## 本次实现

### 1. 客户端从“菜单内全禁用”改为“只阻止当前同一宿主”

双平台 `trySendHovered(...)` 现在不再因为当前菜单是 QuickShulker 菜单就直接返回。

改动后：

- 仍只允许悬停玩家背包中的合法 quick-openable 宿主
- 仍要求鼠标没有携带物品
- 仍要求宿主堆叠数为 `1`
- 如果当前菜单也是 QuickShulker 菜单，则额外比较：
  - `requestedTypeId`
  - `HostStorageScope`
  - `logicalSlotIndex`
- 如果判定为当前同一宿主，则客户端不再发包
- 如果是不同宿主，则继续发送 `OpenHostItemIntent`
- 右键成功发包时仍取消原版点击
- 快捷键成功发包时仍取消原版按键

为避免把不同菜单上下文中的 `menuSlotIndex` 误当成宿主身份，本次新增了平台无关 helper：

- `common/.../open/HostIdentity.java`

它把宿主身份比较收敛为：

- 类型 id 一致
- `scope` 一致
- `logicalSlotIndex` 一致

`menuSlotIndex` 继续只作为菜单上下文信息保留，不作为“是否同一宿主”的唯一依据。

### 2. 服务端从“有 active session 就拒绝”改为“同宿主拒绝，异宿主切换”

双平台 `ShulkerSessionManager.open(...)` 现在改成：

1. 没有 existing session
   - 直接按原逻辑打开

2. 已有 existing session，且新请求与当前 session 是同一宿主
   - 拒绝
   - 不创建第二个容器
   - 不覆盖原 session
   - 记录 debug 日志

3. 已有 existing session，且新请求是不同宿主
   - 先显式调用 `finishSession(player, existingSession.menu(), CloseReason.PLAYER_CLOSED, "switch_open")`
   - 这一步会先把当前 session 从 manager 中移除，再走既有保存 / 丢弃判定
   - 对 `shulker_box`：
     - 当前宿主仍有效时，把实时容器内容写回当前宿主
   - 对 `ender_chest`：
     - 继续走现有 transient session 收尾，不向错误目标写回
   - 完成收尾后，再重新校验新宿主是否仍有效
   - 若新宿主已失效，则拒绝新 open，不创建新 menu
   - 若仍有效，则打开新的目标

### 3. 避免 `openMenu(...)` 过程中旧菜单重复收尾带来副作用

这次没有依赖 `tick_menu_mismatch` 去延迟补救，而是在处理新 open 请求时就显式 finish 当前 session。

当前 `finishSession(...)` 的幂等特性继续保留：

- session 不存在时直接返回
- 指定 menu 与当前 session menu 不一致时直接返回
- 先从 `sessions` 中移除，再执行保存 / 丢弃逻辑

这样即使后续 `player.openMenu(...)` 触发旧菜单 `removed()`，旧菜单再次调用 `finishSession(...)` 也只会因为 session 已不存在而直接返回，不会重复写回，不会重复关闭。

## 数据安全说明

以“从 A 潜影盒切换到 B 潜影盒”为例：

1. 玩家先在 A 中改动内容
2. 在 A 界面里右键 / 快捷键请求打开 B
3. 服务端发现这是“不同宿主切换”
4. 先 finish A：
   - A 仍有效时，把当前 `ItemBackedShulkerContainer` 内容写回 A
5. 再重新校验 B
6. B 仍有效时，打开 B

因此本次目标是继续保证：

- A 的内容不会因为切换而丢失
- 不会把 A 的内容写到 B
- 不会把 B 的内容写回 A
- 不会为同一宿主创建两个不互通页面
- 新目标在切换过程中若已失效，不会错误打开，也不会写回错误目标

对末影箱切换同理：

- 末影箱宿主切换时，旧 session 先按既有 close 流程收尾
- 末影箱本身不走 shulker item write-back
- 新目标重新校验通过后再打开

## 影响文件

- `common/src/main/java/com/ice2974/quickshulkerneoforged/common/open/HostIdentity.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeQuickOpenMenu.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerMenu.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeEnderChestMenu.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerSessionManager.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeQuickOpenMenu.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerMenu.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeEnderChestMenu.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerSessionManager.java`

## 人工测试清单

### Forge 1.20.1

- 打开 A 潜影盒，在 A 中放入物品后，右键打开背包里的 B 潜影盒
- 打开 A 潜影盒，在 A 中放入物品后，快捷键打开背包里的 B 潜影盒
- 打开 A 潜影盒，右键当前宿主 A，不创建第二个页面
- 打开 A 潜影盒，快捷键当前宿主 A，不创建第二个页面
- 打开潜影盒后，右键打开背包里的末影箱
- 打开潜影盒后，快捷键打开背包里的末影箱
- 打开末影箱后，右键打开背包里的潜影盒
- 打开末影箱后，快捷键打开背包里的潜影盒
- 打开末影箱后，右键 / 快捷键打开另一个末影箱宿主
- 切换打开过程中，宿主锁定、`F` 键拦截、`PICKUP_ALL`、`QUICK_CRAFT` 防护不回退
- `Esc` / `E` 正常关闭保存不回退
- 宿主在切换前后被移走、替换、数量不为 `1` 时，不错误写回，不错误打开

### NeoForge 1.21.1

- 与 Forge 1.20.1 同样逐项验证

## 留待阶段 7 的边界

本次没有扩展处理以下边界，继续留到阶段 7：

- 创造模式与特殊 `SlotWrapper` / `CLONE` 相关路径的专项验证
- 更复杂 screen / menu 组合下的宿主映射边角
- 工作台 / 切石机 / 铁砧等其他 quick-openable 类型
- 鼠标拖拽批量行为
- 完整多人实机回归
