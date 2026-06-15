# 阶段 3.8 幽灵 carried 潜影盒修复

## 问题

本阶段只处理背包界面里的客户端同步问题，不扩大到 quick-open 保存链路、`rightClickClose`、reopen inventory、Bundle 或末影箱 bundling。

复现现象：

1. 在背包界面把鼠标移动到潜影盒上。
2. 同时按下左键和右键。
3. 关闭并重新打开背包。
4. 客户端鼠标准星 / carried stack 会显示一个“幽灵潜影盒”。
5. 再点一下后幽灵潜影盒消失，但真实潜影盒仍在原槽位。

当前观察表明，这不是稳定复制物品，但会导致客户端 carried stack 显示和服务端真实状态短暂失步。

## 根因

根因在客户端输入入口，而不是 quick-open 保存主链路：

1. QuickShulker 在 `ScreenEvent.MouseButtonPressed.Pre` 里接管了右键 bundling / quick-open 输入。
2. 左右键几乎同时按下时，同一轮鼠标输入里可能同时出现：
   - 本模组右键分支已经发送 bundling / quick-open 请求并取消右键事件；
   - 原版左键槽位点击仍继续执行，临时改写客户端本地 carried stack。
3. 这样会形成“本模组已经接管右键，但原版左键又在本地拿起了潜影盒”的冲突状态。
4. 旧实现对这类同次输入没有做单次消费保护，也没有在 screen 关闭 / 重开后彻底清掉这轮临时鼠标状态。
5. 背包重新打开后，客户端就可能继续显示上一轮残留的 carried 潜影盒，形成幽灵物品显示。

本次没有把旧 `cursorStack` 或客户端本地预测 carried 当作长期真值来源，服务端真实 carried 仍然是权威。

## 修复方式

本次只改 Forge / NeoForge 的客户端屏幕输入状态机，保持服务端 bundling / quick-open 主链路不变。

### 输入互斥

1. 当本模组收到背包界面右键事件时，如果检测到左键此刻也处于按下状态，则直接拒绝本模组处理。
2. 也就是说，左右键同时按下时，不再尝试做 bundling / quick-open 的“模糊判断”，避免与原版左键 pickup 冲突。
3. 只有右键单独成立时，才允许进入本模组 bundling / quick-open 分支。

### 单次输入消费

1. 一旦本模组已经接管本次右键 bundling / quick-open，会开启一个短生命周期的客户端屏蔽状态。
2. 在这轮右键释放前，后续同次鼠标输入里的其它按键事件不再继续交给原版槽位处理。
3. 这样可以避免“本模组已经处理右键，但左键或其它派生鼠标事件又继续改写客户端 carried”的情况。

### 临时状态清理

1. `mouse release` 时继续统一结束本模组 drag 状态。
2. `screen init` 时清空本模组这轮输入屏蔽和拖拽状态。
3. screen 关闭到 `null` 后，客户端 tick 也会兜底清理这轮临时状态，避免重新打开背包时沿用旧输入状态。
4. 本模组客户端没有新增任何长期保存的预测 carried stack。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `docs/stage-1.0.0-p3.8-ghost-carried-shulker-fix.md`

## 双平台影响

### Forge 1.20.1

- 背包界面右键 bundling / quick-open 现在会在左键同时按下时保守拒绝。
- 本模组右键接管后，会屏蔽同轮释放前的其它鼠标按压，避免原版本地 carried 临时改写。
- `screen init` 和 screen 关闭后的 client tick 会清理本模组临时鼠标状态。

### NeoForge 1.21.1

- 采用与 Forge 相同的输入互斥和临时状态清理策略。
- NeoForge 客户端原先缺少与 Forge 等价的“接管后按压屏蔽”状态，本次补齐。

## 不在本次范围内

- 不改拖拽放出顺序。
- 不重写 `mouse dragged` 数据安全逻辑。
- 不修改 quick-open 保存主链路。
- 不恢复 `rightClickClose`。
- 不恢复 reopen inventory。
- 不实现 Bundle。
- 不实现末影箱 bundling。

## 验证命令

- `git diff --check`
- `.\gradlew.bat :forge-1.20.1:compileJava`
- `.\gradlew.bat :neoforge-1.21.1:compileJava`
- `.\gradlew.bat build`

## 验证结果

- `git diff --check` 通过。
- `.\gradlew.bat :forge-1.20.1:compileJava` 通过。
- `.\gradlew.bat :neoforge-1.21.1:compileJava` 通过。
- `.\gradlew.bat build` 通过。
- 本地 `build` 过程中仍有既有的 Gradle / Java 过时 API 提示，但本次没有新增编译失败。

## 待人工确认项

- 需要在 Forge 1.20.1 实机确认：同时按下左右键后重新打开背包，不再出现幽灵潜影盒 carried 显示。
- 需要在 NeoForge 1.21.1 实机确认同场景结果。
- 需要确认本次保守拒绝“左右键同时按下的右键 bundling / quick-open”不会影响维护者期望的常规右键交互节奏。
- 当前仍无法仅通过本地编译确认 Minecraft 客户端内真实交互结果。
- 当前仍无法完成多人服务器实机验证。
