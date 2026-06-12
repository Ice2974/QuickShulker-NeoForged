# stage-1.0.0-p3.3-mouse-dragged-data-safety

## 漏洞描述

本阶段处理 1.0.0 发布前阻塞级数据安全问题：

- Forge 1.20.1 快速拖拽收纳时，被目标槽扣除的物品数量可能大于实际进入鼠标潜影盒的数量，表现为吞物品。
- Forge 1.20.1 快速拖拽放出时，放出的物品数量可能大于潜影盒实际减少的数量，表现为复制物品。
- NeoForge 1.21.1 快速拖拽后关闭背包时，鼠标携带的潜影盒存在消失风险。
- NeoForge 1.21.1 偶发潜影盒复制的触发条件暂未完全明确，本阶段先按旧 packet、旧菜单和 carried stack 副本冲突做保守防护。

## 原作对照

已查看的原作文件：

- `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/util/MouseDraggedHandler.java`
- `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/util/BundleHelper.java`
- `references/quickshulker-1.21.1/src/main/java/net/kyrptonaught/quickshulker/network/QuickBundlePacket.java`
- `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/util/MouseDraggedHandler.java`
- `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/util/BundleHelper.java`
- `references/quickshulker-26.1-neo/src/main/java/net/kyrptonaught/quickshulker/network/QuickBundlePacket.java`

原作 `MouseDraggedHandler` 在客户端拖过新槽位时按槽触发一次点击路径，不是拖拽结束后 batch 提交；它只维护客户端 `DRAGGED_SLOTS` 去重。原作 `BundleHelper` 使用存储 API transaction 和槽位 `safeTake` / `safeInsert` 一类原子操作，让单次槽位移动在一个事务边界内完成。原作网络包在服务端线程执行；NeoForge 26.1 的 packet 处理使用 `context.enqueueWork`。

当前项目没有 Fabric/NeoForge Storage transaction 可直接照搬，Forge 1.20.1 也以 NBT 写回为主。因此本阶段采用等价替代：

- 仍保留逐槽 packet，不改成 batch。
- 每个服务端槽位处理先复制真实 carried stack 和真实目标槽，再计算结果。
- 写回前重新确认 containerId、目标槽、carried stack 和安全写回条件。
- 写回前后做数量守恒校验，失败则整次槽位操作拒绝。
- 增加服务端 drag session 和 dragId 去重，避免只依赖客户端 `DRAGGED_HOST_SLOTS`。

涉及参考项目思路但没有整段复制原作实现。是否需要更新 `THIRD_PARTY_NOTICES.md` 仍建议维护者在发布前人工确认。

## 日志分析

已查看日志：

- `.minecraft/versions/1.20.1/logs/latest.log`
- `.minecraft/versions/1.20.1/logs/debug.log`
- `.minecraft/versions/1.21.1/logs/latest.log`
- `.minecraft/versions/1.21.1/logs/debug.log`

检索关键字包括 `quickshulker`、`shulker`、`mouse`、`drag`、`carried`、`slot`、`container`、`packet`、`disconnect`、`exception`、`error`、`warn`。

Forge 日志未发现直接异常、崩溃或断连栈；但 debug 中能看到创造模式拖拽时服务端 `menuCarried=<empty>`、payload 携带 `shulker_box x1`，说明旧逻辑会依赖客户端上传的 cursor stack 副本继续处理。NeoForge 日志同样出现该现象，并且在 `debug.log` 约 6891、6909、6913 行附近可以看到 `payloadCursor=minecraft:shulker_box x1`、`resolvedCarried=minecraft:shulker_box x1`、`menuCarried=<empty>` 的组合。NeoForge `latest.log` 仅有普通连接断开记录，未发现可直接归因于本问题的异常栈。

## 根因分析

Forge 1.20.1 的数量不守恒根因是 mouse dragged 逐槽 packet 处理没有服务端 drag session，也没有把“目标槽扣减”和“carried shulker 内容写回”放在一个可验证的守恒边界内。快速拖拽时多个请求都可能基于客户端 cursor stack 或过期 carried 状态处理，导致目标槽和潜影盒内容更新不是同一份状态的连续演进。

NeoForge 1.21.1 的盒子消失/复制风险根因没有完全复现到唯一路径，但日志和代码共同显示旧逻辑存在两个危险点：服务端 packet 没有显式绑定当前 menu/container，且创造模式会把 payload cursor stack 当作 carried shulker 来源。快速关闭背包或切换菜单后，旧 packet 若继续处理，就可能对已经不再对应当前菜单的 carried shulker 副本做写回或清空。

## 修复策略

- `ShulkerBundlingIntent` 增加 `containerId` 和 `dragId`。
- Forge / NeoForge 客户端在开始右键拖拽时生成非 0 `dragId`，后续每个拖拽槽位携带同一个 `dragId` 和当前 `containerId`。
- 客户端在鼠标释放、screen 不再是容器界面、containerId 变化时清理拖拽状态。
- 服务端收到 bundling packet 后先校验 `intent.containerId()` 必须等于当前 `player.containerMenu.containerId`，不匹配直接拒绝。
- 服务端按玩家 UUID 保存 drag session，记录 `containerId`、`dragId`、action、HostSlotRef 已处理集合和 session carried shulker。
- 同一 dragId 下同一 action + HostSlotRef 重复请求直接忽略。
- 服务端 drag session 在玩家 tick 中按超时和 menuId 变化清理，并在登出、重生、切维度时立即清理。
- Forge 网络原有 `consumerMainThread` 保持不变；NeoForge 服务端 packet 改为 `context.enqueueWork`，确保菜单和物品读写在服务端线程顺序执行。
- 拖拽收纳和拖拽放出都使用逐槽事务化处理，不采用 batch。
- 每个槽位写回前重新确认 carried shulker、目标槽状态和安全写回条件。
- 每个成功槽位提交后调用 menu/inventory 同步。

## 守恒校验

拖拽收纳：

- 读取 `targetBefore` 和 `shulkerBefore`。
- 计算 helper 结果但先不写回真实槽位。
- 要求 `targetBefore - updatedTarget.getCount()` 等于 `count(updatedCarried) - shulkerBefore`。
- 守恒失败、目标槽变化、carried stack 变化或不可安全写回时，拒绝本槽操作。

拖拽放出：

- 目标槽必须为空。
- 读取 `shulkerBefore`。
- 计算 extracted stack 和 updated carried shulker。
- 要求 `extractedStack.getCount()` 等于 `shulkerBefore - count(updatedCarried)`。
- 守恒失败、目标槽不再为空、carried stack 变化或不可安全写回时，拒绝本槽操作。

## 已支持场景

- 鼠标携带单个潜影盒，右键拖过多个安全普通物品槽，将物品收纳进鼠标潜影盒。
- 鼠标携带单个潜影盒，右键拖过多个安全空槽，从潜影盒中依次放出物品。

## 未支持场景

- Bundle。
- 末影箱 bundling。
- `rightClickClose`。
- `reopen inventory`。
- 向非空槽合并放出。
- 潜影盒到潜影盒拖拽转移。

## 修改文件

- `common/src/main/java/com/ice2974/quickshulkerneoforged/common/network/ShulkerBundlingIntent.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeQuickShulkerEvents.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/network/ForgeQuickShulkerNetwork.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/network/ForgeShulkerBundlingPacket.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeQuickShulkerEvents.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/network/NeoForgeQuickShulkerNetwork.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/network/NeoForgeShulkerBundlingPayload.java`
- `docs/stage-1.0.0-p3.3-mouse-dragged-data-safety.md`

## 验证命令和结果

- `.\gradlew.bat :forge-1.20.1:compileJava`
  - 通过。
- `.\gradlew.bat :neoforge-1.21.1:compileJava`
  - 通过，仅有既有 deprecated API 提示。
- `.\gradlew.bat :forge-1.20.1:build`
  - 通过。
- `.\gradlew.bat :neoforge-1.21.1:build`
  - 通过。
- `git diff --check`
  - 通过。

## 未验证内容

- 未进行 Minecraft 客户端内真实拖拽实机测试。
- 未进行 Forge / NeoForge 专用服务器多人测试。
- 未复现 NeoForge 偶发潜影盒复制的唯一触发路径，因此不能声明“已确认根因唯一且完全复现修复”。
- 未确认许可证 / NOTICE 是否需要因参考原作思路而更新。

## 待人工确认项

- Forge 1.20.1 和 NeoForge 1.21.1 实机快速拖拽收纳 / 放出是否在生存、创造和服务器环境下均无吞物品、复制物品、盒子消失。
- 快速拖拽过程中关闭背包、切换菜单、死亡、掉线、切维度后的同步表现。
- 是否需要更新 `THIRD_PARTY_NOTICES.md` 或发布说明中的来源说明。
- `docs/stage-1.0.0-p3-mouse-dragged.md` 和 `docs/stage-1.0.0-p3.1-bundling-menu-slots.md` 是较早阶段文档，部分内容已不代表当前 p3.3 后行为；当前行为以源码和本文档为准。

## 追加排查：创造模式关闭界面丢失鼠标潜影盒

用户反馈前述数量守恒问题修复后，Forge 1.20.1 和 NeoForge 1.21.1 在创造模式拖拽收纳或放出过程中关闭背包界面，鼠标携带的潜影盒仍可能消失。

本次继续查看：

- `.minecraft/versions/1.20.1/logs/latest.log`
- `.minecraft/versions/1.20.1/logs/debug.log`
- `.minecraft/versions/1.21.1/logs/latest.log`
- `.minecraft/versions/1.21.1/logs/debug.log`

日志未发现新的崩溃栈或直接异常。关键线索出现在 debug 日志：创造模式 bundling 服务端处理时，packet 中 `payloadCursor` 和解析后的 `resolvedCarried` 仍是 `shulker_box x1`，但 `menuCarried` 初始为 `<empty>`；事务写回后日志显示 `after setCarried` 已变为潜影盒，随后又出现 `cleared server carried after sync` 并把 `menuCarried` 清回 `<empty>`。

代码层面根因是双平台 `clearCreativeServerCarriedAfterSync` 在创造模式且当前菜单是 `inventoryMenu` 时，无条件执行 `player.containerMenu.setCarried(ItemStack.EMPTY)`。这会让服务端权威 carried stack 在拖拽操作后变成空；如果玩家此时关闭创造背包，关闭流程会看到服务端鼠标为空，从而可能丢失客户端仍显示携带的潜影盒。

修复策略：

- Forge 1.20.1 和 NeoForge 1.21.1 都移除创造模式 bundling 后的服务端 carried 清空。
- 保留事务化写回后的 `player.containerMenu.setCarried(updatedCarried)` 结果作为服务端权威状态。
- 继续执行 `broadcastChanges()` / `broadcastFullState()`，让客户端显示由服务端最新 carried stack 覆盖。
- 将收尾方法改名为 `finishCreativeServerCarriedAfterSync`，只记录当前菜单类型和 carried 状态，不再修改物品。

该修复没有改变逐槽事务、dragId/session 去重、containerId 校验和数量守恒逻辑。仍需实机确认创造模式关闭背包时潜影盒不再消失，且不会重新引入复制。
