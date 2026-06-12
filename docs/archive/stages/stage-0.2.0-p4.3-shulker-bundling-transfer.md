# stage-0.2.0-p4d-shulker-bundling-transfer

本阶段为 0.2.0 的 4D，接入 shulker-to-shulker `transfer`，不扩大到 mouse dragged、Bundle 菜单、`rightClickClose` 或 `reopen inventory`。

## 本阶段完成内容

已接入 `ShulkerBundlingAction.TRANSFER`，行为语义固定为：

* 鼠标拿起的单个潜影盒是来源潜影盒
* 鼠标悬停并右键的玩家背包单个潜影盒是目标潜影盒
* 将来源潜影盒内容尽可能转移到目标潜影盒

`transfer` 受 `supportsBundlingTransfer()` 配置控制。

## 客户端输入与发包

Forge 1.20.1 与 NeoForge 1.21.1 客户端右键容器界面时，当前 bundling 判断顺序为：

1. `EXTRACT`：carried 为单个潜影盒，hovered 为空槽
2. `TRANSFER`：carried 为单个潜影盒，hovered 为单个潜影盒
3. `INSERT`：carried 为普通物品，hovered 为单个潜影盒
4. `PICKUP_INSERT`：carried 为单个潜影盒，hovered 为普通物品

客户端只发送 bundling 意图和 `HostSlotRef`，不把客户端看到的悬停槽位物品当作可信真值提交。

发送成功后会继续复用现有右键 release 抑制逻辑，避免同一次输入继续触发原版右键行为。

## 服务端重校验与提交

服务端收到 `TRANSFER` 后，会重新校验：

* `supportsBundlingTransfer()` 是否开启
* `HostSlotRef` 是否仍指向玩家真实背包槽位
* 当前槽位是否是 quick-open 当前宿主槽位
* 目标槽位真实物品是否仍为单个潜影盒
* 来源 carried 是否仍为单个潜影盒

非创造模式下，来源 carried 只使用服务端 `player.containerMenu.getCarried()`。

创造模式下，沿用 4B/4C 的 `cursorStack` 特例，只在 `player.getAbilities().instabuild == true` 时使用随包 `cursorStack` 作为 carried 真值来源；成功后仍执行完整菜单同步，并清理服务端 carried，避免幽灵物品或关闭界面时把旧副本回写到物品栏。

转移结果先通过 4A helper 计算，确认 helper 返回可提交结果后，再一次性写回：

* hovered 目标潜影盒
* carried 来源潜影盒

写回前还会再次确认 hovered 槽位仍是单个潜影盒，避免处理期间槽位被替换后错误回写。

## transfer 语义

本阶段复用 4A helper 的 `transferBetweenShulkers(sourceShulker, targetShulker)`：

* 按来源潜影盒各槽位依次尝试转移
* 优先合并到目标中已有同类堆
* 再写入目标空槽
* 空间不足时只移动目标能接收的部分
* 来源潜影盒中的潜影盒物品会被跳过，不允许转移，避免 shulker nesting
* 如果来源为空、目标无空间、任一方不是单个潜影盒，则不修改任何物品

## quick-open 安全边界

本阶段保持现有安全策略不变：

* quick-open 当前宿主槽位不会执行 `transfer`
* 不会关闭界面
* 不会恢复 `rightClickClose`
* 不会恢复 `reopen inventory`
* 不修改 quick-open 保存路径

## 构建与验证

建议至少执行：

```powershell
.\gradlew.bat build
```

如时间允许，再执行：

```powershell
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

## 建议人工测试

Forge 1.20.1 与 NeoForge 1.21.1 都应至少验证：

* 生存模式 transfer：空目标
* 生存模式 transfer：部分满目标
* 目标满时不发生变化
* 来源为空时不发生变化
* 来源含潜影盒物品时只转移普通物品
* quick-open 当前宿主槽位不执行 transfer
* `supportsBundlingTransfer=false` 时不触发
* 如创造模式启用 transfer，关闭/重开背包后无幽灵物品、无复制
* insert / pickup insert / extract 回归正常
* 不会恢复原版容器关闭后自动 reopen inventory
