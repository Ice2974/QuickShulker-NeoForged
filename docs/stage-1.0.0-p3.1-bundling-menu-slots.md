# stage-1.0.0-p3.1-bundling-menu-slots

> 阶段 3.1 将单次 bundling 与 mouse dragged 收纳目标扩展到当前菜单槽位。本次文档同步补上安全补丁，并在保守前提下重新放开一部分已知安全的输入槽位。

## 阶段目标

- 保留阶段 3 已完成的“鼠标携带单个潜影盒，右键拖过多个普通物品槽位，将物品收纳进鼠标潜影盒”的 `mouse dragged` 收纳路径。
- 保留玩家背包、hotbar、副手和安全菜单槽位中的单次 bundling / 拖拽收纳。
- 收紧 `PLAYER_CONTAINER_MENU` 槽位判定，避免把特殊输出槽、虚拟槽和无法证明安全的菜单槽位纳入 bundling 目标。
- 在可证明不会绕过原版结果结算的前提下，允许一部分原版输入槽位继续使用 QuickShulker bundling。
- 不进入 mouse dragged 批量放出物品阶段。

## 实现内容

### Forge 1.20.1

- `ForgeHostSlotResolver`
  - bundling 槽位判定从“纯存储菜单 allowlist”扩展为“按菜单类型 + 槽位索引”的安全判定。
  - 继续放行原版常规存储菜单中的普通存储槽。
  - 额外放行玩家背包合成输入槽、工作台输入槽、铁砧输入槽、切石机输入槽。
  - 结果槽和其他无法证明安全的菜单槽位继续拒绝。
- `ForgeShulkerBundlingHandler`
  - `INSERT`、`PICKUP_INSERT`、`MOUSE_DRAG_PICKUP_INSERT`、`TRANSFER` 会先校验目标槽是否允许安全读取 / 缩减，再校验写回是否安全。
  - `EXTRACT` 在向空槽写入前会校验该槽是否允许安全替换。
  - 不安全槽位会被服务端直接拒绝，避免直接 `set` 特殊输出槽。

### NeoForge 1.21.1

- `NeoForgeHostSlotResolver`
  - 与 Forge 侧同步增加“按菜单类型 + 槽位索引”的 bundling 安全槽位判定。
  - 继续放行原版常规存储菜单中的普通存储槽。
  - 额外放行玩家背包合成输入槽、工作台输入槽、铁砧输入槽、切石机输入槽。
  - 结果槽和其他无法证明安全的菜单槽位继续拒绝。
- `NeoForgeShulkerBundlingHandler`
  - 与 Forge 侧同步收紧 `INSERT`、`PICKUP_INSERT`、`MOUSE_DRAG_PICKUP_INSERT`、`TRANSFER`、`EXTRACT` 的槽位安全校验。
  - 不安全槽位会被服务端直接拒绝。

## 当前继续支持的槽位

- 玩家背包主背包槽位。
- 玩家 hotbar。
- 玩家副手槽位。
- 玩家背包 2x2 合成输入槽位。
- 原版常规存储容器的普通存储槽：
  - 箱子菜单
  - 潜影盒菜单
  - 漏斗菜单
  - 发射器 / 投掷器菜单
- 原版工作台输入槽位。
- QuickShulker 工作台页面输入槽位。
- 原版铁砧输入槽位。
- QuickShulker 铁砧页面输入槽位。
- 原版切石机输入槽位。
- QuickShulker 切石机页面输入槽位。

上述范围内继续支持：

- `INSERT`
- `PICKUP_INSERT`
- `MOUSE_DRAG_PICKUP_INSERT`
- `TRANSFER`
- `EXTRACT`

## 当前明确拒绝的槽位

- 玩家背包 2x2 合成结果槽。
- 工作台结果槽。
- 熔炉结果槽。
- 村民交易结果槽。
- 铁砧结果槽。
- 切石机结果槽。
- 其他配方 / 结算结果槽。
- 其他特殊输出槽。
- 虚拟槽。
- 不允许安全直接 `set` 的槽位。
- 无法确认安全的第三方菜单槽位。

## 安全补丁

- `PICKUP_INSERT` / `MOUSE_DRAG_PICKUP_INSERT` 现在要求目标槽是可普通取出、可安全缩减并可安全写回的真实槽位。
- `TRANSFER` / `INSERT` 现在要求目标 shulker 所在槽本身就是可安全替换的真实槽位。
- `EXTRACT` 现在要求目标空槽是可安全写入的真实槽位。
- 合成 / 铁砧 / 切石机目前只放行输入槽，不放行结果槽。
- `mouse dragged` 批量收纳功能仍保留，但现在只处理上述安全槽位，不处理特殊输出槽。
- 这样做是为了避免绕过原版 `onTake`、输入消耗、经验结算或其他菜单副作用，降低复制、幽灵物品和错误写回风险。

## 保留与未实现项

- 已保留阶段 3 的 mouse dragged 批量收纳。
- 未实现 mouse dragged 批量放出物品。
- 未恢复 `rightClickClose`。
- 未恢复 `reopen inventory`。
- 未实现 Bundle 相关功能。
- 未改 QuickShulker quick-open session 保存主链路。

## 修改文件

- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeHostSlotResolver.java`
- `versions/forge-1.20.1/src/main/java/com/ice2974/quickshulkerneoforged/forge/ForgeShulkerBundlingHandler.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeHostSlotResolver.java`
- `versions/neoforge-1.21.1/src/main/java/com/ice2974/quickshulkerneoforged/neoforge/NeoForgeShulkerBundlingHandler.java`
- `docs/stage-1.0.0-p3.1-bundling-menu-slots.md`

## 建议人工测试

### Forge 1.20.1

- 箱子 / 潜影盒 / 漏斗 / 发射器菜单中的普通存储槽仍可执行单次 bundling 与 mouse dragged 收纳。
- 玩家背包合成输入槽、工作台输入槽、铁砧输入槽、切石机输入槽可执行 bundling。
- 合成结果槽、熔炉结果槽、铁砧结果槽、交易结果槽、切石机结果槽不会触发 bundling。
- 创造模式与生存模式下，对不安全菜单槽位重复右键 / 拖拽不会出现复制或幽灵物品。

### NeoForge 1.21.1

- 箱子 / 潜影盒 / 漏斗 / 发射器菜单中的普通存储槽仍可执行单次 bundling 与 mouse dragged 收纳。
- 玩家背包合成输入槽、工作台输入槽、铁砧输入槽、切石机输入槽可执行 bundling。
- 合成结果槽、熔炉结果槽、铁砧结果槽、交易结果槽、切石机结果槽不会触发 bundling。
- 创造模式与生存模式下，对不安全菜单槽位重复右键 / 拖拽不会出现复制或幽灵物品。

## 待人工确认项

- 尚未进行 Minecraft 实机测试，未确认 Forge / NeoForge 两侧在所有原版菜单中的最终手感与事件取消表现。
- 尚未进行多人服务器测试，未确认双平台在高频拖拽和菜单快速切换下的同步表现。
- 当前对第三方容器槽位仍采用保守拒绝策略；是否需要后续为特定模组菜单逐个做安全兼容，待维护者决定。
