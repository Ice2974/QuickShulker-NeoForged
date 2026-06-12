# stage-0.2.0-p5-config-gui-entry

> 历史说明：本文档记录早期配置 GUI 入口补齐与分页整理结果。当前实现状态以源码、当前发布文档和后续阶段报告为准。

## 历史阶段已完成内容

该阶段当时完成了以下基础配置 GUI 能力：

- Forge 1.20.1 与 NeoForge 1.21.1 的 Mods 页面配置入口
- `openSettingsKey` 打开配置界面
- 配置界面分页整理
- 中英文配置界面文案补齐

## 当前仍有效的结论

- 配置 GUI 只负责配置读写与入口展示，不参与 quick-open 保存链路
- `rightClickClose` 未恢复
- `reopen inventory` 未恢复
- 不会因为配置 GUI 改动而改变 HostSlotRef、slot 映射或容器保存逻辑

## 已过期的旧表述说明

以下旧阶段表述不再代表当前状态：

- `supportsMouseDragged` 不再继续显示在配置 GUI 中
- bundling 分页当前应明确理解为“潜影盒 bundling / shulker bundling”，不是 Bundle 物品支持入口
- 1.0.0 已明确主动跳过 Bundle 独立菜单、Bundle quick-open 和 Bundle bundling

## 当前配置 GUI 边界

当前配置 GUI 可见范围应限定为：

- 基础输入 / 快捷键配置
- 已接入的 quick-open 目标开关
- 已接入的潜影盒 bundling 开关

当前配置 GUI 不应向玩家暴露：

- Bundle 菜单入口
- Bundle quick-open 开关
- Bundle bundling 开关
- `supportsMouseDragged` 可编辑项

## 历史验证记录

该阶段文档原记录过以下构建验证：

```powershell
.\gradlew.bat :forge-1.20.1:build :neoforge-1.21.1:build
```

该记录只代表当时补丁状态，不代表当前工作区状态。
