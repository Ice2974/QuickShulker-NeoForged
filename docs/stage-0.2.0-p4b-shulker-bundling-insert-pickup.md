# 0.2.0 P4B 阶段：Shulker Bundling `insert` / `pickup insert`

## 概要

本阶段把阶段 4A 已完成的 shulker bundling 底层规则正式接入客户端右键输入、C2S 意图包和服务端重校验路径。

本阶段已接入的行为只有：

* `insert`
* `pickup insert`

本阶段明确不包含：

* `extract`
* shulker-to-shulker `transfer`
* `mouse dragged`
* Bundle 独立菜单
* `rightClickClose`

## 已接入行为

### 1. insert

当玩家在背包类界面中：

* 鼠标手上拿着普通物品
* 鼠标悬停在玩家自身背包里的单个潜影盒上
* 对该潜影盒右键

客户端只发送“我要对哪个玩家背包槽位执行 `INSERT`”的意图。

服务端收到后会重新读取当前真实 carried stack 和真实宿主槽位 `ItemStack`，重新校验：

* `supportsBundlingInsert()` 已开启
* 玩家当前不是创造模式
* 当前不是 QuickShulker quick-open 菜单
* carried stack 非空
* carried stack 不是潜影盒
* 目标槽位当前仍是单个潜影盒

只有校验通过时，才调用平台 `ShulkerBundlingHelper.insertIntoShulker(...)` 计算结果，并一次性写回：

* 更新后的宿主潜影盒 `ItemStack`
* 更新后的 carried stack

失败时不修改任何物品。

### 2. pickup insert

当玩家在背包类界面中：

* 鼠标手上拿着单个潜影盒
* 鼠标悬停在玩家自身背包里的普通物品上
* 对该普通物品右键

客户端只发送“我要对哪个玩家背包槽位执行 `PICKUP_INSERT`”的意图。

服务端收到后会重新读取当前真实 carried stack 和真实目标槽位 `ItemStack`，重新校验：

* `supportsBundlingPickup()` 已开启
* 玩家当前不是创造模式
* 当前不是 QuickShulker quick-open 菜单
* carried stack 当前仍是单个潜影盒
* 目标槽位当前仍是普通可放入容器的物品
* 目标槽位物品不是潜影盒

只有校验通过时，才调用平台 helper 把目标槽位物品插入 carried 潜影盒，并一次性写回：

* 更新后的 carried 潜影盒
* 更新后的目标槽位物品

失败时不修改任何物品。

## 配置控制

本阶段行为只受以下配置控制：

* `supportsBundlingInsert()`
* `supportsBundlingPickup()`

客户端在配置关闭时不会发送对应 C2S。
服务端即使收到了包，也会再次按配置拒绝。

本阶段没有使用 `supportsMouseDragged()`。

## 网络与安全边界

本阶段新增了 shulker bundling 专用 C2S 意图：

* common: `ShulkerBundlingIntent`
* common: `ShulkerBundlingAction`
* Forge: `ForgeShulkerBundlingPacket`
* NeoForge: `NeoForgeShulkerBundlingPayload`

客户端发送的数据只有：

* bundling 动作类型
* 玩家背包宿主槽位 `HostSlotRef`

客户端不会把自己看到的 `ItemStack` 内容当作真值上传。

服务端始终按玩家当前真实状态重新解析 `HostSlotRef`，重新读取：

* 当前 carried stack
* 当前玩家真实背包槽位物品

只有服务端重校验通过后才提交 helper 结果，并在提交后调用菜单同步。

## 当前限制

### 创造模式

为了优先保证多人和数据安全，本阶段对创造模式玩家禁用 bundling。

也就是说：

* Forge 1.20.1：创造模式不支持本阶段 bundling
* NeoForge 1.21.1：创造模式不支持本阶段 bundling

这属于阶段 4B 的已知限制。

### quick-open 菜单

为了避免与当前 quick-open 宿主锁定、关闭保存和会话收尾路径发生耦合，本阶段不允许在 QuickShulker quick-open 菜单内执行 bundling。

也就是说：

* 只在普通背包 / 容器界面中，对“玩家自身背包槽位”启用 bundling
* QuickShulker quick-open 菜单内，即使目标不是当前宿主，也先整体禁用 bundling

这同样属于阶段 4B 的已知限制。

## 不包含内容

本阶段没有实现以下内容：

* `extract`
* shulker-to-shulker `transfer`
* `mouse dragged`
* Bundle 独立菜单
* `rightClickClose`
* 末影箱 bundling
* quick-open 保存路径修改
* 末影箱同步路径修改
* `reopen inventory` 触发条件修改
* `HostSlotRef` 映射语义修改
* 宿主槽位锁定规则修改

## 构建与人工测试建议

建议至少执行：

```powershell
.\gradlew.bat build
```

如需分别确认平台，也可继续执行：

```powershell
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

建议人工重点测试：

* Forge / NeoForge 单人启动
* 客户端 + 服务端双端安装
* `insert`
* `pickup insert`
* 失败条件下物品不变
* 满潜影盒时不复制不丢失
* quick-open 菜单内不会触发 bundling
* 创造模式下不会触发 bundling
