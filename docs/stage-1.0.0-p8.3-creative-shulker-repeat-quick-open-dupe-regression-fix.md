# 阶段 1.0.0 P8.3 创造模式潜影盒重复 quick-open + bundling 复制回归修复

## 背景

1.0.0 发布前测试发现，阶段 8.2 修复"创造模式容器 bundling 后切换 quick-open 导致物品复制"问题后，此前已被修复的另一个创造模式潜影盒复制 bug 重新出现。这是发布阻断问题。

## 问题现象

### 复现路径

1. 创造模式打开物品栏。
2. 使用一次潜影盒右键或拖拽功能（如 EXTRACT 放出、PICKUP_INSERT 收入）。
3. 在不关闭背包的情况下，右键打开一个 QuickShulker 页面。
4. 拿起潜影盒，对刚才的潜影盒使用右键或拖拽功能。
5. 关闭背包页面并重新打开。
6. 重复两次此操作。
7. 在第二次关闭背包时，会发现潜影盒被复制。

这不一定是唯一触发途径，可能只是其中一种。该问题是"创造模式 carried stack / quick-open 切换 / bundling session 状态 / 宿主限制"组合回归。

## 根因

### 历史背景

- 阶段 3.5：确立创造模式下 bundling 的 carried 真值应保存在 QuickShulker 自己的 drag session 中，而不是长期写在服务端 InventoryMenu.carried 上。
- 阶段 5.2：在 writeCarried 创造模式分支中额外调用 player.containerMenu.setCarried(copy)，修复"末影箱右键收入后关闭背包界面复制"问题。
- 阶段 8.1：发现 5.2 方式在某些场景下仍有复制风险，移除了 writeCarried 创造模式分支中的 player.containerMenu.setCarried(copy)，改为完全不写服务端 carried。
- 阶段 8.2：发现 8.1 的"完全不写服务端 carried"策略在玩家通过**原版左键**从背包拿起物品后做 bundling INSERT 时，服务端 carried 保留了旧值（左键拿起的物品），AbstractContainerMenu.removed() 把旧值放回背包，产生复制。8.2 恢复了 writeCarried 中的 player.containerMenu.setCarried(copy)。

### 当前 bug 的直接原因

阶段 8.2 恢复的 `player.containerMenu.setCarried(copy)` 在**创造物品列表来源**的 carried 场景下引入了新的复制路径。

创造模式下，客户端的 CreativeModeInventoryScreen 使用 ItemPickerMenu（containerId=0，与服务端 InventoryMenu 相同）。当玩家从**创造物品列表**（非 Inventory tab）拿起物品时：

1. 客户端通过 CreativeModeInventoryScreen 的特殊逻辑处理，ItemPickerMenu.carried 被设置为拿起的物品（如 shulker_box）。
2. 但这个操作**不走标准的 ServerboundContainerClickPacket**，服务端 InventoryMenu.carried **保持为空**。

随后玩家做 bundling 操作（如 EXTRACT）：

3. 客户端发送 ShulkerBundlingIntent，附带 cursorStack（= shulker_box，来自创造列表）。
4. 服务端 resolvedCarried 在创造模式下返回 cursorStack（shulker_box），因为服务端 InventoryMenu.carried 为空。
5. bundling 执行，writeCarried 设置 player.containerMenu.setCarried(shulker_box)。
6. **此时服务端 InventoryMenu.carried = shulker_box，这是一个"凭空生成"的物品**——它来自创造物品列表，从未从玩家背包中取出。

当菜单关闭或切换时：

7. AbstractContainerMenu.removed() 检测到 carried 非空（shulker_box），调用 Inventory.placeItemBackInInventory(shulker_box)。
8. **shulker_box 被放回玩家背包，变成一个真实的物品**——这就是复制。

重复此操作时，每次都会把一个创造列表的虚拟物品转化为真实物品，累积后表现为潜影盒复制。

### 与 8.2 的矛盾

- 8.2 的修复：标准左键来源的 carried（服务端有记录）→ writeCarried 必须更新服务端 carried，否则 removed() 放回旧值。
- 8.3 的发现：创造列表来源的 carried（服务端无记录）→ writeCarried 不能写入服务端 carried，否则 removed() 放回虚拟值。

两者矛盾的核心在于：服务端无法直接从客户端 cursorStack 区分 carried 来源。

## 修复方案

### 区分 carried 来源

利用服务端 containerMenu.getCarried() 在 writeCarried 调用时的值（即操作前的值）来区分：

- **标准左键来源**：操作前服务端 carried 非空 → writeCarried 正常写入服务端 carried，removed() 放回正确值（8.2 修复保持）。
- **创造列表来源**：操作前服务端 carried 为空 → writeCarried **不写入**服务端 carried（保持空），removed() 看到 carried 空不放回（8.3 修复）。
- **INSERT 后 carried 变空**：操作前 carried 非空 → writeCarried 写入 empty，清除旧值，removed() 不放回。

客户端始终通过 syncCreativeCursor 保持 carried 同步，不受服务端是否写入影响。

### 涉及路径

- Forge / NeoForge bundling handler 的 writeCarried 创造模式分支。
- carried 写入 / menu removed / screen close / drag session / 宿主限制路径均不受额外影响。
- 生存模式 carried 主链路完全不变（writeCarried 生存分支不受影响）。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`

## 数据安全

- 创造模式 bundling 修改 carried 后，服务端 containerMenu.carried 仅在操作前非空时才更新。
- 创造物品列表来源的 carried 不再被写入服务端，removed() 不会放回虚拟物品。
- 标准左键来源的 carried 仍然被正确更新（8.2 修复保持）。
- 生存模式 carried / bundling 主链路完全不变。
- 末影箱内容仍只读写玩家自己的 EnderChestInventory，不写入宿主 ItemStack。
- 宿主槽位锁定、同一宿主重复打开拒绝、容器写回目标校验等既有安全规则不受影响。
- 服务端仍不信任客户端请求，resolvedCarried / carriedStillMatches / HostSlotRef 校验 / 宿主有效性校验逻辑不变。
- 潜影盒不能嵌套进潜影盒、末影箱不能收入末影箱等既有规则不放松。

## 双平台影响

### Forge 1.20.1

- ForgeShulkerBundlingHandler.writeCarried 创造模式分支增加服务端 carried 来源判断。

### NeoForge 1.21.1

- NeoForgeShulkerBundlingHandler.writeCarried 创造模式分支增加服务端 carried 来源判断。

## 与上一阶段修复的关系

- 8.2 修复了"标准左键来源 carried 在 INSERT 后服务端 carried 未更新导致 removed() 放回旧值"的复制。
- 8.3 修复了"创造物品列表来源 carried 被错误写入服务端导致 removed() 放回虚拟物品"的复制。
- 两者通过"检查操作前服务端 carried 是否为空"统一为同一段逻辑。

## 验证

- `git diff --check`
- `.\gradlew.bat :common:test`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`

## 验证结果

- `git diff --check`：通过（无空白错误，仅行尾符警告）。
- `:common:test`：BUILD SUCCESSFUL（common 未改动，规则层不变）。
- `:forge-1.20.1:compileJava`：BUILD SUCCESSFUL。
- `:neoforge-1.21.1:compileJava`：BUILD SUCCESSFUL。
- 代码层修复完成；游戏内行为待人工验收。

## 待人工确认项

1. 无法进行 Minecraft 游戏内测试，无法确认创造模式复制问题在实机中已被完全消除。需维护者在 Forge 1.20.1 和 NeoForge 1.21.1 实机确认以下场景：
   - 创造模式从创造物品列表拿起潜影盒做 EXTRACT 后切换 quick-open，在 quick-open 内重复 bundling 操作，关闭重开不复制。
   - 创造模式从创造物品列表拿起潜影盒做 PICKUP_INSERT 后切换 quick-open，在 quick-open 内重复 bundling 操作，关闭重开不复制。
   - 创造模式从创造物品列表拿起末影箱做 bundling 后切换 quick-open，关闭重开不复制。
   - 创造模式从 Inventory tab（背包）拿起潜影盒做 bundling 后切换 quick-open，关闭重开不复制（8.2 路径不回退）。
   - 创造模式连续多次 bundling + quick-open 切换，不出现复制或丢失。
   - 生存模式相同操作物品数量守恒，不丢失、不复制。
2. 无法进行多人服务器测试，无法确认多人环境下创造模式 bundling + quick-open 切换不再产生复制。
3. 无法确认是否存在除已知复现路径以外的其他创造模式复制触发方式。
4. 无法确认最低 Forge / NeoForge loader 版本。
5. 无法确认 Modrinth 发布元数据。