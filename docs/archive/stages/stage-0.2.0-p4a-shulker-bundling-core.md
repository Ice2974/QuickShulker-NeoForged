# 0.2.0 P4A 阶段：Shulker Bundling 服务端核心工具

## 概要

本阶段只实现潜影盒 bundling 的底层服务端工具与规则判断，为后续右键事件接入做准备。

本阶段没有接入玩家输入事件，没有新增网络包，没有修改 quick-open 菜单打开、关闭、保存或 `reopen inventory` 路径，因此当前玩家可见行为不应发生变化。

## 本阶段新增内容

### common 纯规则层

新增 `common/.../bundling/`：

* `ShulkerBundlingFailure`
* `ShulkerBundlingResult`
* `ShulkerBundlingStackAdapter`
* `ShulkerBundlingRules`

这里放的是纯 Java 规则：

* 插入普通物品到潜影盒内容
* 从潜影盒内容提取第一个非空槽位的整组物品
* 将一个潜影盒内容转移到另一个潜影盒内容
* 返回明确结果对象，而不是只返回 `boolean`

算法默认在副本上计算，结果由调用方决定是否真正写回宿主 `ItemStack`。

### Forge 1.20.1

新增：

* `versions/forge-1.20.1/.../ForgeShulkerBundlingHelper.java`

该 helper 复用了现有 `ForgeShulkerContentAccess`：

* 读取路径仍走 `BlockEntityTag` / `ContainerHelper.loadAllItems`
* 写回路径仍走现有 `writeItemStacks`

helper 只负责：

* 校验宿主是否为单个潜影盒 `ItemStack`
* 读取宿主内容副本
* 调用 common bundling 规则
* 将结果写回新的宿主 `ItemStack` 副本并返回

### NeoForge 1.21.1

新增：

* `versions/neoforge-1.21.1/.../NeoForgeShulkerBundlingHelper.java`

该 helper 复用了现有 `NeoForgeShulkerContentAccess`：

* 读取路径仍走 `DataComponents.CONTAINER`
* 写回路径仍走现有 `writeItemStacks`

helper 的职责与 Forge 版本一致，只是底层内容读写复用 NeoForge 的 DataComponent 实现。

## 当前支持的底层操作

### insert

规则摘要：

* 目标宿主必须是单个潜影盒
* 输入物品不能为空
* 输入物品不能是潜影盒，避免 nesting
* 优先合并到已有同类堆
* 再放入空槽
* 不超过每格最大堆叠数
* 返回实际移动数量与更新后的宿主副本、输入副本

### extract

规则摘要：

* 源宿主必须是单个潜影盒
* 从第一个非空槽位提取
* 当前阶段提取的是该槽位的整组物品
* 返回提取出的物品副本与更新后的宿主副本
* 空潜影盒返回 `NO_ITEMS_TO_EXTRACT`

### transfer

规则摘要：

* 源和目标都必须是单个潜影盒
* 逐槽尝试把源内容插入目标
* 遇到潜影盒物品时拒绝嵌套，不移动该槽位
* 只移动目标当前可容纳的数量
* 返回更新后的源宿主副本、目标宿主副本和总移动数量

注意：

* 当前阶段不会判断源宿主和目标宿主是否是同一个真实槽位
* 后续事件接入阶段必须由调用方结合 `HostSlotRef` 保证不会把同一宿主当成两个独立容器处理

## 明确不包含的内容

本阶段不包含：

* 右键事件接入
* mouse dragged 批量行为
* Bundle 独立菜单
* `rightClickClose`
* C2S bundling 包
* quick-open 菜单行为变更
* `reopen inventory` 行为变更
* 末影箱同步变更
* HostSlotRef 映射改动
* 宿主槽位锁定规则改动

## 构建与测试

本阶段补充了 `common` 的 JUnit 测试，覆盖：

* 普通物品插入空潜影盒
* 合并到已有同类堆
* 目标空间不足时的部分插入
* 满目标时不修改
* 潜影盒物品插入失败
* 提取成功 / 提取失败
* 潜影盒到潜影盒转移
* 部分可转移场景

实际构建结果以本阶段完成时的 Gradle 验证命令为准。

## 下一阶段接入事件时的风险点

后续如果把这些 helper 接入右键事件，需要重点注意：

* 客户端只能表达意图，不能把客户端看到的宿主内容当作可信真值
* 服务端必须重新解析 `HostSlotRef` 并重新校验宿主仍然有效
* 同宿主重复触发必须拒绝，不能生成两个互不相通的容器副本
* 切换宿主前必须先安全收尾已有 quick-open session
* bundling 结果真正写回玩家背包前，必须确认宿主没有被外部移动、替换或堆叠数量变化
* 不能让 bundling 绕过现有潜影盒保存链路
* 不能破坏末影箱同步、`reopen inventory` 和宿主槽位锁定逻辑
