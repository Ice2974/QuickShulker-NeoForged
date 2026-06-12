# 阶段 1 补充：移除 `rightClickClose`

本次调整只移除 `rightClickClose` 相关配置语义与文档说明，不引入新的玩家交互行为，也不改变现有 quick-open 行为。

## 调整背景

`rightClickClose` 原本只作为预留配置存在，仓库内并未实现“再次右键关闭当前 quick-open 菜单”的完整行为。

结合当前项目状态，本仓库决定暂不移植该功能，原因包括：

- 上游 `1.21+` 相关实现存在额外风险，尤其是菜单生命周期、关闭后回到玩家背包以及后续 reopen 流程之间的耦合较高
- 当前仓库已经对“同一宿主重复触发打开请求”做了明确防护，核心目标是避免重复 open、错误 reopen 和错误写回
- 在现阶段直接保留 `rightClickClose` 配置入口，容易让配置语义与实际行为产生误解

## 本次代码调整

本次已从以下层面移除 `rightClickClose`：

- common 配置模型 `QuickShulkerConfig`
- common 配置视图 `QuickShulkerConfigView`
- Forge 平台配置 `ForgeQuickShulkerConfig`
- NeoForge 平台配置 `NeoForgeQuickShulkerConfig`

这意味着：

- 双平台配置文件不再暴露 `rightClickClose`
- common 侧不再保留 `rightClickClose()` accessor
- 现有输入、网络、菜单、保存和宿主锁定逻辑保持不变

## 明确未实现内容

本次没有实现以下行为：

- 再次右键关闭当前 quick-open 菜单
- 关闭后自动回到玩家背包的 `rightClickClose` 专属流程
- 基于该功能的额外客户端 / 服务端 reopen 协调

## 当前结论

`rightClickClose` 当前不属于本仓库的移植范围。

后续如果要重新评估该功能，应先确认：

- 上游 `1.21+` 行为是否足够稳定
- 与当前宿主重复触发防护策略是否兼容
- 是否不会影响已有的 quick-open 保存、切换打开和宿主锁定链路
