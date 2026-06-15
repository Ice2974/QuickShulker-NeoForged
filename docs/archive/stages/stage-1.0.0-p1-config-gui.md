# stage-1-config-gui

> 历史更新：阶段 3 已实现潜影盒 mouse dragged 批量交互，并重新开放 `supportsMouseDragged` 配置 GUI。本文中关于 `supportsMouseDragged` 仅为兼容字段的描述只代表阶段 1 当时状态，当前行为以源码和 `docs/stage-3-mouse-dragged.md` 为准。

> 历史说明：本文档记录阶段 1 的配置 GUI 入口与修补内容。当前实现状态以源码、当前发布文档和后续阶段报告为准。

## 阶段 1 范围

阶段 1 只处理配置 GUI 入口和配置编辑能力，不进入 Bundle、mouse dragged、`rightClickClose`、`reopen inventory` 或末影箱 bundling 等后续阶段。

已完成：

- Forge 1.20.1 与 NeoForge 1.21.1 的配置界面入口
- Mods 页面配置按钮入口
- 配置界面快捷键被 quick-open 配置误拦截的问题修复

## 本阶段未实现内容

以下内容在阶段 1 没有实现，也没有在该阶段作为可用功能对玩家承诺：

- Bundle 独立菜单
- Bundle quick-open
- Bundle bundling
- `supportsMouseDragged` 对应的实际 mouse dragged 行为
- 末影箱 bundling
- `rightClickClose`
- `reopen inventory`

## 后续状态说明

后续阶段已经明确：

- 1.0.0 主动跳过 Bundle 相关功能，不会提供 Bundle 独立菜单、Bundle quick-open 或 Bundle bundling
- `rightClickClose` 仍不恢复
- `reopen inventory` 仍不恢复
- `supportsMouseDragged` 如保留配置字段，也只作为兼容字段，不代表当前已支持对应行为

## 本阶段未改动边界

- shulker / ender chest 容器保存逻辑
- HostSlotRef / slot 映射逻辑
- 网络协议和数据包结构
- quick-open 切换与宿主锁定逻辑

## 历史验证记录

阶段 1 记录中曾执行：

```powershell
git diff --check
.\gradlew.bat :forge-1.20.1:build
.\gradlew.bat :neoforge-1.21.1:build
```

该验证结果仅代表阶段 1 当时的补丁提交状态，不代表当前工作区状态。
