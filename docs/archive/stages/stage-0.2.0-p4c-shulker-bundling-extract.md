# stage-0.2.0-p4c-shulker-bundling-extract

本阶段为 0.2.0 的 4C，接入 shulker bundling `extract`，不扩大到 transfer、mouse dragged、Bundle 菜单、`rightClickClose` 或 `reopen inventory`。

## 本阶段完成内容

已接入 `extract`：

* 玩家在背包类界面中鼠标拿起单个潜影盒
* 鼠标悬停在玩家真实背包空槽
* 右键该空槽

效果：

* 从鼠标拿起的潜影盒中找到第一个非空槽位
* 提取该槽位整组 `ItemStack`
* 将提取出的整组物品放入目标空槽
* 将更新后的潜影盒写回鼠标 carried stack

`extract` 受 `supportsBundlingExtract()` 控制。

## 语义与安全边界

本阶段保持 4A 的提取语义，不实现半组提取、单个提取或修饰键变体：

```text
从潜影盒中找到第一个非空槽位，提取该槽位整组 ItemStack。
```

客户端只发送：

* `ShulkerBundlingAction.EXTRACT`
* 目标 `HostSlotRef`
* 仅供创造模式特例使用的 `cursorStack`

服务端收到请求后重新校验真实状态：

* `supportsBundlingExtract()` 是否开启
* `HostSlotRef` 是否仍是合法玩家背包槽位
* 目标槽位当前是否真实为空
* carried stack 是否为单个潜影盒
* 当前目标是否撞上 quick-open 菜单正在锁定的宿主槽位

只有校验通过且 helper 成功提取后，才会一次性写回：

* 更新后的 carried 潜影盒
* 目标空槽中的 extracted stack

失败时不修改任何物品。

## 创造模式边界

本阶段保留 4B 的 creative 特例策略，`cursorStack` 只在 `player.getAbilities().instabuild == true` 时作为 carried 真值来源使用。

非创造模式仍只信任服务端 `player.containerMenu.getCarried()`，不会把客户端上传的 cursor stack 当作真实物品状态。

后续修正中，creative bundling 的同步改为以当前打开菜单的 carried/full-state 为主，不再额外推送 `inventoryMenu` 的整包数据，避免关闭创造背包时把修改前的潜影盒副本重新同步回物品栏。

再次修正后，客户端发包时的 `cursorStack` 也改为直接读取当前 `AbstractContainerScreen` 所对应菜单的 carried stack，而不是读取 `player.containerMenu`。这是因为创造背包界面下两者可能不是同一个菜单对象，错误的 carried 来源会让服务端按旧潜影盒内容处理 bundling。

结合后续日志排查，creative bundling 现在还会在 `broadcastFullState()` 之后立即把服务端 `containerMenu` 的 carried 清回空。原因是创造背包下服务端原始 `menuCarried` 本来就是空，若把潜影盒长期留在服务端 carried，有概率在关闭界面阶段被原版路径再次回收到物品栏，表现为额外复制出一个潜影盒。

## 未包含内容

本阶段明确未实现：

* shulker-to-shulker transfer
* mouse dragged 批量行为
* Bundle 独立菜单
* `rightClickClose`
* `reopen inventory`
* 末影箱 bundling

也没有修改 quick-open 保存路径、宿主锁定规则、HostSlotRef 映射语义或菜单切换逻辑。

## 构建与验证

建议至少执行：

```powershell
.\gradlew.bat build
```

如时间允许，再分别执行：

```powershell
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

## 建议人工测试

建议重点验证：

* Forge 1.20.1 单人下空槽 extract
* NeoForge 1.21.1 单人下空槽 extract
* quick-open 菜单内非宿主槽位 extract
* quick-open 菜单内当前宿主槽位 extract 被拒绝
* 潜影盒为空时不改物品
* 目标槽非空时不改物品
* 非单个潜影盒 carried 时不触发
* 创造模式下不出现复制、幽灵物品或错误写回
* 客户端和服务端双端安装时多人环境只影响发包玩家本人
