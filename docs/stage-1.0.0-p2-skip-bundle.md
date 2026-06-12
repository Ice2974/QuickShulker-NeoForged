# stage-2-skip-bundle

## 阶段目标

本阶段按维护者决定，明确将 Minecraft Bundle 相关功能从 `1.0.0` 移植范围中主动排除。

原因：

- Forge 1.20.1 与 NeoForge 1.21.1 目标环境中的 Bundle 仍属于实验性 / 非主线稳定玩法
- Bundle 独立菜单、Bundle quick-open、Bundle bundling 会引入额外的菜单、宿主保存、跨版本 `NBT` / `DataComponent` 维护负担
- 当前版本优先保证 shulker / ender chest quick-open、shulker bundling、保存链路和 slot 映射稳定

## 本阶段清理内容

### 配置 GUI

- Forge 1.20.1 配置 GUI 不再显示 `supportsMouseDragged`
- NeoForge 1.21.1 配置 GUI 不再显示 `supportsMouseDragged`
- 双平台配置 GUI 将 `Bundling` 页签改为明确的“潜影盒 Bundling / Shulker Bundling”
- 双平台 bundling 配置文案改为明确指向潜影盒，避免玩家误解为 Bundle 物品支持

### 配置字段与注释

- 保留 `supportsMouseDragged` 配置字段，仅作为兼容字段
- 双平台配置注释明确写明：`QuickShulker 1.0.0` 不支持 mouse dragged shulker 交互，该字段不得重新暴露到 GUI 或接入行为逻辑

### 文档

- 更新 `docs/stage-1-config-gui.md`，明确阶段 1 从未承诺 Bundle 可用，且 1.0.0 主动跳过 Bundle
- 新增本阶段报告，记录 Bundle 主动跳过结论与本阶段清理范围

## 明确不包含的内容

`1.0.0` 不包含：

- Bundle 独立菜单
- Bundle quick-open
- Bundle bundling

本阶段也没有恢复或进入以下高风险 / 后续内容：

- `rightClickClose`
- `reopen inventory`
- mouse dragged
- 末影箱 bundling

## 未改动边界

本阶段没有修改以下稳定逻辑：

- shulker / ender chest quick-open
- 已有 shulker bundling 行为
- 容器保存路径
- `HostSlotRef` / slot 映射
- quick-open session 收尾逻辑

## 验证命令与结果

本阶段实际执行：

```powershell
git diff --check
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

结果以本阶段提交时的实际命令输出为准。

## 待人工确认项

- Minecraft 游戏内是否仍有其他会让玩家误解为 Bundle 已支持的可见入口
- Forge 1.20.1 与 NeoForge 1.21.1 的配置 GUI 实机显示是否符合维护者预期
- 无法在本阶段内完成 Minecraft 游戏内测试
- 无法在本阶段内完成多人服务器测试
