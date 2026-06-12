# stage-1-config-gui

## 本阶段实现内容

本阶段聚焦配置界面入口与配置编辑能力，保持阶段 1 只处理配置 GUI，不进入阶段 2。

- Forge 1.20.1 与 NeoForge 1.21.1 均保留配置界面入口。
- 配置界面继续兼容显示 `supportsMouseDragged`，但本阶段不接入实际鼠标拖拽行为，也不可在本阶段视为已实现。
- Mods 页面配置按钮仍作为配置界面入口之一。

## 本次补丁修复内容

本次补丁仅修复“配置界面快捷键被 quick-open 配置条件错误拦截”的问题。

### Forge 1.20.1

- 调整 `ForgeQuickShulkerClient#onClientTick` 中 `OPEN_SETTINGS_SCREEN` 的处理顺序。
- 现在配置快捷键只受以下条件限制：
  - `minecraft.player != null`
  - `minecraft.screen == null`
  - `ForgeQuickShulkerConfig.view().openSettingsKeyEnabled()`
  - `ForgeKeyMappings.OPEN_SETTINGS_SCREEN.consumeClick()`
- 不再依赖 `hasAnyEnabledQuickOpenable()`、`keybindInHand()`、`keybindInInventory()`、`rightClickToOpen()`。

### NeoForge 1.21.1

- 按与 Forge 相同的方式调整 `NeoForgeQuickShulkerClient#onClientTick`。
- 现在配置快捷键只受以下条件限制：
  - `minecraft.player != null`
  - `minecraft.screen == null`
  - `NeoForgeQuickShulkerConfig.view().openSettingsKeyEnabled()`
  - `NeoForgeKeyMappings.OPEN_SETTINGS_SCREEN.consumeClick()`
- 不再依赖 `hasAnyEnabledQuickOpenable()`、`keybindInHand()`、`keybindInInventory()`、`rightClickToOpen()`。

## 明确未实现内容

以下内容本阶段未实现，也未在本次补丁中恢复或接入：

- Bundle 独立菜单
- `supportsMouseDragged` 对应的实际 mouse dragged 行为
- 末影箱 bundling
- `rightClickClose`
- `reopen inventory`

## 未改动边界

本次补丁未改动以下范围：

- shulker / ender chest 容器保存逻辑
- HostSlotRef / slot 映射逻辑
- 网络协议和数据包结构
- Bundle 独立菜单、mouse dragged 交互与其他阶段 2 内容

## 验证结果

已执行：

```powershell
git diff --check
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

结果以本次补丁提交时的实际命令输出为准，应确认：

- `git diff --check` 无格式错误
- Forge 1.20.1 构建通过
- NeoForge 1.21.1 构建通过

## 待人工测试项

- 正常情况下按配置快捷键能打开配置界面。
- 关闭 `keybindInHand` 后，配置快捷键仍能打开配置界面。
- 关闭所有 quick-open 目标后，配置快捷键仍能打开配置界面。
- 关闭 `openSettingsKeyEnabled` 后，配置快捷键不再打开配置界面。
- Mods 页面配置按钮仍可打开配置界面。
- 修改配置、关闭界面、重启客户端后配置仍生效。
- `supportsMouseDragged` 仍然只是兼容显示，不应表现为已接入真实拖拽功能。
