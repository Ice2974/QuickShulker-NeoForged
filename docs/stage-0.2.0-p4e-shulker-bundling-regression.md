# stage-0.2.0-p4e-shulker-bundling-regression

本阶段为 0.2.0 的 4E 回归修复与审查核实阶段，聚焦 shulker bundling 的网络解码安全性和双平台一致性，不扩大到 mouse dragged、Bundle 菜单、`rightClickClose` 或 `reopen inventory`。

## 本阶段完成内容

本次对话里实际修改了以下内容：

* 为 bundling C2S 数据的枚举反序列化增加容错解析
* 将 Forge / NeoForge 的 bundling `decode` 从直接 `valueOf()` 改为安全解析
* 在 Forge / NeoForge 服务端 handler 中显式拒绝未知 action

具体表现为：

* `ShulkerBundlingAction` 新增 `UNKNOWN` 和 `fromSerializedName(...)`
* `HostStorageScope` 新增 `UNKNOWN` 和 `fromSerializedName(...)`
* `ForgeShulkerBundlingPacket.decode(...)` 改为使用容错解析
* `NeoForgeShulkerBundlingPayload.decode(...)` 改为使用容错解析
* Forge / NeoForge bundling handler 增加 `UNKNOWN` 拒绝分支

这次修复只处理服务端对客户端输入的解码边界，不改动 4E 的业务语义：

* `insert` 仍是鼠标普通物品 -> 悬停单个潜影盒
* `pickup insert` 仍是鼠标单个潜影盒 -> 悬停普通物品
* `extract` 仍是鼠标单个潜影盒 -> 悬停空槽
* `transfer` 仍保持 quickshulker-multi 语义，即鼠标来源潜影盒 -> 悬停目标潜影盒

## 审查意见核实

本阶段对外部审查意见逐条核实后，结论如下：

* `ShulkerBundlingAction` / `HostStorageScope` 直接 `valueOf()` 的解码崩溃风险：成立，已修复
* `INSERT` / `PICKUP_INSERT` 缺少显式 `isPlayerInventorySlotRef()`：不成立，现有 `resolve()` + 类型校验已经能拒绝非法 scope，属于风格建议，不是当前阶段 bug
* Forge `forPlayerInventorySlot` 缺少 offhand fallback：不成立，Forge 1.20.1 当前 `containerSlot == 40` 已覆盖副手映射
* `CreativeModeInventoryScreen$SlotWrapper` 反射路径需要补注释：不成立，属于可读性建议，不是 4E 的功能或安全缺陷

## 构建与验证

已执行：

```powershell
.\gradlew.bat build
```

结果：

* 构建成功
* 仅出现过时 API 的编译提示，没有构建失败

已执行关键词检查：

```powershell
rg "ReopenPlayerInventory|reopenPlayerInventoryAfterClose|shouldReturnToPlayerInventory|QuickOpenReturnToInventoryPolicy|schedulePendingInventoryReopenAndProcess|processPendingInventoryReopen|sendReopenPlayerInventory|reopen_player_inventory|rightClickClose"
```

结果：

* 源码目录中未发现这些 `reopen inventory` / `rightClickClose` 功能残留
* 命中仅出现在 `AGENTS.md` 和 `docs/` 的历史说明中

## 未验证内容

以下内容仍需要人工实机确认：

* Forge 1.20.1 创造模式下的 `insert` / `pickup insert` / `extract` / `transfer`
* NeoForge 1.21.1 创造模式下的 `insert` / `pickup insert` / `extract` / `transfer`
* 多人服务器环境下 bundling 对其他玩家物品栏无副作用
* quick-open 菜单已打开时，当前宿主槽位仍会拒绝 bundling
* 客户端和服务端配置不一致时的实际交互表现
* `transfer` 在游戏内是否仍严格保持“鼠标来源 -> 悬停目标”的方向

## 未包含内容

本阶段明确未实现、未恢复或未修改以下内容：

* `reopen inventory`
* `rightClickClose`
* mouse dragged
* Bundle 菜单
* 末影箱 bundling
* quick-open 保存路径的大改
* `HostSlotRef` 语义的大改
* transfer 当前方向的回滚

## 文件变更

本阶段实际修改文件：

* [common/src/main/java/com/ice2974/quickshulkerneoforged/common/network/ShulkerBundlingAction.java](../common/src/main/java/com/ice2974/quickshulkerneoforged/common/network/ShulkerBundlingAction.java)
* [common/src/main/java/com/ice2974/quickshulkerneoforged/common/open/HostStorageScope.java](../common/src/main/java/com/ice2974/quickshulkerneoforged/common/open/HostStorageScope.java)
* [versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/network/ForgeShulkerBundlingPacket.java](../versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/network/ForgeShulkerBundlingPacket.java)
* [versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/network/NeoForgeShulkerBundlingPayload.java](../versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/network/NeoForgeShulkerBundlingPayload.java)
* [versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java](../versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java)
* [versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java](../versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java)

## 备注

本阶段没有恢复 `reopen inventory` 或 `rightClickClose`，也没有改变 transfer 当前方向。Forge / NeoForge 的 bundling 行为仍保持对称，仅在非法输入上增加了更稳健的拒绝处理。
