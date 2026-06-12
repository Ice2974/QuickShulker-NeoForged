# stage-1.0.0-p4-bundling-menu-slots

## 阶段目标

本阶段按维护者反馈调整 shulker bundling 的右键与拖拽行为：

- 移除“鼠标携带普通物品，右键拖过多个潜影盒槽位，逐个插入”的批量拖拽路径。
- 将单次右键 bundling 目标槽从玩家背包扩展到当前菜单槽位，包括 QuickShulker 菜单、原版容器菜单和物品栏合成格。
- 将“拿盒子右键物品将其收纳”“拿物品右键盒子将其放入”“拿盒子右键空格子放出物品”同步扩展到上述菜单槽位。
- 允许创造模式使用保留的拖拽收纳路径，即“拿单个潜影盒右键拖过多个普通物品槽位，将物品收纳进鼠标潜影盒”。

## 实现内容

### Forge 1.20.1

- `ForgeHostSlotResolver` 新增 `PLAYER_CONTAINER_MENU` 槽位解析：
  - 玩家背包、hotbar、副手仍沿用原有稳定映射。
  - 非玩家背包的当前菜单槽位使用 `menuSlotIndex` 在服务端当前 `player.containerMenu` 中重新定位。
  - 写入当前菜单槽位前会重新解析服务端真实槽位，避免信任客户端上传的 `ItemStack`。
- `ForgeQuickShulkerClient` 的 bundling 入口改用通用 bundling 槽位解析：
  - QuickShulker 菜单槽、原版容器槽、物品栏合成格可发起 bundling 请求。
  - 不再为普通物品拖拽插入潜影盒创建拖拽状态。
  - 保留“拿潜影盒拖过普通物品槽位收纳”的拖拽状态，并移除创造模式禁用条件。
- `ForgeShulkerBundlingHandler`：
  - `INSERT`、`PICKUP_INSERT`、`EXTRACT`、`TRANSFER` 使用扩展后的槽位解析读写目标。
  - `EXTRACT` 写入空槽前校验目标槽 `mayPlace`，避免写入不接受该物品的槽位。
  - `MOUSE_DRAG_INSERT` 服务端显式拒绝，防旧客户端或异常包恢复已移除行为。
  - `MOUSE_DRAG_PICKUP_INSERT` 在创造模式下使用客户端随包携带的 cursor 副本继续处理。

### NeoForge 1.21.1

- 与 Forge 同步扩展 `NeoForgeHostSlotResolver`、`NeoForgeQuickShulkerClient` 和 `NeoForgeShulkerBundlingHandler`。
- NeoForge 侧仍使用 1.21.1 平台的数据组件读写逻辑，未把 Forge NBT-only 逻辑引入 NeoForge 模块。

## 已移除行为

当前不再支持：

- 鼠标携带普通物品，按住右键拖过多个潜影盒槽位，逐个插入。

单次右键“拿物品右键盒子将其放入”仍保留；只是不会在拖过多个盒子时继续批量插入。

## 保留并扩展的行为

以下行为现在可在玩家背包、QuickShulker 菜单、原版容器菜单和物品栏合成格中触发：

- 鼠标拿普通物品，右键单个潜影盒，将鼠标物品放入潜影盒。
- 鼠标拿单个潜影盒，右键普通物品槽，将该槽物品收纳进鼠标潜影盒。
- 鼠标拿单个潜影盒，右键空槽，从潜影盒放出第一组非空物品。
- 鼠标拿单个潜影盒，右键另一个单个潜影盒，将来源潜影盒内容尽量转移到目标潜影盒。
- 鼠标拿单个潜影盒，按住右键拖过多个普通物品槽位，逐个收纳物品；创造模式也允许发起。

## 安全边界

- 客户端仍只发送 action、`HostSlotRef` 和 cursor 副本，不把目标槽物品作为可信真值。
- 服务端按当前真实 `player.containerMenu` 和 `HostSlotRef` 重新解析目标槽。
- 当前 quick-open 宿主槽仍通过 `HostIdentity.sameSlot` 拒绝 bundling，避免写回当前宿主自身。
- 从潜影盒放出物品到空槽时，服务端会校验目标槽可接受该物品。
- 普通物品拖拽插入潜影盒即使被旧客户端发包，服务端也会拒绝。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeHostSlotResolver.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/client/ForgeQuickShulkerClient.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeHostSlotResolver.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/client/NeoForgeQuickShulkerClient.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `docs/stage-3-mouse-dragged.md`
- `docs/stage-1.0.0-p4-bundling-menu-slots.md`

## 验证命令与结果

已执行：

```powershell
.\gradlew.bat compileJava
```

结果：

- 通过。
- `:common:compileJava` 为 up-to-date。
- `:forge-1.20.1:compileJava` 通过。
- `:neoforge-1.21.1:compileJava` 通过。
- 编译输出仍有既有 deprecated API 提示，未发现本阶段新增编译错误。

## 未验证内容

- 未进行 Minecraft 客户端实机测试。
- 未进行 Forge 1.20.1 / NeoForge 1.21.1 专用服务器多人测试。
- 未验证所有原版容器和第三方容器的 Slot `mayPlace` 行为差异。
- 未确认创造模式拖拽收纳在所有创造物品栏分页中的最终手感和同步表现。

## 待人工确认项

- Forge 1.20.1 实机确认：QuickShulker 菜单、箱子/熔炉等原版容器、物品栏合成格均可触发单次右键 bundling。
- NeoForge 1.21.1 实机确认：QuickShulker 菜单、箱子/熔炉等原版容器、物品栏合成格均可触发单次右键 bundling。
- 双平台确认：普通物品右键拖过多个潜影盒不再逐个插入。
- 双平台确认：拿单个潜影盒右键拖过多个普通物品槽位仍可收纳，且创造模式也可用。
- 双平台多人服务器确认：目标槽被其他操作改变时不会写入错误槽位或产生幽灵物品。
- 是否需要更新许可证 / NOTICE：本阶段未复制新的第三方大段代码，但仍建议维护者按发布流程确认。
