# 阶段 3.5 创造模式 carried 潜影盒修复

## 问题

本阶段处理的是创造模式下与鼠标携带潜影盒相关的两个连续问题：

- 在非玩家背包界面、以及 QuickShulker 界面的玩家背包区域里，鼠标拿起潜影盒后右键收纳/放出或拖拽，潜影盒会消失。
- 在前一轮“避免消失”修复后，创造模式下右键收纳/放出或拖拽成功后再关闭界面，会额外复制出一个潜影盒。

生存模式现有 bundling / quick-open 行为需要保持不变，且不重新大改 `mouse dragged` 主逻辑。

## 日志定位

重点查看了：

- `D:\Project\QuickShulkerNeoForged\.minecraft\versions\1.20.1\logs\debug.log`
- `D:\Project\QuickShulkerNeoForged\.minecraft\versions\1.21.1\logs\debug.log`

关键日志特征：

1. 创造模式成功执行 `PICKUP_INSERT` / `EXTRACT` / `MOUSE_DRAG_*` 后，服务端日志持续出现：
   - `... after setCarried ... menuCarried=shulker_box x1`
   - `... kept server carried after sync until drag end ... menuClass=InventoryMenu, menuCarried=shulker_box x1`
2. 这说明服务端把“最新潜影盒”长期挂在了 `InventoryMenu.carried` 上。
3. 关闭创造背包前，`END_MOUSE_DRAG` 只结束了 session，没有消掉这个服务端 carried。
4. 创造背包关闭时，原版又把这份残留的服务端 carried 当成待返还物品处理，于是出现“关界面复制一个潜影盒”。

同时，前一轮日志也证明过另一个边界：

- 如果简单地在 `END_MOUSE_DRAG` 里清空 `containerMenu.carried`，会把玩家鼠标上真实拿着的潜影盒一起清掉，重新触发“潜影盒消失”。

## 根因

根因是创造模式下把 `player.containerMenu.getCarried()` 同时拿来承担了两件互相冲突的职责：

1. 作为 bundling 连续右键 / 拖拽过程中的“最新潜影盒内容”来源。
2. 又作为原版创造背包关闭逻辑会消费的服务端 carried 状态。

这会导致两个相反的故障：

- 收尾时清掉它：玩家手上的潜影盒消失。
- 收尾时保留它：关界面时被原版再次返还，产生复制。

因此，创造模式不能继续把 `InventoryMenu.carried` 当作 QuickShulker creative bundling 的长期状态仓库。

## 修复方式

本次改动采用最小必要调整：

1. 创造模式下，bundling 的“最新 carried 栈”改为保存在 QuickShulker 自己的 drag session 中，而不是长期写在服务端 `InventoryMenu.carried` 上。
2. 服务端每次完成创造模式 bundling 后，新增一个小型 S2C 同步包，把更新后的 carried 栈直接同步到客户端当前鼠标 / 当前菜单。
3. 创造模式 drag continuation 继续从 session 中读取最新潜影盒，保证连续右键和拖拽不会回退到旧 NBT。
4. `END_MOUSE_DRAG` 和 `clearDragSession` 现在只结束会话缓存，不再依赖清空服务端 `containerMenu.carried` 来“收尾”。
5. 生存模式仍然继续使用原有 `containerMenu.carried` 路径，不改变现有已验证行为。

这样可以同时满足：

- 创造模式下成功 bundling 后，鼠标上的潜影盒不会消失。
- 关闭创造背包时，不会因为服务端 `InventoryMenu.carried` 残留而复制潜影盒。
- 生存模式逻辑不受影响。
- 不重写已有 `mouse dragged` 主流程，只修正 creative carried 的真值来源和同步方式。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/network/ForgeQuickShulkerNetwork.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/network/ForgeCreativeCursorSyncPacket.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/network/NeoForgeQuickShulkerNetwork.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/network/NeoForgeCreativeCursorSyncPayload.java`
- `docs/stage-1.0.0-p3.5-creative-carried-shulker-fix.md`

## 双平台影响

### Forge 1.20.1

- 创造模式 bundling 不再把最新潜影盒内容长期保留在服务端 `InventoryMenu.carried`。
- 连续右键 / 拖拽改为复用 QuickShulker drag session + 客户端 cursor 同步包。
- 生存模式 carried / bundling 主链路不变。

### NeoForge 1.21.1

- 与 Forge 保持同样的 creative carried 修复策略。
- 连续右键 / 拖拽同样改为复用 session 缓存和独立 cursor 同步。
- 生存模式现有逻辑不变。

## 验证命令

- `git diff --check`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`

## 验证结果

待本轮代码编译与检查完成后补充。

## 待人工确认项

- 需要维护者在 Forge 1.20.1 实机确认：创造模式下对有物品格子的右键收纳/放出、连续拖拽，以及随后关闭界面，不再出现“消失”或“复制潜影盒”。
- 需要维护者在 NeoForge 1.21.1 实机确认同类场景。
- 无法仅通过本地编译确认 Minecraft 客户端内真实交互结果。
- 无法在本地完成多人服务器实机验证。
