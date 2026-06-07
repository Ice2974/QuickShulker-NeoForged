# 初始骨架

> 历史开发记录：本文档记录早期阶段实现过程，不再作为当前实现状态的唯一依据。当前状态请以源码、README 和 `docs/releases/release-0.1.0.md` 为准。

当时仓库的初始骨架如下：

- `common`：共享的纯 Java 常量与启动辅助逻辑
- `versions/forge-1.20.1`：Forge 47.x 入口与 Forge 平台元数据
- `versions/neoforge-1.21.1`：NeoForge 21.x 入口与 NeoForge 平台元数据

本阶段的目标只是建立一个可构建的多模块骨架。

在这个阶段中，QuickShulker 的玩法功能、菜单、网络通信、配置项和快捷键都还没有实现。
