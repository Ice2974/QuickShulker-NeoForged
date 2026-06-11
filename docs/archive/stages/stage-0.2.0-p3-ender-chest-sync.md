# 0.2.0 P3 阶段：末影箱同步

## 概要

本阶段为 Forge 1.20.1 和 NeoForge 1.21.1 的 quick-open 末影箱会话增加了额外的服务端到客户端同步。

末影箱仍然以服务端玩家自己的 `EnderChestInventory` 作为真相来源：

* Forge：`serverPlayer.getEnderChestInventory()`
* NeoForge：`serverPlayer.getEnderChestInventory()`

本阶段不实现 bundling、mouse dragged 行为、Bundle 菜单或 `rightClickClose`。

## 已实现

### 全量同步

当末影箱 quick-open 会话成功打开后，服务端会为当前会话发送一次完整的 S2C 快照：

* `sessionId`
* 27 个末影箱槽位的完整槽位数量数据
* 每个槽位完整的 `ItemStack` 数据

Forge 使用平台侧的 `FriendlyByteBuf` 物品序列化。

NeoForge 使用平台侧的 `RegistryFriendlyByteBuf` 加 `ItemStack.OPTIONAL_STREAM_CODEC`，因为空末影箱槽位也必须能够被编码，不能因此把客户端断开。

### 增量槽位同步

在 quick-open 末影箱菜单保持打开期间：

* 菜单会追踪 27 个末影箱槽位的最近一次同步副本
* `broadcastChanges()` 会将当前 `EnderChestInventory` 内容与最近一次同步状态进行对比
* 发生变化的槽位会以 S2C 槽位同步 payload 的形式发送给当前玩家

只有末影箱槽位 `0-26` 会被同步。宿主 `ItemStack` 不参与同步，也不会被当作末影箱回写目标。

### 客户端处理

收到 full sync 或 slot sync 后：

* 客户端会先确认 `Minecraft.getInstance().player` 可用
* 只更新本地客户端自己的 `player.getEnderChestInventory()` 缓存 / 显示状态
* 校验槽位范围是否合法
* 在写入前先对同步到的堆栈执行复制
* 标记本地末影箱容器已变更

这个处理路径不会额外发送 C2S 包。

## 专用服务器安全

本阶段还对现有 S2C 处理补了一个最小化的专用服务器类加载安全修正：

* Forge 网络侧的 S2C handler 不再直接导入 client handler 类
* NeoForge 网络侧的 S2C handler 不再直接导入 client handler 类
* NeoForge 模组启动入口不再直接导入 client bootstrap 类

这些路径现在只会在真正调用客户端处理逻辑时，才通过反射解析 client-only 入口。

## 不在本阶段范围内

本阶段不包含以下内容：

* `rightClickClose`
* shulker bundling 的 insert / pickup / transfer / extract
* mouse dragged 批量行为
* Bundle 独立菜单
* 宿主槽位映射改动
* 宿主槽位锁定规则改动
* quick-open 切换规则改动
* reopen inventory 触发条件改动
* 将末影箱内容写回宿主物品

## 验证

已执行构建验证：

* `.\gradlew.bat build`
* `.\gradlew.bat :forge-1.20.1:build`
* `.\gradlew.bat :neoforge-1.21.1:build`

本阶段没有完成游戏内人工验证，仍然需要维护者实际测试。

## 待人工测试

* Forge 1.20.1 单人模式下 quick-open 末影箱的打开 / 修改 / 关闭 / 再打开
* NeoForge 1.21.1 单人模式下 quick-open 末影箱的打开 / 修改 / 关闭 / 再打开
* 两个平台的专用服务器加入和 packet 流验证
* 双人隔离验证，确认末影箱数据不会跨玩家串用
* 潜影盒保存、宿主槽位锁定、switch-open、reopen inventory、生存 / 创造模式边界回归验证
